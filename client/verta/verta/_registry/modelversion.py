# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import time

import requests

from .._protos.public.registry import RegistryService_pb2 as _ModelVersionService
from .._protos.public.common import CommonService_pb2 as _CommonCommonService

import requests
import time
import os
import pickle
from ..external import six

from .._internal_utils import (
    _utils,
    _artifact_utils,
    importer, _request_utils
)
from .._internal_utils._utils import NoneProtoResponse
from .. import utils

from .._tracking.entity import _ModelDBEntity, _OSS_DEFAULT_WORKSPACE
from ..environment import _Environment, Python


class RegisteredModelVersion(_ModelDBEntity):
    """
    Object representing a version of a Registered Model.

    There should not be a need to instantiate this class directly; please use
    :meth:`RegisteredModel.get_or_create_version`.

    Attributes
    ----------
    id : int
        ID of this Model Version.
    name : str
        Name of this Model Version.
    has_environment : bool
        Whether there is an environment associated with this Model Version.
    has_model : bool
        Whether there is a model associated with this Model Version.
    registered_model_id : int
        ID of this version's Registered Model.
    is_archived : bool
        Whether this Model Version is archived.

    """
    def __init__(self, conn, conf, msg):
        super(RegisteredModelVersion, self).__init__(conn, conf, _ModelVersionService, "registered_model_version", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg
        artifact_keys = self.get_artifact_keys()
        if self.has_model:
            artifact_keys.append("model")

        return '\n'.join((
            "version: {}".format(msg.version),
            "url: {}://{}/{}/registry/{}/versions/{}".format(self._conn.scheme, self._conn.socket, self.workspace, self.registered_model_id, self.id),
            "time created: {}".format(_utils.timestamp_to_str(int(msg.time_created))),
            "time updated: {}".format(_utils.timestamp_to_str(int(msg.time_updated))),
            "description: {}".format(msg.description),
            "labels: {}".format(msg.labels),
            "id: {}".format(msg.id),
            "registered model id: {}".format(msg.registered_model_id),
            "experiment run id: {}".format(msg.experiment_run_id),
            "archived status: {}".format(msg.archived),
            "artifact keys: {}".format(artifact_keys),
        ))

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.version

    @property
    def has_environment(self):
        self._refresh_cache()
        return self._msg.environment.HasField("python") or self._msg.environment.HasField("docker")

    @property
    def has_model(self):
        self._refresh_cache()
        return bool(self._msg.model) and bool(self._msg.model.key)

    @property
    def registered_model_id(self):
        self._refresh_cache()
        return self._msg.registered_model_id

    @property
    def is_archived(self):
        self._refresh_cache()
        return self._msg.archived == _CommonCommonService.TernaryEnum.TRUE

    @property
    def workspace(self):
        self._refresh_cache()
        Message = _ModelVersionService.GetRegisteredModelRequest
        response = self._conn.make_proto_request(
            "GET", "/api/v1/registry/registered_models/{}".format(self.registered_model_id)
        )

        registered_model_msg = self._conn.maybe_proto_response(response, Message.Response).registered_model

        if registered_model_msg.workspace_id:
            return self._get_workspace_name_by_id(registered_model_msg.workspace_id)
        else:
            return _OSS_DEFAULT_WORKSPACE

    def get_artifact_keys(self):
        """
        Gets the artifact keys of this Model Version.

        Returns
        -------
        list of str
            List of artifact keys of this Model Version.

        """
        self._refresh_cache()
        return list(map(lambda artifact: artifact.key, self._msg.artifacts))

    @classmethod
    def _generate_default_name(cls):
        return "ModelVersion {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ModelVersionService.GetModelVersionRequest
        endpoint = "/api/v1/registry/model_versions/{}".format(id)
        response = conn.make_proto_request("GET", endpoint)

        return conn.maybe_proto_response(response, Message.Response).model_version

    @classmethod
    def _get_proto_by_name(cls, conn, name, registered_model_id):
        Message = _ModelVersionService.FindModelVersionRequest
        predicates = [
            _CommonCommonService.KeyValueQuery(key="version",
                                               value=_utils.python_to_val_proto(name),
                                               operator=_CommonCommonService.OperatorEnum.EQ)
        ]
        endpoint = "/api/v1/registry/registered_models/{}/model_versions/find".format(registered_model_id)
        msg = Message(predicates=predicates)

        proto_response = conn.make_proto_request("POST", endpoint, body=msg)
        response = conn.maybe_proto_response(proto_response, Message.Response)

        if not response.model_versions:
            return None

        # should only have 1 entry here, as name/version is unique
        return response.model_versions[0]

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, experiment_run_id=None):
        ModelVersionMessage = _ModelVersionService.ModelVersion
        SetModelVersionMessage = _ModelVersionService.SetModelVersion
        registered_model_id = ctx.registered_model.id

        model_version_msg = ModelVersionMessage(registered_model_id=registered_model_id, version=name,
                                                description=desc, labels=tags,
                                                time_created=date_created, time_updated=date_created,
                                                experiment_run_id=experiment_run_id)
        endpoint = "/api/v1/registry/registered_models/{}/model_versions".format(registered_model_id)
        response = conn.make_proto_request("POST", endpoint, body=model_version_msg)
        model_version = conn.must_proto_response(response, SetModelVersionMessage.Response).model_version

        print("Created new ModelVersion: {}".format(model_version.version))
        return model_version

    def log_model(self, model, model_api=None, overwrite=False):
        """
        Logs a model to this Model Version.

        Parameters
        ----------
        model : str or file-like or object
            Model or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact. If it is a directory path, its contents will be zipped.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        model_api : :class:`~utils.ModelAPI`, optional
            Model API specifying details about the model and its deployment.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        self._fetch_with_no_cache()
        if self.has_model and not overwrite:
            raise ValueError("model already exists; consider setting overwrite=True")

        if isinstance(model, six.string_types):  # filepath
            model = open(model, 'rb')
        serialized_model, method, model_type = _artifact_utils.serialize_model(model)

        try:
            extension = _artifact_utils.get_file_ext(serialized_model)
        except (TypeError, ValueError):
            extension = _artifact_utils.ext_from_method(method)

        # Create artifact message and update ModelVersion's message:
        self._msg.model.CopyFrom(self._create_artifact_msg("model", serialized_model, artifact_type=_CommonCommonService.ArtifactTypeEnum.MODEL, extension=extension))
        self._update()

        # Upload the artifact to ModelDB:
        self._upload_artifact(
            "model", serialized_model,
            _CommonCommonService.ArtifactTypeEnum.MODEL,
        )

        # build model API
        if model_api is None:
            model_api = utils.ModelAPI()
        elif not isinstance(model_api, utils.ModelAPI):
            raise ValueError("`model_api` must be `verta.utils.ModelAPI`, not {}".format(type(model_api)))
        if 'model_packaging' not in model_api:
            # add model serialization info to model_api
            model_api['model_packaging'] = {
                'python_version': _utils.get_python_version(),
                'type': model_type,
                'deserialization': method,
            }
        self.log_artifact("model_api.json", model_api, overwrite)

    def get_model(self):
        """
        Gets the model of this Model Version.

        If the model was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the model object will be returned.

        Returns
        -------
        str or object or bytes
            Path of the model, the model object, or a bytestream representing the
            model.

        """
        model_artifact = self._get_artifact("model", _CommonCommonService.ArtifactTypeEnum.MODEL)
        return _artifact_utils.deserialize_model(model_artifact)

    def del_model(self):
        """
        Deletes model of this Model Version.

        """
        self._fetch_with_no_cache()
        self._msg.ClearField("model")
        self._update()

    def log_artifact(self, key, artifact, overwrite=False):
        """
        Logs an artifact to this Model Version.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact : str or file-like or object
            Artifact or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact. If it is a directory path, its contents will be zipped.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        if key == "model":
            raise ValueError("the key \"model\" is reserved for model; consider using log_model() instead")

        self._fetch_with_no_cache()
        same_key_ind = -1

        for i in range(len(self._msg.artifacts)):
            if self._msg.artifacts[i].key == key:
                if not overwrite:
                    raise ValueError("The key has been set; consider setting overwrite=True")
                else:
                    same_key_ind = i
                break

        artifact_type = _CommonCommonService.ArtifactTypeEnum.BLOB

        if isinstance(artifact, six.string_types):  # filepath
            artifact = open(artifact, 'rb')
        artifact_stream, method = _artifact_utils.ensure_bytestream(artifact)

        try:
            extension = _artifact_utils.get_file_ext(artifact_stream)
        except (TypeError, ValueError):
            extension = _artifact_utils.ext_from_method(method)

        artifact_msg = self._create_artifact_msg(key, artifact_stream, artifact_type=artifact_type, extension=extension)
        if same_key_ind == -1:
            self._msg.artifacts.append(artifact_msg)
        else:
            self._msg.artifacts[same_key_ind].CopyFrom(artifact_msg)

        self._update()
        self._upload_artifact(key, artifact_stream, artifact_type=artifact_type)

    def get_artifact(self, key):
        """
        Gets the artifact with name `key` from this Model Version.

        If the artifact was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the artifact object will be returned.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or object or bytes
            Path of the artifact, the artifact object, or a bytestream representing the
            artifact.

        """
        artifact = self._get_artifact(key, _CommonCommonService.ArtifactTypeEnum.BLOB)
        artifact_stream = six.BytesIO(artifact)

        torch = importer.maybe_dependency("torch")
        if torch is not None:
            try:
                obj = torch.load(artifact_stream)
            except:  # not something torch can deserialize
                artifact_stream.seek(0)
            else:
                artifact_stream.close()
                return obj

        try:
            obj = pickle.load(artifact_stream)
        except:  # not something pickle can deserialize
            artifact_stream.seek(0)
        else:
            artifact_stream.close()
            return obj

        return artifact_stream

    def del_artifact(self, key):
        """
        Deletes the artifact with name `key` from this Model Version.

        Parameters
        ----------
        key : str
            Name of the artifact.

        """
        if key == "model":
            raise ValueError("model can't be deleted through del_artifact(); consider using del_model() instead")

        self._fetch_with_no_cache()

        ind = -1
        for i in range(len(self._msg.artifacts)):
            artifact = self._msg.artifacts[i]
            if artifact.key == key:
                ind = i
                break

        if ind == -1:
            raise KeyError("no artifact found with key {}".format(key))

        del self._msg.artifacts[ind]
        self._update()

    def log_environment(self, env):
        """
        Logs an environment to this Model Version.

        Parameters
        ----------
        environment : `verta.environment._Environment.`
            Environment to log.

        """
        if not isinstance(env, _Environment):
            raise TypeError("`env` must be of type Environment, not {}".format(type(env)))

        self._fetch_with_no_cache()
        self._msg.environment.CopyFrom(env._msg)
        self._update()

    def del_environment(self):
        """
        Deletes the environment of this Model Version.

        """
        self._fetch_with_no_cache()
        self._msg.ClearField("environment")
        self._update()

    def get_environment(self):
        """
        Gets the environment of this Model Version.

        Returns
        -------
        :class:`verta.environment._Environment`
            Environment of this ModelVersion.

        """
        self._refresh_cache()
        if not self.has_environment:
            raise RuntimeError("environment was not previously set.")

        return Python._from_proto(self._msg)

    def _get_url_for_artifact(self, key, method, artifact_type, part_num=0):
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _ModelVersionService.GetUrlForArtifact
        msg = Message(
            model_version_id=self.id,
            key=key,
            method=method,
            artifact_type=artifact_type,
            part_number=part_num
        )
        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/registry/model_versions/{}/getUrlForArtifact".format(
            self._conn.scheme,
            self._conn.socket,
            self.id
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)
        return _utils.json_to_proto(response.json(), Message.Response)

    def _upload_artifact(self, key, file_handle, artifact_type, part_size=64*(10**6)):
        file_handle.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(key, "PUT", artifact_type, part_num=1)

        print("uploading {} to ModelDB".format(key))
        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: file_handle.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._get_url_for_artifact(key, "PUT", artifact_type, part_num=part_num).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                #     Retry connection errors, to make large multipart uploads more robust.
                for _ in range(3):
                    try:
                        response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                    except requests.ConnectionError:  # e.g. broken pipe
                        time.sleep(1)
                        continue  # try again
                    else:
                        break
                _utils.raise_for_http_error(response)

                # commit part
                url = "{}://{}/api/v1/registry/model_versions/{}/commitArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                    self.id
                )
                msg = _ModelVersionService.CommitArtifactPart(
                    model_version_id=self.id,
                    key=key
                )
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers['ETag']
                data = _utils.proto_to_json(msg)
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/registry/model_versions/{}/commitMultipartArtifact".format(
                self._conn.scheme,
                self._conn.socket,
                self.id
            )
            msg = _ModelVersionService.CommitMultipartArtifact(
                model_version_id=self.id,
                key=key
            )
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=file_handle)
            _utils.raise_for_http_error(response)

        print("upload complete")

    def _create_artifact_msg(self, key, artifact_stream, artifact_type, extension=None):
        # calculate checksum
        artifact_hash = _artifact_utils.calc_sha256(artifact_stream)
        artifact_stream.seek(0)

        # determine basename
        #     The key might already contain the file extension, thanks to our hard-coded deployment
        #     keys e.g. "model.pkl" and "model_api.json".
        if extension is None:
            basename = key
        elif key.endswith(os.extsep + extension):
            basename = key
        else:
            basename = key + os.extsep + extension

        # build upload path from checksum and basename
        artifact_path = os.path.join(artifact_hash, basename)

        # TODO: support VERTA_ARTIFACT_DIR

        # log key to ModelDB
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=artifact_path,
                                               path_only=False,
                                               artifact_type=artifact_type,
                                               filename_extension=extension)

        return artifact_msg

    def _get_artifact(self, key, artifact_type):
        # check to see if key exists
        self._refresh_cache()
        if key == "model":
            # get model artifact
            if not self.has_model:
                raise KeyError("no model associated with this version")
        elif len(list(filter(lambda artifact: artifact.key == key, self._msg.artifacts))) == 0:
            raise KeyError("no artifact found with key {}".format(key))

        # download artifact from artifact store
        url = self._get_url_for_artifact(key, "GET", artifact_type).url

        response = _utils.make_request("GET", url, self._conn)
        _utils.raise_for_http_error(response)

        return response.content

    def set_description(self, desc):
        if not desc:
            raise ValueError("desc is not specified")
        self._fetch_with_no_cache()
        self._msg.description = desc
        self._update()

    def get_description(self):
        self._refresh_cache()
        return self._msg.description

    def add_labels(self, labels):
        """
        Adds multiple labels to this Model Version.

        Parameters
        ----------
        labels : list of str
            Labels to add.

        """
        if not labels:
            raise ValueError("label is not specified")

        self._fetch_with_no_cache()
        for label in labels:
            if label not in self._msg.labels:
                self._msg.labels.append(label)
        self._update()

    def add_label(self, label):
        """
        Adds a label to this Model Version.

        Parameters
        ----------
        str
            Label to add.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._fetch_with_no_cache()
        if label not in self._msg.labels:
            self._msg.labels.append(label)
            self._update()

    def del_label(self, label):
        """
        Deletes a label from this Model Version.

        Parameters
        ----------
        str
            Label to delete.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._fetch_with_no_cache()
        if label in self._msg.labels:
            self._msg.labels.remove(label)
            self._update()

    def get_labels(self):
        """
        Gets all labels of this Model Version.

        Returns
        -------
        list of str
            List of all labels of this Model Version.

        """
        self._refresh_cache()
        return self._msg.labels

    def download_docker_context(self, download_to_path):
        """
        Downloads this Model Version's Docker context ``tgz``.

        Parameters
        ----------
        download_to_path : str
            Path to download Docker context to.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where Docker context was downloaded to. Matches `download_to_path`.

        """
        self._refresh_cache()
        endpoint = "{}://{}/api/v1/registry/registered_models/{}/model_versions/{}/dockercontext".format(
            self._conn.scheme,
            self._conn.socket,
            self._msg.registered_model_id,
            self.id
        )
        with _utils.make_request("GET", endpoint, self._conn, stream=True) as response:
            try:
                _utils.raise_for_http_error(response)
            except requests.HTTPError as e:
                # propagate error caused by missing artifact
                error_text = e.response.text.strip()
                if error_text.startswith("missing artifact"):
                    new_e = RuntimeError("unable to obtain Docker context due to " + error_text)
                    six.raise_from(new_e, None)
                else:
                    raise e

            downloaded_to_path = _request_utils.download(response, download_to_path, overwrite_ok=True)
            return os.path.abspath(downloaded_to_path)

    def archive(self):
        """
        Archive this Model Version.

        """
        if self.is_archived:
            raise RuntimeError("the version has already been archived")

        self._fetch_with_no_cache()
        self._msg.archived = _CommonCommonService.TernaryEnum.TRUE
        self._update()

    def _update(self):
        response = self._conn.make_proto_request("PUT", "/api/v1/registry/registered_models/{}/model_versions/{}".format(self._msg.registered_model_id, self.id),
                                                 body=self._msg)
        Message = _ModelVersionService.SetModelVersion
        if isinstance(self._conn.maybe_proto_response(response, Message.Response), NoneProtoResponse):
            raise ValueError("Model not found")
        self._clear_cache()

    def _get_info_list(self, model_name):
        if model_name is None:
            id_or_name = str(self._msg.registered_model_id)
        else :
            id_or_name = model_name
        return [self._msg.version, str(self.id), id_or_name, _utils.timestamp_to_str(self._msg.time_updated)]
