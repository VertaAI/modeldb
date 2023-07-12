# -*- coding: utf-8 -*-

from __future__ import print_function

import ast
import json
import logging
import os
import pathlib
import pickle
import tempfile
from typing import List
import warnings

from google.protobuf.struct_pb2 import Value

import requests

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb.versioning import (
    VersioningService_pb2 as _VersioningService,
)
from verta._protos.public.registry import (
    RegistryService_pb2 as _RegistryService,
    StageService_pb2 as _StageService,
)

from verta._vendored import six

from verta._internal_utils import (
    _artifact_utils,
    _request_utils,
    _utils,
    arg_handler,
    importer,
    time_utils,
)
from verta import utils

from verta import _blob, code, data_types, environment
from verta.endpoint.build import Build
from verta.tracking.entities._entity import _MODEL_ARTIFACTS_ATTR_KEY
from verta.tracking.entities import _deployable_entity
from .. import lock, DockerImage
from ..stage_change import _StageChange

from verta.dataset.entities import _dataset_version

logger = logging.getLogger(__name__)


class RegisteredModelVersion(_deployable_entity._DeployableEntity):
    """
    Object representing a version of a Registered Model.

    There should not be a need to instantiate this class directly; please use
    :meth:`RegisteredModel.get_or_create_version()
    <verta.registry.entities.RegisteredModel.get_or_create_version>`.

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
    stage : str
        Model version stage.
    url : str
        Verta web app URL.

    """

    ModelVersionMessage = _RegistryService.ModelVersion

    def __init__(self, conn, conf, msg):
        super(RegisteredModelVersion, self).__init__(
            conn, conf, _RegistryService, "registered_model_version", msg
        )

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg
        artifact_keys = self.get_artifact_keys()
        if self.has_model:
            artifact_keys.append(self._MODEL_KEY)

        return "\n".join(
            (
                "version: {}".format(msg.version),
                "stage: {}".format(self.stage),
                "lock level: {}".format(
                    _RegistryService.ModelVersionLockLevelEnum.ModelVersionLockLevel.Name(
                        msg.lock_level
                    ).lower()
                ),
                "url: {}".format(self.url),
                "time created: {}".format(
                    _utils.timestamp_to_str(int(msg.time_created))
                ),
                "time updated: {}".format(
                    _utils.timestamp_to_str(int(msg.time_updated))
                ),
                "description: {}".format(msg.description),
                "labels: {}".format(msg.labels),
                "attributes: {}".format(
                    {
                        key: val
                        for key, val in _utils.unravel_key_values(
                            msg.attributes
                        ).items()
                        if not key.startswith(_deployable_entity._INTERNAL_ATTR_PREFIX)
                    }
                ),
                "input description: {}".format(msg.input_description),
                "hide input label: {}".format(msg.hide_input_label),
                "output description: {}".format(msg.output_description),
                "hide output label: {}".format(msg.hide_output_label),
                "id: {}".format(msg.id),
                "registered model id: {}".format(msg.registered_model_id),
                "experiment run id: {}".format(msg.experiment_run_id),
                # "archived status: {}".format(msg.archived == _CommonCommonService.TernaryEnum.TRUE),
                "artifact keys: {}".format(sorted(artifact_keys)),
                "dataset version keys: {}".format(
                    sorted(dataset.key for dataset in msg.datasets)
                ),
                "code version keys: {}".format(sorted(msg.code_blob_map.keys())),
            )
        )

    @property
    def _MODEL_KEY(self):
        return _artifact_utils.REGISTRY_MODEL_KEY

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.version

    @property
    def has_model(self):
        self._refresh_cache()
        return bool(self._msg.model) and bool(self._msg.model.key)

    @property
    def registered_model_id(self):
        self._refresh_cache()
        return self._msg.registered_model_id

    @property
    def stage(self):
        self._refresh_cache()
        return _StageService.StageEnum.Stage.Name(self._msg.stage).lower()

    # @property
    # def is_archived(self):
    #     self._refresh_cache()
    #     return self._msg.archived == _CommonCommonService.TernaryEnum.TRUE

    @property
    def url(self):
        return "{}://{}/{}/registry/{}/versions/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.registered_model_id,
            self.id,
        )

    @property
    def workspace(self):
        self._refresh_cache()
        Message = _RegistryService.GetRegisteredModelRequest
        response = self._conn.make_proto_request(
            "GET",
            "/api/v1/registry/registered_models/{}".format(self.registered_model_id),
        )

        registered_model_msg = self._conn.maybe_proto_response(
            response, Message.Response
        ).registered_model

        if registered_model_msg.workspace_id:
            return self._conn.get_workspace_name_from_id(
                registered_model_msg.workspace_id
            )
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

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
        Message = _RegistryService.GetModelVersionRequest
        endpoint = "/api/v1/registry/model_versions/{}".format(id)
        response = conn.make_proto_request("GET", endpoint)

        return conn.maybe_proto_response(response, Message.Response).model_version

    @classmethod
    def _get_proto_by_name(cls, conn, name, registered_model_id):
        if isinstance(name, six.string_types):
            value = Value(string_value=name)
        else:
            raise TypeError("`name` must be a string")

        Message = _RegistryService.FindModelVersionRequest
        predicates = [
            _CommonCommonService.KeyValueQuery(
                key="version",
                value=value,
                operator=_CommonCommonService.OperatorEnum.EQ,
            )
        ]
        endpoint = "/api/v1/registry/registered_models/{}/model_versions/find".format(
            registered_model_id
        )
        msg = Message(predicates=predicates)

        proto_response = conn.make_proto_request("POST", endpoint, body=msg)
        response = conn.maybe_proto_response(proto_response, Message.Response)

        if not response.model_versions:
            return None

        # should only have 1 entry here, as name/version is unique
        return response.model_versions[0]

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        desc=None,
        tags=None,
        attrs=None,
        date_created=None,
        experiment_run_id=None,
        lock_level=None,
        input_description=None,
        hide_input_label=None,
        output_description=None,
        hide_output_label=None,
    ):
        if lock_level is None:
            lock_level = lock.Open()
        registered_model_id = ctx.registered_model.id

        msg = cls.ModelVersionMessage(
            registered_model_id=registered_model_id,
            version=name,
            description=desc,
            labels=tags,
            attributes=attrs,
            time_created=date_created,
            time_updated=date_created,
            experiment_run_id=experiment_run_id,
            lock_level=lock_level._as_proto(),
            input_description=input_description,
            hide_input_label=hide_input_label,
            output_description=output_description,
            hide_output_label=hide_output_label,
        )
        endpoint = "/api/v1/registry/registered_models/{}/model_versions".format(
            registered_model_id
        )
        response = conn.make_proto_request("POST", endpoint, body=msg)
        model_version = conn.must_proto_response(
            response, _RegistryService.SetModelVersion.Response
        ).model_version

        print("created new ModelVersion: {}".format(model_version.version))
        return model_version

    def _get_artifact_msg(self, key):
        self._refresh_cache()

        if key == self._MODEL_KEY:
            if not self.has_model:
                raise KeyError("no model associated with this version")
            return self._msg.model

        for artifact_msg in self._msg.artifacts:
            if artifact_msg.key == key:
                return artifact_msg

        raise KeyError("no artifact found with key {}".format(key))

    def change_stage(self, stage_change):
        """Change this model version's stage, bypassing the approval cycle.

        .. versionadded:: 0.19.2

        .. note::

            User must have read-write permissions.

        Parameters
        ----------
        stage_change : :mod:`~verta.registry.stage_change`
            Desired stage change.

        Returns
        -------
        str
            This model version's new stage.

        Examples
        --------
        See documentation for individual stage change objects for usage
        examples.

        """
        if not isinstance(stage_change, _StageChange):
            raise TypeError(
                "`stage_change` must be an object from `verta.registry.stage_change`,"
                " not {}".format(type(stage_change))
            )

        msg = stage_change._to_proto_request(self.id)
        endpoint = "/api/v1/registry/stage/updateStage"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()

        return self.stage

    def log_docker(
        self,
        docker_image,
        model_api=None,
        overwrite=False,
    ):
        """Log Docker image information for deployment.

        .. versionadded:: 0.20.0

        .. note::

            |experimental|

        .. note::

            This method cannot be used alongside :meth:`log_environment`.

        Parameters
        ----------
        docker_image : :class:`~verta.registry.DockerImage`
            Docker image information.
        model_api : :class:`~verta.utils.ModelAPI`, optional
            Model API specifying the model's expected input and output
        overwrite : bool, default False
            Whether to allow overwriting existing Docker image information.

        Examples
        --------
        .. code-block:: python

            from verta.registry import DockerImage

            model_ver.log_docker(
                DockerImage(
                    port=5000,
                    request_path="/predict_json",
                    health_path="/health",

                    repository="012345678901.dkr.ecr.apne2-az1.amazonaws.com/models/example",
                    tag="example",

                    env_vars={"CUDA_VISIBLE_DEVICES": "0,1"},
                )
            )

        """
        if not isinstance(docker_image, DockerImage):
            raise TypeError(
                "`docker_image` must be type verta.registry.DockerImage,"
                " not {}".format(type(docker_image))
            )
        if model_api and not isinstance(model_api, utils.ModelAPI):
            raise ValueError(
                "`model_api` must be `verta.utils.ModelAPI`, not {}".format(
                    type(model_api)
                )
            )

        # check for conflict
        if not overwrite:
            self._refresh_cache()
            if self._msg.docker_metadata.request_port:
                raise ValueError(
                    "Docker image information already exists;"
                    " consider setting overwrite=True"
                )
            if self.has_environment:
                raise ValueError(
                    "environment already exists;" " consider setting overwrite=True"
                )

        # log model API (first, in case there's a conflict)
        if model_api:
            self.log_artifact(
                _artifact_utils.MODEL_API_KEY,
                model_api,
                overwrite,
                "json",
            )

        # log docker
        if overwrite:
            self._fetch_with_no_cache()
            docker_image._merge_into_model_ver_proto(self._msg)
            self._update(self._msg, method="PUT")
        else:
            self._update(
                docker_image._as_model_ver_proto(),
                method="PATCH",
                update_mask={"paths": ["docker_metadata", "environment"]},
            )

    def get_docker(self):
        """Get logged Docker image information.

        Returns
        -------
        :class:`~verta.registry.DockerImage`

        """
        self._refresh_cache()
        if not self._msg.docker_metadata.request_port:
            raise ValueError("Docker image information has not been logged")

        return DockerImage._from_model_ver_proto(self._msg)

    def log_model(
        self,
        model,
        custom_modules=None,
        model_api=None,
        artifacts=None,
        overwrite=False,
    ):
        if self.has_model and not overwrite:
            raise ValueError("model already exists; consider setting overwrite=True")

        if model_api and not isinstance(model_api, utils.ModelAPI):
            raise ValueError(
                "`model_api` must be `verta.utils.ModelAPI`, not {}".format(
                    type(model_api)
                )
            )
        if artifacts is not None and not (
            isinstance(artifacts, list)
            and all(
                isinstance(artifact_key, six.string_types) for artifact_key in artifacts
            )
        ):
            raise TypeError(
                "`artifacts` must be list of str, not {}".format(type(artifacts))
            )

        # validate that `artifacts` are actually logged
        if artifacts:
            self._refresh_cache()
            run_msg = self._msg
            existing_artifact_keys = {artifact.key for artifact in run_msg.artifacts}
            unlogged_artifact_keys = set(artifacts) - existing_artifact_keys
            if unlogged_artifact_keys:
                raise ValueError(
                    "`artifacts` contains keys that have not been logged: {}".format(
                        sorted(unlogged_artifact_keys)
                    )
                )

        # associate artifact dependencies
        if artifacts:
            self.add_attribute(
                _MODEL_ARTIFACTS_ATTR_KEY, artifacts, overwrite=overwrite
            )

        serialized_model, method, model_type = _artifact_utils.serialize_model(model)

        if artifacts and model_type != "class":
            raise ValueError("`artifacts` can only be provided if `model` is a class")

        # Create artifact message and update ModelVersion's message:
        model_msg = self._create_artifact_msg(
            self._MODEL_KEY,
            serialized_model,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.MODEL,
            method=method,
            framework=model_type,
        )
        model_version_update = self.ModelVersionMessage(model=model_msg)
        self._update(model_version_update)

        # Upload the artifact to ModelDB:
        self._upload_artifact(
            self._MODEL_KEY,
            serialized_model,
            _CommonCommonService.ArtifactTypeEnum.MODEL,
        )

        # create and upload model API
        if model_type or model_api:  # only if provided or model is deployable
            if model_api is None:
                model_api = utils.ModelAPI()
            self.log_artifact(
                _artifact_utils.MODEL_API_KEY, model_api, overwrite, "json"
            )

        # create and upload custom modules
        if model_type or custom_modules:  # only if provided or model is deployable
            # Log modules:
            custom_modules_artifact = self._custom_modules_as_artifact(custom_modules)
            self.log_artifact(
                _artifact_utils.CUSTOM_MODULES_KEY,
                custom_modules_artifact,
                overwrite,
                "zip",
            )

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
        model_artifact = self._get_artifact(
            self._MODEL_KEY,
            _CommonCommonService.ArtifactTypeEnum.MODEL,
        )
        return _artifact_utils.deserialize_model(model_artifact, error_ok=True)

    def download_model(self, download_to_path):
        return self.download_artifact(self._MODEL_KEY, download_to_path)

    def del_model(self):
        """
        Deletes model of this Model Version.

        """
        self._fetch_with_no_cache()
        self._msg.ClearField("model")
        self._update(self._msg, method="PUT")

    def log_artifact(self, key, artifact, overwrite=False, _extension=None):
        """
        Logs an artifact to this Model Version.

        .. note::

            The following artifact keys are reserved for internal use within the
            Verta system:

            - ``"custom_modules"``
            - ``"model"``
            - ``"model.pkl"``
            - ``"model_api.json"``
            - ``"requirements.txt"``
            - ``"train_data"``
            - ``"tf_saved_model"``
            - ``"setup_script"``

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
        # TODO: should validate keys, but can't here because this public
        #       method is also used to log internal artifacts
        # _artifact_utils.validate_key(key)
        if key == self._MODEL_KEY:
            raise ValueError(
                'the key "{}" is reserved for model;'
                " consider using log_model() instead".format(self._MODEL_KEY)
            )

        self._fetch_with_no_cache()
        same_key_ind = -1

        for i in range(len(self._msg.artifacts)):
            if self._msg.artifacts[i].key == key:
                if not overwrite:
                    raise ValueError(
                        "The key has been set; consider setting overwrite=True"
                    )
                else:
                    same_key_ind = i
                break

        artifact_type = _CommonCommonService.ArtifactTypeEnum.BLOB

        if isinstance(artifact, six.string_types):
            if os.path.isdir(artifact):  # zip dirpath
                artifact = _artifact_utils.zip_dir(artifact)
            else:  # open filepath
                artifact = open(artifact, "rb")
        artifact_stream, method = _artifact_utils.ensure_bytestream(artifact)

        artifact_msg = self._create_artifact_msg(
            key,
            artifact_stream,
            artifact_type=artifact_type,
            method=method,
            extension=_extension,
        )
        if same_key_ind == -1:
            self._msg.artifacts.append(artifact_msg)
        else:
            self._msg.artifacts[same_key_ind].CopyFrom(artifact_msg)

        self._update(self._msg, method="PUT")
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

    def download_artifact(self, key, download_to_path):
        download_to_path = os.path.abspath(download_to_path)
        artifact = self._get_artifact_msg(key)

        # create parent dirs
        pathlib.Path(download_to_path).parent.mkdir(parents=True, exist_ok=True)
        # TODO: clean up empty parent dirs if something later fails

        # get a stream of the file bytes, without loading into memory, and write to file
        logger.info("downloading %s from Registry", key)
        url = self._get_url_for_artifact(key, "GET").url
        with _utils.make_request("GET", url, self._conn, stream=True) as response:
            _utils.raise_for_http_error(response)

            if (
                artifact.filename_extension == _artifact_utils.ZIP_EXTENSION
            ):  # verta-created ZIP
                downloader = _request_utils.download_zipped_dir
            else:
                downloader = _request_utils.download_file
            downloader(response, download_to_path, overwrite_ok=True)

        return download_to_path

    def del_artifact(self, key):
        """
        Deletes the artifact with name `key` from this Model Version.

        Parameters
        ----------
        key : str
            Name of the artifact.

        """
        if key == self._MODEL_KEY:
            raise ValueError(
                "model can't be deleted through del_artifact(); consider using del_model() instead"
            )

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
        self._update(self._msg, method="PUT")

    def log_environment(self, env, overwrite=False):
        if not isinstance(env, environment._Environment):
            raise TypeError(
                "`env` must be of type Environment, not {}".format(type(env))
            )

        if self.has_environment and not overwrite:
            raise ValueError(
                "environment already exists; consider setting overwrite=True"
            )

        if overwrite:
            self._fetch_with_no_cache()
            self._msg.environment.CopyFrom(env._as_env_proto())
            self._update(self._msg, method="PUT")
        else:
            self._update(
                self.ModelVersionMessage(environment=env._as_env_proto()),
                method="PATCH",
                update_mask={"paths": ["environment"]},
            )

    def del_environment(self):
        """
        Deletes the environment of this Model Version.

        """
        self._fetch_with_no_cache()
        self._msg.ClearField("environment")
        self._update(self._msg, method="PUT")

    def _get_url_for_artifact(self, key, method, artifact_type=0, part_num=0):
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _RegistryService.GetUrlForArtifact
        msg = Message(
            model_version_id=self.id,
            key=key,
            method=method,
            artifact_type=artifact_type,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/registry/model_versions/{}/getUrlForArtifact".format(
            self._conn.scheme, self._conn.socket, self.id
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)
        return _utils.json_to_proto(response.json(), Message.Response)

    def _upload_artifact(
        self, key, file_handle, artifact_type, part_size=_artifact_utils._64MB
    ):
        file_handle.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(
            key, "PUT", artifact_type, part_num=1
        )

        print("uploading {} to Registry".format(key))
        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: file_handle.read(part_size), b"")
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end="\r")

                # get presigned URL
                url = self._get_url_for_artifact(
                    key, "PUT", artifact_type, part_num=part_num
                ).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                _utils.raise_for_http_error(response)

                # commit part
                url = "{}://{}/api/v1/registry/model_versions/{}/commitArtifactPart".format(
                    self._conn.scheme, self._conn.socket, self.id
                )
                msg = _RegistryService.CommitArtifactPart(
                    model_version_id=self.id, key=key
                )
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers["ETag"]
                data = _utils.proto_to_json(msg)
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/registry/model_versions/{}/commitMultipartArtifact".format(
                self._conn.scheme, self._conn.socket, self.id
            )
            msg = _RegistryService.CommitMultipartArtifact(
                model_version_id=self.id, key=key
            )
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            if url_for_artifact.fields:
                # if fields were returned by backend, make a POST request and supply them as form fields
                response = _utils.make_request(
                    "POST",
                    url_for_artifact.url,
                    self._conn,
                    # requests uses the `files` parameter for sending multipart/form-data POSTs.
                    #     https://stackoverflow.com/a/12385661/8651995
                    # the file contents must be the final form field
                    #     https://docs.aws.amazon.com/AmazonS3/latest/dev/HTTPPOSTForms.html#HTTPPOSTFormFields
                    files=list(url_for_artifact.fields.items())
                    + [("file", file_handle)],
                )
            else:
                response = _utils.make_request(
                    "PUT", url_for_artifact.url, self._conn, data=file_handle
                )
            _utils.raise_for_http_error(response)

        print("upload complete")

    def _get_artifact(self, key, artifact_type=0):
        # check to see if key exists
        self._get_artifact_msg(key)

        # download artifact from artifact store
        url = self._get_url_for_artifact(key, "GET", artifact_type).url

        response = _utils.make_request("GET", url, self._conn)
        _utils.raise_for_http_error(response)

        return response.content

    def set_description(self, desc):
        if not desc:
            raise ValueError("desc is not specified")
        self._update(self.ModelVersionMessage(description=desc))

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

        self._update(self.ModelVersionMessage(labels=labels))

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
        self._update(self.ModelVersionMessage(labels=[label]))

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
            self._update(self._msg, method="PUT")

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

    def log_setup_script(self, script, overwrite=False):
        """
        Associate a model deployment setup script with this Experiment Run.

        .. versionadded:: 0.13.8

        Parameters
        ----------
        script : str
            String composed of valid Python code for executing setup steps at the beginning of model
            deployment. An on-disk file can be passed in using ``open("path/to/file.py", 'r').read()``.
        overwrite : bool, default False
            Whether to allow overwriting an existing setup script.

        Raises
        ------
        SyntaxError
            If `script` contains invalid Python.

        """
        # validate `script`'s syntax
        try:
            ast.parse(script)
        except SyntaxError as e:
            # clarify that the syntax error comes from `script`, and propagate details
            reason = e.args[0]
            line_no = e.args[1][1]
            line = script.splitlines()[line_no - 1]
            six.raise_from(
                SyntaxError(
                    "{} in provided script on line {}:\n{}".format(
                        reason, line_no, line
                    )
                ),
                e,
            )

        # convert into bytes for upload
        script = six.ensure_binary(script)

        # convert to file-like for `_log_artifact()`
        script = six.BytesIO(script)

        self.log_artifact(
            "setup_script",
            script,
            overwrite,
            "py",
        )

    def download_docker_context(self, download_to_path, self_contained=False):
        """
        Downloads this Model Version's Docker context ``tgz``.

        Parameters
        ----------
        download_to_path : str
            Path to download Docker context to.
        self_contained : bool, default False
            Whether the downloaded Docker context should be self-contained.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where Docker context was downloaded to. Matches `download_to_path`.

        """
        self._refresh_cache()
        endpoint = "{}://{}/api/v1/deployment/builds/dockercontext".format(
            self._conn.scheme,
            self._conn.socket,
        )
        body = {
            "model_version_id": self.id,
            "self_contained": self_contained,
        }

        with _utils.make_request(
            "POST", endpoint, self._conn, json=body, stream=True
        ) as response:
            try:
                _utils.raise_for_http_error(response)
            except requests.HTTPError as e:
                # propagate error caused by missing artifact
                error_text = e.response.text.strip()
                if error_text.startswith("missing artifact"):
                    new_e = RuntimeError(
                        "unable to obtain Docker context due to " + error_text
                    )
                    six.raise_from(new_e, None)
                else:
                    raise e

            downloaded_to_path = _request_utils.download_file(
                response, download_to_path, overwrite_ok=True
            )
            return os.path.abspath(downloaded_to_path)

    # def archive(self):
    #     """
    #     Archive this Model Version.

    #     """
    #     if self.is_archived:
    #         raise RuntimeError("the version has already been archived")

    #     self._update(self.ModelVersionMessage(archived=_CommonCommonService.TernaryEnum.TRUE))

    def add_attribute(self, key, value, overwrite=False):
        """
        Adds an attribute to this Model Version.

        Parameters
        ----------
        key : str
            Name of the attribute.
        value : one of {None, bool, float, int, str, list, dict}
            Value of the attribute.
        overwrite : bool, default False
            Whether to allow overwriting an existing attribute with key `key`.

        """
        self.add_attributes({key: value}, overwrite)

    def add_attributes(self, attrs, overwrite=False):
        """
        Adds potentially multiple attributes to this Model Version.

        Parameters
        ----------
        attrs : dict of str to {None, bool, float, int, str, list, dict}
            Attributes.
        overwrite : bool, default False
            Whether to allow overwriting an existing attribute with key `key`.

        """
        # validate all keys first
        for key in six.viewkeys(attrs):
            _utils.validate_flat_key(key)
        # convert data_types to dicts
        for key, value in attrs.items():
            if isinstance(value, data_types._VertaDataType):
                attrs[key] = value._as_dict()

        # build KeyValues
        attribute_keyvals = []
        existing_attrs = self.get_attributes()
        for key, value in six.viewitems(attrs):
            if key in existing_attrs and not overwrite:
                warnings.warn(
                    "skipping attribute {} which already exists;"
                    " set `overwrite=True` to overwrite".format(key)
                )
                continue

            attribute_keyvals.append(
                _CommonCommonService.KeyValue(
                    key=key,
                    value=_utils.python_to_val_proto(value, allow_collection=True),
                )
            )

        self._update(self.ModelVersionMessage(attributes=attribute_keyvals))

    def get_attribute(self, key):
        """
        Gets the attribute with name `key` from this Model Version.

        Parameters
        ----------
        key : str
            Name of the attribute.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the attribute.

        """
        _utils.validate_flat_key(key)
        attributes = self.get_attributes()

        try:
            return attributes[key]
        except KeyError:
            six.raise_from(KeyError("no attribute found with key {}".format(key)), None)

    def get_attributes(self):
        """
        Gets all attributes from this Model Version.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
        self._refresh_cache()
        attributes = _utils.unravel_key_values(self._msg.attributes)
        for key, attribute in attributes.items():
            try:
                attributes[key] = data_types._VertaDataType._from_dict(attribute)
            except (KeyError, TypeError, ValueError):
                pass
        return attributes

    def _get_attribute_keys(self):
        return list(map(lambda attribute: attribute.key, self.get_attributes()))

    def del_attribute(self, key):
        """
        Deletes the attribute with name `key` from this Model Version

        Parameters
        ----------
        key : str
            Name of the attribute.

        """
        _utils.validate_flat_key(key)

        self._fetch_with_no_cache()
        attributes = list(
            filter(lambda attribute: attribute.key == key, self._msg.attributes)
        )
        if attributes:
            self._msg.attributes.remove(attributes[0])
            self._update(self._msg, method="PUT")

    def set_lock_level(self, lock_level):
        """
        Sets this model version's lock level

        Parameters
        ----------
        lock_level : :mod:`~verta.registry.lock`
            Lock level to set.

        """
        if not isinstance(lock_level, lock._LockLevel):
            raise TypeError(
                "`lock_level` must be an object from `verta.registry.lock`,"
                " not {}".format(type(lock_level))
            )

        msg = _RegistryService.SetLockModelVersionRequest(
            lock_level=lock_level._as_proto(),
        )
        endpoint = "/api/v1/registry/model_versions/{}/lock".format(self.id)
        response = self._conn.make_proto_request("PUT", endpoint, body=msg)
        self._conn.must_proto_response(response, msg.Response)

    def get_lock_level(self):
        """
        Gets this model version's lock level.

        Returns
        -------
        lock_level : :mod:`~verta.registry.lock`
            This model version's lock level.

        """
        self._refresh_cache()
        return lock._LockLevel._from_proto(self._msg.lock_level)

    def _update(self, msg, method="PATCH", update_mask=None):
        self._refresh_cache()  # to have `self._msg.registered_model_id` for URL
        if update_mask:
            url = "{}://{}/api/v1/registry/registered_models/{}/model_versions/{}/full_body".format(
                self._conn.scheme,
                self._conn.socket,
                self._msg.registered_model_id,
                self.id,
            )
            # proto converter for update_mask is missing
            data = {
                "model_version": _utils.proto_to_json(msg, False),
                "update_mask": update_mask,
            }
            response = _utils.make_request(method, url, self._conn, json=data)
        else:
            response = self._conn.make_proto_request(
                method,
                "/api/v1/registry/registered_models/{}/model_versions/{}".format(
                    self._msg.registered_model_id, self.id
                ),
                body=msg,
                include_default=False,
            )
        self._conn.must_proto_response(
            response, _RegistryService.SetModelVersion.Response
        )
        self._clear_cache()

    def _get_info_list(self, model_name):
        if model_name is None:
            id_or_name = str(self._msg.registered_model_id)
        else:
            id_or_name = model_name
        return [
            self._msg.version,
            str(self.id),
            id_or_name,
            _utils.timestamp_to_str(self._msg.time_updated),
        ]

    def delete(self):
        """
        Deletes this model version.

        .. versionadded:: 0.17.3

        """
        endpoint = "/api/v1/registry/model_versions/{}".format(self.id)
        response = self._conn.make_proto_request("DELETE", endpoint)
        self._conn.must_response(response)

    def log_code_version(self, key, code_version):
        """Log a code version snapshot.

        .. versionadded:: 0.19.0

        Parameters
        ----------
        key : str
            Name for the code version.
        code_version : `code <verta.code.html>`__
            Code version.

        Examples
        --------
        .. code-block:: python

            from verta.code import Git

            training_code = Git(
                repo_url="git@github.com:VertaAI/models.git",
                commit_hash="52f3d22",
                autocapture=False,
            )
            inference_code = Git(
                repo_url="git@github.com:VertaAI/data-processing.git",
                commit_hash="26f9787",
                autocapture=False,
            )

            model_ver.log_code_version("training", training_code)
            model_ver.log_code_version("inference_code", inference_code)

        """
        self.log_code_versions({key: code_version})

    def log_code_versions(self, code_versions):
        """Log multiple code version snapshots in a batched request.

        .. versionadded:: 0.19.0

        Parameters
        ----------
        code_versions : dict of str to `code <verta.code.html>`__
            Code versions mapped to names.

        Examples
        --------
        .. code-block:: python

            from verta.code import Git

            code_versions = {
                "training": Git(
                    repo_url="git@github.com:VertaAI/models.git",
                    commit_hash="52f3d22",
                    autocapture=False,
                ),
                "inference_code": Git(
                    repo_url="git@github.com:VertaAI/data-processing.git",
                    commit_hash="26f9787",
                    autocapture=False,
                ),
            }

            model_ver.log_code_versions(code_versions)

        """
        for key, code_version in code_versions.items():
            if not isinstance(key, six.string_types):
                raise TypeError("key must be str, not {}".format(type(key)))
            if not isinstance(code_version, code._Code):
                raise TypeError(
                    "code version must be an object from verta.code,"
                    " not {}".format(type(code_version))
                )

        msg = _RegistryService.LogCodeBlobInModelVersion(
            model_version_id=self.id,
            code_blob_map={
                key: code_version._as_proto().code
                for key, code_version in code_versions.items()
            },
        )
        endpoint = (
            "/api/v1/registry/model_versions/{}/logCodeBlobInModelVersion".format(
                self.id,
            )
        )
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()

    def get_code_version(self, key):
        """Get a code version snapshot.

        .. versionadded:: 0.19.0

        Parameters
        ----------
        key : str
            Name of the code version.

        Returns
        -------
        `code <verta.code.html>`__
            Code version.

        Examples
        --------
        .. code-block:: python

            model_ver.get_code_version("training")
            # Git Version
            #     commit 52f3d22
            #     in repo git@github.com:VertaAI/models.git

        """
        code_versions = self.get_code_versions()

        try:
            return code_versions[key]
        except KeyError:
            raise KeyError("no code version found with key {}".format(key))

    def get_code_versions(self):
        """Get all code version snapshots.

        .. versionadded:: 0.19.0

        Returns
        -------
        dict of str to `code <verta.code.html>`__
            Code versions mapped to names.

        Examples
        --------
        .. code-block:: python

            model_ver.get_code_versions()
            # {'training': Git Version
            #      commit 52f3d22
            #      in repo git@github.com:VertaAI/models.git,
            #  'inference_code': Git Version
            #      commit 26f9787
            #      in repo git@github.com:VertaAI/data-processing.git}

        """
        self._refresh_cache()

        code_versions = dict()
        for key, code_blob in self._msg.code_blob_map.items():
            # create wrapper blob msg so we can reuse the blob system's proto-to-obj
            blob = _VersioningService.Blob()
            blob.code.CopyFrom(code_blob)
            content = _blob.Blob.blob_msg_to_object(blob)

            code_versions[key] = content

        return code_versions

    def log_reference_data(self, X, Y, overwrite=False):
        """Log tabular reference data.

        Parameters
        ----------
        X : pd.DataFrame
            Reference data inputs.
        Y : pd.DataFrame
            Reference data outputs.
        overwrite : bool, default False
            Whether to allow overwriting existing reference data.

        """
        pd = importer.maybe_dependency("pandas")
        if pd is None:
            raise ImportError("pandas is not installed; try `pip install pandas`")

        if isinstance(X, pd.Series):
            X = X.to_frame()
        if isinstance(Y, pd.Series):
            Y = Y.to_frame()
        if not isinstance(X, pd.DataFrame):
            raise TypeError("`X` must be a DataFrame, not {}".format(type(X)))
        if not isinstance(Y, pd.DataFrame):
            raise TypeError("`Y` must be a DataFrame, not {}".format(type(Y)))

        df = pd.DataFrame()
        for c in X.columns:
            df["input." + str(c)] = X[c]
        for c in Y.columns:
            df["output." + str(c)] = Y[c]
        df["source"] = "reference"
        df["model_version_id"] = self.id

        tempf = tempfile.NamedTemporaryFile(suffix=".csv", delete=False)
        try:
            df.to_csv(tempf.name, encoding="utf-8", index=False)
            self.log_artifact("reference_data", tempf.name, overwrite=overwrite)
        finally:
            os.remove(tempf.name)

    def set_input_description(self, desc):
        """
        Sets this description of the model version's input.

        This field helps users have a quick view of what type of data will be used as input for a model.
        This field also helps non-tech users to understand model behavior at a glance.

        Parameters
        ----------
        desc : str

        """
        if not desc:
            raise ValueError("input description is not specified")
        self._update(self.ModelVersionMessage(input_description=desc))

    def get_input_description(self):
        """
        Gets this description of the model version's input.

        This field helps users have a quick view of what type of data will be used as input for a model.
        This field also helps non-tech users to understand model behavior at a glance.

        Returns
        -------
        desc : str

        """
        self._refresh_cache()
        return self._msg.input_description

    def set_hide_input_label(self, hide):
        """
        Sets whether to hide the model version's input label on the preview.

        Parameters
        ----------
        hide : bool

        """
        if not hide:
            raise ValueError("hide input label is not specified")
        self._update(self.ModelVersionMessage(hide_input_label=hide))

    def get_hide_input_label(self):
        """
        Gets whether to hide the model version's input label on the preview.

        Returns
        -------
        hide : bool

        """
        self._refresh_cache()
        return self._msg.hide_input_label

    def set_output_description(self, desc):
        """
        Sets this description of the model version's output.

        This field helps users have a quick view of what type of data will be produced as a result of executing a model.
        This field also helps non-tech users to understand model behavior at a glance.

        Parameters
        ----------
        desc : str

        """
        if not desc:
            raise ValueError("output description is not specified")
        self._update(self.ModelVersionMessage(output_description=desc))

    def get_output_description(self):
        """
        Gets this description of the model version's output.

        This field helps users have a quick view of what type of data will be produced as a result of executing a model.
        This field also helps non-tech users to understand model behavior at a glance.

        Returns
        -------
        desc : str

        """
        self._refresh_cache()
        return self._msg.output_description

    def set_hide_output_label(self, hide):
        """
        Sets whether to hide the model version's output label on the preview.

        Parameters
        ----------
        hide : bool

        """
        if not hide:
            raise ValueError("hide output label is not specified")
        self._update(self.ModelVersionMessage(hide_output_label=hide))

    def get_hide_output_label(self):
        """
        Gets whether to hide the model version's output label on the preview.

        Returns
        -------
        hide : bool

        """
        self._refresh_cache()
        return self._msg.hide_output_label

    def log_dataset_version(self, key, dataset_version):
        """
        Logs a Verta DatasetVersion to this Model Version with the given key.

        .. versionadded:: 0.21.1

        Parameters
        ----------
        key : str
            Name of the dataset version.
        dataset_version : :class:`~verta.dataset.entities.DatasetVersion`
            Dataset version.

        """
        if not isinstance(dataset_version, _dataset_version.DatasetVersion):
            raise TypeError("`dataset_version` must be of type DatasetVersion")

        artifact_msg = _CommonCommonService.Artifact(
            key=key,
            path_only=True,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.DATA,
            linked_artifact_id=dataset_version.id,
        )

        msg = _RegistryService.LogDatasetsInModelVersion(
            model_version_id=self.id,
            datasets=[artifact_msg],
        )

        endpoint = "/api/v1/registry/model_versions/{}/logDatasets".format(
            self.id,
        )

        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()

    def get_dataset_version(self, key):
        """
        Gets the DatasetVersion with name `key` from this Model Version.

        .. versionadded:: 0.21.1

        Parameters
        ----------
        key : str
            Name of the dataset version.

        Returns
        -------
        :class:`~verta.dataset.entities.DatasetVersion`
            DatasetVersion associated with the given key.

        """
        self._refresh_cache()

        for dataset in self._msg.datasets:
            if dataset.key == key:
                return _dataset_version.DatasetVersion(
                    self._conn,
                    self._conf,
                    _dataset_version.DatasetVersion._get_proto_by_id(
                        self._conn, dataset.linked_artifact_id
                    ),
                )

        raise KeyError("no dataset found with key {}".format(key))

    def get_dataset_versions(self):
        """
        Gets all DatasetVersions associated with this Model Version.

        Returns
        -------
        list of :class:`~verta.dataset.entities.DatasetVersion`
            DatasetVersions associated with this Model Version.
        """
        self._refresh_cache()

        dataset_versions = list()

        for dataset in self._msg.datasets:
            dataset_versions.append(
                _dataset_version.DatasetVersion(
                    self._conn,
                    self._conf,
                    _dataset_version.DatasetVersion._get_proto_by_id(
                        self._conn, dataset.linked_artifact_id
                    ),
                )
            )
        return dataset_versions

    def del_dataset_version(self, key):
        """
        Deletes the DatasetVersion with name `key` from this Model Version.

        .. versionadded:: 0.21.1

        Parameters
        ----------
        key : str
            Name of dataset version.

        """
        self._fetch_with_no_cache()

        ind = -1
        for i in range(len(self._msg.datasets)):
            dataset = self._msg.datasets[i]
            if dataset.key == key:
                ind = i
                break

        if ind == -1:
            raise KeyError("no dataset found with key {}".format(key))

        del self._msg.datasets[ind]
        self._update(self._msg, method="PUT")

    def list_builds(self) -> List[Build]:
        """
        Gets this model version's past and present builds.

        .. versionadded:: 0.23.0

        Builds are returned in order of creation (most recent first).

        Returns
        -------
        list of :class:`~verta.endpoint.build.Build`

        Examples
        --------
        To fetch builds that have passed their scans:

        .. code-block:: python

            passed_builds = list(filter(
                lambda build: build.get_scan().passed,
                model_ver.list_builds(),
            ))

        To fetch builds that haven't been scanned in a while:

        .. code-block:: python

            from datetime import datetime, timedelta

            past_builds = list(filter(
                lambda build: build.get_scan().date_updated < datetime.now().astimezone() - timedelta(days=30),
                model_ver.list_builds(),
            ))

        """
        builds = Build._list_model_version_builds(self._conn, self.workspace, self.id)
        return sorted(builds, key=lambda build: build.date_created, reverse=True)
