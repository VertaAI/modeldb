# -*- coding: utf-8 -*-

from __future__ import print_function

import ast
import copy
import glob
import os
import pathlib2
import pprint
import shutil
import sys
import shutil
import tarfile
import tempfile
import time
import warnings
import zipfile

import requests

from .entity import _ModelDBEntity, _OSS_DEFAULT_WORKSPACE

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import CommonService_pb2 as _CommonService
from .._protos.public.modeldb import ExperimentRunService_pb2 as _ExperimentRunService

from ..external import six
from ..external.six.moves import cPickle as pickle  # pylint: disable=import-error, no-name-in-module

from .._internal_utils import (
    _artifact_utils,
    _histogram_utils,
    _pip_requirements_utils,
    _request_utils,
    _utils,
    importer,
)

from .. import _dataset
from .. import _repository
from .._repository import commit as commit_module
from .. import deployment
from .. import utils


# location in DeploymentService model container
_CUSTOM_MODULES_DIR = os.environ.get('VERTA_CUSTOM_MODULES_DIR', "/app/custom_modules/")

_MODEL_ARTIFACTS_ATTR_KEY = "verta_model_artifacts"

_CACHE_DIR = os.path.join(
    os.path.expanduser("~"),
    ".verta",
    "cache",
)


class ExperimentRun(_ModelDBEntity):
    """
    Object representing a machine learning Experiment Run.

    This class provides read/write functionality for Experiment Run metadata.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_experiment_run`.

    Attributes
    ----------
    id : str
        ID of this Experiment Run.
    name : str
        Name of this Experiment Run.

    """
    def __init__(self, conn, conf, msg):
        super(ExperimentRun, self).__init__(conn, conf, _ExperimentRunService, "experiment-run", msg)

    def __repr__(self):
        self._refresh_cache()
        run_msg = self._msg
        return '\n'.join((
            "name: {}".format(run_msg.name),
            "url: {}://{}/{}/projects/{}/exp-runs/{}".format(self._conn.scheme, self._conn.socket, self.workspace, run_msg.project_id, self.id),
            "date created: {}".format(_utils.timestamp_to_str(int(run_msg.date_created))),
            "date updated: {}".format(_utils.timestamp_to_str(int(run_msg.date_updated))),
            "description: {}".format(run_msg.description),
            "tags: {}".format(run_msg.tags),
            "attributes: {}".format(_utils.unravel_key_values(run_msg.attributes)),
            "id: {}".format(run_msg.id),
            "experiment id: {}".format(run_msg.experiment_id),
            "project id: {}".format(run_msg.project_id),
            "hyperparameters: {}".format(_utils.unravel_key_values(run_msg.hyperparameters)),
            "observations: {}".format(_utils.unravel_observations(run_msg.observations)),
            "metrics: {}".format(_utils.unravel_key_values(run_msg.metrics)),
            "artifact keys: {}".format(_utils.unravel_artifacts(run_msg.artifacts)),
        ))

    def _update_cache(self):
        self._hyperparameters = _utils.unravel_key_values(self._msg.hyperparameters)
        self._metrics = _utils.unravel_key_values(self._msg.metrics)

    @property
    def workspace(self):
        self._refresh_cache()
        proj_id = self._msg.project_id
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/project/getProjectById".format(self._conn.scheme, self._conn.socket),
            self._conn, params={'id': proj_id},
        )
        _utils.raise_for_http_error(response)

        project_json = _utils.body_to_json(response)['project']
        if 'workspace_id' not in project_json:
            # workspace is OSS default
            return _OSS_DEFAULT_WORKSPACE
        else:
            return self._get_workspace_name_by_id(project_json['workspace_id'])

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @classmethod
    def _generate_default_name(cls):
        return "Run {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ExperimentRunService.GetExperimentRunById
        msg = Message(id=id)
        response = conn.make_proto_request("GET",
                                           "/api/v1/modeldb/experiment-run/getExperimentRunById",
                                           params=msg)

        return conn.maybe_proto_response(response, Message.Response).experiment_run

    @classmethod
    def _get_proto_by_name(cls, conn, name, expt_id):
        Message = _ExperimentRunService.GetExperimentRunByName
        msg = Message(experiment_id=expt_id, name=name)
        response = conn.make_proto_request("GET",
                                           "/api/v1/modeldb/experiment-run/getExperimentRunByName",
                                           params=msg)

        return conn.maybe_proto_response(response, Message.Response).experiment_run

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None):
        Message = _ExperimentRunService.CreateExperimentRun
        msg = Message(project_id=ctx.proj.id, experiment_id=ctx.expt.id, name=name,
                      description=desc, tags=tags, attributes=attrs,
                      date_created=date_created, date_updated=date_created)
        response = conn.make_proto_request("POST",
                                           "/api/v1/modeldb/experiment-run/createExperimentRun",
                                           body=msg)
        expt_run = conn.must_proto_response(response, Message.Response).experiment_run
        print("created new ExperimentRun: {}".format(expt_run.name))
        return expt_run

    def _log_artifact(self, key, artifact, artifact_type, extension=None, method=None, overwrite=False):
        """
        Logs an artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact : str or file-like or object
            Artifact or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        artifact_type : int
            Variant of `_CommonCommonService.ArtifactTypeEnum`.
        extension : str, optional
            Filename extension associated with the artifact.
        method : str, optional
            Serialization method used to produce the bytestream, if `artifact` was already serialized by verta.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        if isinstance(artifact, six.string_types):
            os.path.expanduser(artifact)
            artifact = open(artifact, 'rb')

        if hasattr(artifact, 'read') and method is not None:  # already a verta-produced stream
            artifact_stream = artifact
        else:
            artifact_stream, method = _artifact_utils.ensure_bytestream(artifact)

        if extension is None:
            extension = _artifact_utils.ext_from_method(method)

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

        # TODO: incorporate into config
        VERTA_ARTIFACT_DIR = os.environ.get('VERTA_ARTIFACT_DIR', "")
        VERTA_ARTIFACT_DIR = os.path.expanduser(VERTA_ARTIFACT_DIR)
        if VERTA_ARTIFACT_DIR:
            print("set artifact directory from environment:")
            print("    " + VERTA_ARTIFACT_DIR)
            artifact_path = os.path.join(VERTA_ARTIFACT_DIR, artifact_path)
            pathlib2.Path(artifact_path).parent.mkdir(parents=True, exist_ok=True)

        # log key to ModelDB
        Message = _ExperimentRunService.LogArtifact
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=artifact_path,
                                               path_only=True if VERTA_ARTIFACT_DIR else False,
                                               artifact_type=artifact_type,
                                               filename_extension=extension)
        msg = Message(id=self.id, artifact=artifact_msg)
        data = _utils.proto_to_json(msg)
        if overwrite:
            response = _utils.make_request("DELETE",
                                           "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(self._conn.scheme, self._conn.socket),
                                           self._conn, json={'id': self.id, 'key': key})
            _utils.raise_for_http_error(response)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logArtifact".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("artifact with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

        if VERTA_ARTIFACT_DIR:
            print("logging artifact")
            with open(artifact_path, 'wb') as f:
                shutil.copyfileobj(artifact_stream, f)
            print("log complete; file written to {}".format(artifact_path))
        else:
            self._upload_artifact(key, artifact_stream)

        self._clear_cache()

    def _upload_artifact(self, key, artifact_stream, part_size=64*(10**6)):
        """
        Uploads `artifact_stream` to ModelDB artifact store.

        Parameters
        ----------
        key : str
        artifact_stream : file-like
        part_size : int, default 64 MB
            If using multipart upload, number of bytes to upload per part.

        """
        # TODO: add to Client config
        env_part_size = os.environ.get('VERTA_ARTIFACT_PART_SIZE', "")
        try:
            part_size = int(float(env_part_size))
        except ValueError:  # not an int
            pass
        else:
            print("set artifact part size {} from environment".format(part_size))

        artifact_stream.seek(0)
        if self._conf.debug:
            print("[DEBUG] uploading {} bytes ({})".format(_artifact_utils.get_stream_length(artifact_stream), key))
            artifact_stream.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(key, "PUT", part_num=1)

        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: artifact_stream.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._get_url_for_artifact(key, "PUT", part_num=part_num).url

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
                url = "{}://{}/api/v1/modeldb/experiment-run/commitArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
                msg = _CommonService.CommitArtifactPart(id=self.id, key=key)
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers['ETag']
                data = _utils.proto_to_json(msg)
                # TODO: increase retries
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/modeldb/experiment-run/commitMultipartArtifact".format(
                self._conn.scheme,
                self._conn.socket,
            )
            msg = _CommonService.CommitMultipartArtifact(id=self.id, key=key)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=artifact_stream)
            _utils.raise_for_http_error(response)

        print("upload complete ({})".format(key))

    def _log_artifact_path(self, key, artifact_path, artifact_type, overwrite=False):
        """
        Logs the filesystem path of an artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact_path : str
            Filesystem path of the artifact.
        artifact_type : int
            Variant of `_CommonCommonService.ArtifactTypeEnum`.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.
        """
        # log key-path to ModelDB
        Message = _ExperimentRunService.LogArtifact
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=artifact_path,
                                               path_only=True,
                                               artifact_type=artifact_type)
        msg = Message(id=self.id, artifact=artifact_msg)
        data = _utils.proto_to_json(msg)
        if overwrite:
            response = _utils.make_request("DELETE",
                                           "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(self._conn.scheme, self._conn.socket),
                                           self._conn, json={'id': self.id, 'key': key})
            _utils.raise_for_http_error(response)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logArtifact".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("artifact with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def _get_artifact(self, key):
        """
        Gets the artifact with name `key` from this Experiment Run.

        If the artifact was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the artifact object will be returned.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path or bytes representing the artifact.
        bool
            True if the artifact was only logged as its filesystem path.

        """
        # get key-path from ModelDB
        Message = _CommonService.GetArtifacts
        msg = Message(id=self.id, key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        artifact = {artifact.key: artifact for artifact in response_msg.artifacts}.get(key)
        if artifact is None:
            raise KeyError("no artifact found with key {}".format(key))
        if artifact.path_only:
            return artifact.path, artifact.path_only
        else:
            # download artifact from artifact store
            url = self._get_url_for_artifact(key, "GET").url

            response = _utils.make_request("GET", url, self._conn)
            _utils.raise_for_http_error(response)

            return response.content, artifact.path_only

    # TODO: Fix up get dataset to handle the Dataset class when logging dataset
    # version
    def _get_dataset(self, key):
        """
        Gets the dataset with name `key` from this Experiment Run.

        If the dataset was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the dataset object will be returned.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path or bytes representing the artifact.
        bool
            True if the artifact was only logged as its filesystem path.

        """
        # get key-path from ModelDB
        Message = _ExperimentRunService.GetDatasets
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getDatasets".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        dataset = {dataset.key: dataset for dataset in response_msg.datasets}.get(key)
        if dataset is None:
            # may be old artifact-based dataset
            try:
                dataset, path_only = self._get_artifact(key)
            except KeyError:
                six.raise_from(KeyError("no dataset found with key {}".format(key)),
                               None)
            else:
                return dataset, path_only, None
        else:
            return dataset.path, dataset.path_only, dataset.linked_artifact_id

    def clone(self, copy_artifacts=False, copy_code_version=False, copy_datasets=False):
        """
        Returns a newly-created copy of this Experiment Run.

        Parameters
        ----------
        copy_artifacts : bool, default False
            Whether to also copy this Experiment Run's artifacts.
        copy_code_version : bool, default False
            Whether to also copy this Experiment Run's code version.
        copy_datasets : bool, default False
            Whether to also copy this Experiment Run's dataset versions.

        Returns
        -------
        :class:`ExperimentRun`

        """
        # get info for the current run
        current_run = self._get_proto_by_id(self._conn, self.id)

        # clone the current run
        Message = _ExperimentRunService.CreateExperimentRun
        msg = Message(
            project_id=current_run.project_id,
            experiment_id=current_run.experiment_id,
            name=ExperimentRun._generate_default_name(),
            description=current_run.description,
            tags=current_run.tags,
            attributes=current_run.attributes,
            observations=current_run.observations,
            metrics=current_run.metrics,
            hyperparameters=current_run.hyperparameters,
            parent_id=current_run.parent_id,
        )

        msg_artifact = Message()
        msg_code_version = Message()
        msg_datasets = Message()

        if copy_artifacts:
            msg_artifact = Message(
                artifacts=current_run.artifacts,
            )

        if copy_code_version:
            msg_code_version = Message(
                code_version_snapshot=current_run.code_version_snapshot,
            )

        if copy_datasets:
            msg_datasets = Message(
                datasets=current_run.datasets,
            )

        msg.MergeFrom(msg_artifact)
        msg.MergeFrom(msg_code_version)
        msg.MergeFrom(msg_datasets)

        # create the new run
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/createExperimentRun".format(
                                           self._conn.scheme, self._conn.socket), self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        new_run_msg = response_msg.experiment_run
        print("created new ExperimentRun: {}".format(new_run_msg.name))
        new_run = ExperimentRun(self._conn, self._conf, new_run_msg)

        return new_run

    def get_date_created(self):
        """
        Gets a timestamp representing the time (in UTC) this Experiment Run was created.

        Returns
        -------
        timestamp : int
            Unix timestamp in milliseconds.

        """
        self._refresh_cache()
        return int(self._msg.date_created)

    def get_date_updated(self):
        """
        Gets a timestamp representing the time (in UTC) this Experiment Run was updated.

        Returns
        -------
        timestamp : int
            Unix timestamp in milliseconds.

        """
        self._refresh_cache()
        return int(self._msg.date_updated)

    def log_tag(self, tag):
        """
        Logs a tag to this Experiment Run.

        Parameters
        ----------
        tag : str
            Tag.

        """
        if not isinstance(tag, six.string_types):
            raise TypeError("`tag` must be a string")

        Message = _ExperimentRunService.AddExperimentRunTags
        msg = Message(id=self.id, tags=[tag])
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_tags(self, tags):
        """
        Logs multiple tags to this Experiment Run.

        Parameters
        ----------
        tags : list of str
            Tags.

        """
        tags = _utils.as_list_of_str(tags)

        Message = _ExperimentRunService.AddExperimentRunTags
        msg = Message(id=self.id, tags=tags)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_tags(self):
        """
        Gets all tags from this Experiment Run.

        Returns
        -------
        list of str
            All tags.

        """
        Message = _CommonService.GetTags
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.tags

    def log_attribute(self, key, value):
        """
        Logs an attribute to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the attribute.
        value : one of {None, bool, float, int, str, list, dict}
            Value of the attribute.

        """
        _utils.validate_flat_key(key)

        attribute = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
        msg = _ExperimentRunService.LogAttribute(id=self.id, attribute=attribute)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logAttribute".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("attribute with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_attributes(self, attributes):
        """
        Logs potentially multiple attributes to this Experiment Run.

        Parameters
        ----------
        attributes : dict of str to {None, bool, float, int, str, list, dict}
            Attributes.

        """
        # validate all keys first
        for key in six.viewkeys(attributes):
            _utils.validate_flat_key(key)

        # build KeyValues
        attribute_keyvals = []
        for key, value in six.viewitems(attributes):
            attribute_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True)))

        msg = _ExperimentRunService.LogAttributes(id=self.id, attributes=attribute_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some attribute with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_attribute(self, key):
        """
        Gets the attribute with name `key` from this Experiment Run.

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

        Message = _CommonService.GetAttributes
        msg = Message(id=self.id, attribute_keys=[key])
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        attributes = _utils.unravel_key_values(response_msg.attributes)
        try:
            return attributes[key]
        except KeyError:
            six.raise_from(KeyError("no attribute found with key {}".format(key)), None)

    def get_attributes(self):
        """
        Gets all attributes from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
        Message = _CommonService.GetAttributes
        msg = Message(id=self.id, get_all=True)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return _utils.unravel_key_values(response_msg.attributes)

    def _delete_metrics(self, keys):
        response = _utils.make_request("DELETE",
                                       "{}://{}/api/v1/modeldb/experiment-run/deleteMetrics".format(
                                           self._conn.scheme, self._conn.socket),
                                       self._conn, json={'id': self.id, 'metric_keys': keys})
        _utils.raise_for_http_error(response)

    def _delete_observations(self, keys):
        response = _utils.make_request("DELETE",
                                       "{}://{}/api/v1/modeldb/experiment-run/deleteObservations".format(
                                           self._conn.scheme, self._conn.socket),
                                       self._conn, json={'id': self.id, 'observation_keys': keys})
        _utils.raise_for_http_error(response)

    def _delete_hyperparameters(self, keys):
        response = _utils.make_request("DELETE",
                                       "{}://{}/api/v1/modeldb/experiment-run/deleteHyperparameters".format(
                                           self._conn.scheme, self._conn.socket),
                                       self._conn, json={'id': self.id, 'hyperparameter_keys': keys})
        _utils.raise_for_http_error(response)

    def log_metric(self, key, value):
        """
        Logs a metric to this Experiment Run.

        If the metadatum of interest might recur, :meth:`.log_observation` should be used instead.

        Parameters
        ----------
        key : str
            Name of the metric.
        value : one of {None, bool, float, int, str}
            Value of the metric.

        """
        _utils.validate_flat_key(key)

        metric = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        msg = _ExperimentRunService.LogMetric(id=self.id, metric=metric)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logMetric".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("metric with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_metrics(self, metrics):
        """
        Logs potentially multiple metrics to this Experiment Run.

        Parameters
        ----------
        metrics : dict of str to {None, bool, float, int, str}
            Metrics.

        """
        # validate all keys first
        for key in six.viewkeys(metrics):
            _utils.validate_flat_key(key)

        # build KeyValues
        metric_keyvals = []
        keys = []
        for key, value in six.viewitems(metrics):
            metric_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value)))
            keys.append(key)

        msg = _ExperimentRunService.LogMetrics(id=self.id, metrics=metric_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logMetrics".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some metric with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_metric(self, key):
        """
        Gets the metric with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the metric.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the metric.

        """
        self._refresh_cache()
        if key in self._metrics:
            return self._metrics[key]
        else:
            six.raise_from(KeyError("no metric found with key {}".format(key)), None)

    def get_metrics(self):
        """
        Gets all metrics from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all metrics.

        """
        self._refresh_cache()
        return self._metrics

    def log_hyperparameter(self, key, value):
        """
        Logs a hyperparameter to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the hyperparameter.
        value : one of {None, bool, float, int, str}
            Value of the hyperparameter.

        """
        _utils.validate_flat_key(key)

        hyperparameter = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        msg = _ExperimentRunService.LogHyperparameter(id=self.id, hyperparameter=hyperparameter)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logHyperparameter".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("hyperparameter with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_hyperparameters(self, hyperparams):
        """
        Logs potentially multiple hyperparameters to this Experiment Run.

        Parameters
        ----------
        hyperparameters : dict of str to {None, bool, float, int, str}
            Hyperparameters.

        """
        # validate all keys first
        for key in six.viewkeys(hyperparams):
            _utils.validate_flat_key(key)

        # build KeyValues
        hyperparameter_keyvals = []
        keys = []
        for key, value in six.viewitems(hyperparams):
            hyperparameter_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value)))
            keys.append(key)

        msg = _ExperimentRunService.LogHyperparameters(id=self.id, hyperparameters=hyperparameter_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logHyperparameters".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some hyperparameter with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_hyperparameter(self, key):
        """
        Gets the hyperparameter with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the hyperparameter.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the hyperparameter.

        """
        self._refresh_cache()
        if key in self._hyperparameters:
            return self._hyperparameters[key]
        else:
            six.raise_from(KeyError("no hyperparameter found with key {}".format(key)), None)

    def get_hyperparameters(self):
        """
        Gets all hyperparameters from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all hyperparameters.

        """
        self._refresh_cache()
        return self._hyperparameters

    def log_dataset(self, key, dataset, overwrite=False):
        """
        Alias for :meth:`~ExperimentRun.log_dataset_version`.

        .. deprecated:: 0.14.12
            ``log_dataset()`` can no longer be used to log artifacts.
            :meth:`~ExperimentRun.log_artifact` should be used instead.

        """
        if isinstance(dataset, _dataset.Dataset):
            raise TypeError(
                "directly logging a Dataset is not supported;"
                " please create a DatasetVersion for logging"
            )

        if not isinstance(dataset, _dataset.DatasetVersion):
            raise TypeError(
                "`dataset` must be of type DatasetVersion;"
                " to log an artifact, consider using run.log_artifact() instead"
            )

        self.log_dataset_version(key=key, dataset_version=dataset, overwrite=overwrite)

    def log_dataset_version(self, key, dataset_version, overwrite=False):
        """
        Logs a Verta DatasetVersion to this ExperimentRun with the given key.

        Parameters
        ----------
        key : str
        dataset_version : :class:`~verta._dataset.DatasetVersion`
        overwrite : bool, default False
            Whether to allow overwriting a dataset version.

        """
        if not isinstance(dataset_version, _dataset.DatasetVersion):
            raise TypeError("`dataset_version` must be of type DatasetVersion")

        # TODO: hack because path_only artifact needs a placeholder path
        dataset_path = "See attached dataset version"

        # log key-path to ModelDB
        Message = _ExperimentRunService.LogDataset
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=dataset_path,
                                               path_only=True,
                                               artifact_type=_CommonCommonService.ArtifactTypeEnum.DATA,
                                               linked_artifact_id=dataset_version.id)
        msg = Message(id=self.id, dataset=artifact_msg, overwrite=overwrite)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logDataset".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("dataset with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_dataset_path(self, key, path):
        """
        Logs the filesystem path of an dataset to this Experiment Run.

        .. deprecated:: 0.13.0
           The `log_dataset_path()` method will removed in v0.15.0; consider using
           `client.set_dataset(…, type="local")` and `run.log_dataset_version()` instead.

        This function makes no attempt to open a file at `dataset_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the dataset.
        dataset_path : str
            Filesystem path of the dataset.

        """
        _utils.validate_flat_key(key)

        warnings.warn("`log_dataset_path()` is deprecated and will removed in a later version;"
                      " consider using `client.set_dataset(..., type=\"local\")`"
                      " and `run.log_dataset_version()` instead",
                      category=FutureWarning)

        # create impromptu DatasetVersion
        dataset = _dataset.LocalDataset(self._conn, self._conf, name=key)
        dataset_version = dataset.create_version(path=path)

        self.log_dataset_version(key, dataset_version)

    def get_dataset(self, key):
        """
        Gets the dataset artifact with name `key` from this Experiment Run.

        If the dataset was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the dataset object itself will be returned. If the object is unable to be
        deserialized, the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the dataset.

        Returns
        -------
        str or object or file-like
            DatasetVersion if logged using :meth:`log_dataset_version()`.
            Filesystem path of the dataset, the dataset object, or a bytestream representing the
            dataset.

        """
        dataset, path_only, linked_id = self._get_dataset(key)
        if path_only:
            if linked_id:
                return _dataset.DatasetVersion(self._conn, self._conf, _dataset_version_id=linked_id)
            else:
                return dataset
        else:
            # TODO: may need to be updated for raw
            try:
                return pickle.loads(dataset)
            except pickle.UnpicklingError:
                return six.BytesIO(dataset)

    def get_dataset_version(self, key):
        """
        Gets the DatasetVersion with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the dataset version.

        Returns
        -------
        DatasetVersion
            DatasetVersion associated with the given key.

        """
        return self.get_dataset(key)

    def log_model_for_deployment(self, model, model_api, requirements, train_features=None, train_targets=None):
        """
        Logs a model artifact, a model API, requirements, and a dataset CSV to deploy on Verta.

        .. deprecated:: 0.13.13
           This function has been superseded by :meth:`log_model`, :meth:`log_requirements`, and
           :meth:`log_training_data`; consider using them instead.

        Parameters
        ----------
        model : str or file-like or object
            Model or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        model_api : str or file-like
            Model API, specifying model deployment and predictions.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
        requirements : str or file-like
            pip requirements file specifying packages necessary to deploy the model.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
        train_features : pd.DataFrame, optional
            pandas DataFrame representing features of the training data. If provided, `train_targets`
            must also be provided.
        train_targets : pd.DataFrame, optional
            pandas DataFrame representing targets of the training data. If provided, `train_features`
            must also be provided.

        Warnings
        --------
        Due to the way deployment currently works, `train_features` and `train_targets` will be joined
        together and then converted into a CSV. Retrieving the dataset through the Client will return
        a file-like bytestream of this CSV that can be passed directly into `pd.read_csv()
        <https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_csv.html>`_.

        """
        if sum(arg is None for arg in (train_features, train_targets)) == 1:
            raise ValueError("`train_features` and `train_targets` must be provided together")

        # open files
        if isinstance(model, six.string_types):
            model = open(model, 'rb')
        if isinstance(model_api, six.string_types):
            model_api = open(model_api, 'rb')
        if isinstance(requirements, six.string_types):
            requirements = open(requirements, 'rb')

        # prehandle model
        if self._conf.debug:
            if not hasattr(model, 'read'):
                print("[DEBUG] model is type: {}".format(model.__class__))
        _artifact_utils.reset_stream(model)  # reset cursor to beginning in case user forgot
        # obtain serialized model and info
        try:
            model_extension = _artifact_utils.get_file_ext(model)
        except (TypeError, ValueError):
            model_extension = None
        # serialize model
        _utils.THREAD_LOCALS.active_experiment_run = self
        try:
            model, method, model_type = _artifact_utils.serialize_model(model)
        finally:
            _utils.THREAD_LOCALS.active_experiment_run = None
        # check serialization method
        if method is None:
            raise ValueError("will not be able to deploy model due to unknown serialization method")
        if model_extension is None:
            model_extension = _artifact_utils.ext_from_method(method)

        # prehandle model_api
        _artifact_utils.reset_stream(model_api)  # reset cursor to beginning in case user forgot
        model_api = utils.ModelAPI.from_file(model_api)
        if 'model_packaging' not in model_api:
            # add model serialization info to model_api
            model_api['model_packaging'] = {
                'python_version': _utils.get_python_version(),
                'type': model_type,
                'deserialization': method,
            }
        if self._conf.debug:
            print("[DEBUG] model API is:")
            pprint.pprint(model_api.to_dict())

        # handle requirements
        _artifact_utils.reset_stream(requirements)  # reset cursor to beginning in case user forgot
        req_deps = six.ensure_str(requirements.read()).splitlines()  # get list repr of reqs
        _artifact_utils.reset_stream(requirements)  # reset cursor to beginning as a courtesy
        try:
            self.log_requirements(req_deps)
        except ValueError as e:
            if "artifact with key requirements.txt already exists" in e.args[0]:
                print("requirements.txt already logged; skipping")
            else:
                six.raise_from(e, None)

        # prehandle train_features and train_targets
        if train_features is not None and train_targets is not None:
            stringstream = six.StringIO()
            train_df = train_features.join(train_targets)
            train_df.to_csv(stringstream, index=False)  # write as CSV
            stringstream.seek(0)
            train_data = stringstream
        else:
            train_data = None

        self._log_artifact("model.pkl", model, _CommonCommonService.ArtifactTypeEnum.MODEL, model_extension, method)
        self._log_artifact("model_api.json", model_api, _CommonCommonService.ArtifactTypeEnum.BLOB, 'json')
        if train_data is not None:
            self._log_artifact("train_data", train_data, _CommonCommonService.ArtifactTypeEnum.DATA, 'csv')

    def log_tf_saved_model(self, export_dir):
        with tempfile.TemporaryFile() as tempf:
            with zipfile.ZipFile(tempf, 'w') as zipf:
                for root, _, files in os.walk(export_dir):
                    for filename in files:
                        filepath = os.path.join(root, filename)
                        zipf.write(filepath, os.path.relpath(filepath, export_dir))
            tempf.seek(0)
            # TODO: change _log_artifact() to not read file into memory
            self._log_artifact("tf_saved_model", tempf, _CommonCommonService.ArtifactTypeEnum.BLOB, 'zip')

    def log_model(self, model, custom_modules=None, model_api=None, artifacts=None, overwrite=False):
        """
        Logs a model artifact for Verta model deployment.

        Parameters
        ----------
        model : str or object
            Model for deployment.
                - If str, then it will be interpreted as a filesystem path to a serialized model file
                  for upload.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        custom_modules : list of str, optional
            Paths to local Python modules and other files that the deployed model depends on.
                - If directories are provided, all files within—excluding virtual environments—will
                  be included.
                - If not provided, all Python files located within `sys.path`—excluding virtual
                  environments—will be included.
        model_api : :class:`~utils.ModelAPI`, optional
            Model API specifying details about the model and its deployment.
        artifacts : list of str, optional
            Keys of logged artifacts to be used by a class model.
        overwrite : bool, default False
            Whether to allow overwriting existing artifacts.

        """
        if (artifacts is not None
                and not (isinstance(artifacts, list)
                         and all(isinstance(artifact_key, six.string_types) for artifact_key in artifacts))):
            raise TypeError("`artifacts` must be list of str, not {}".format(type(artifacts)))

        # validate that `artifacts` are actually logged
        if artifacts:
            self._refresh_cache()
            run_msg = self._msg
            existing_artifact_keys = {artifact.key for artifact in run_msg.artifacts}
            unlogged_artifact_keys = set(artifacts) - existing_artifact_keys
            if unlogged_artifact_keys:
                raise ValueError("`artifacts` contains keys that have not been logged: {}".format(sorted(unlogged_artifact_keys)))

        # serialize model
        try:
            extension = _artifact_utils.get_file_ext(model)
        except (TypeError, ValueError):
            extension = None
        _utils.THREAD_LOCALS.active_experiment_run = self
        try:
            serialized_model, method, model_type = _artifact_utils.serialize_model(model)
        finally:
            _utils.THREAD_LOCALS.active_experiment_run = None
        if method is None:
            raise ValueError("will not be able to deploy model due to unknown serialization method")
        if extension is None:
            extension = _artifact_utils.ext_from_method(method)
        if self._conf.debug:
            print("[DEBUG] model is type {}".format(model_type))

        if artifacts and model_type != "class":
            raise ValueError("`artifacts` can only be provided if `model` is a class")

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
        if self._conf.debug:
            print("[DEBUG] model API is:")
            pprint.pprint(model_api.to_dict())

        # associate artifact dependencies
        if artifacts:
            self.log_attribute(_MODEL_ARTIFACTS_ATTR_KEY, artifacts)

        self._log_modules(custom_modules, overwrite=overwrite)
        self._log_artifact("model.pkl", serialized_model, _CommonCommonService.ArtifactTypeEnum.MODEL, extension, method, overwrite=overwrite)
        self._log_artifact("model_api.json", model_api, _CommonCommonService.ArtifactTypeEnum.BLOB, 'json', overwrite=overwrite)

    def get_model(self):
        """
        Gets the model artifact for Verta model deployment from this Experiment Run.

        Returns
        -------
        object
            Model for deployment.

        """
        model, _ = self._get_artifact("model.pkl")
        return _artifact_utils.deserialize_model(model)

    def log_image(self, key, image, overwrite=False):
        """
        Logs a image artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the image.
        image : one of {str, file-like, pyplot, matplotlib Figure, PIL Image, object}
            Image or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - If matplotlib pyplot, then the image will be serialized and uploaded as an artifact.
                - If matplotlib Figure, then the image will be serialized and uploaded as an artifact.
                - If PIL Image, then the image will be serialized and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        overwrite : bool, default False
            Whether to allow overwriting an existing image with key `key`.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        # convert pyplot, Figure or Image to bytestream
        bytestream, extension = six.BytesIO(), 'png'
        try:  # handle matplotlib
            image.savefig(bytestream, format=extension)
        except AttributeError:
            try:  # handle PIL Image
                colors = image.getcolors()
            except AttributeError:
                try:
                    extension = _artifact_utils.get_file_ext(image)
                except (TypeError, ValueError):
                    extension = None
            else:
                if len(colors) == 1 and all(val == 255 for val in colors[0][1]):
                    warnings.warn("the image being logged is blank")
                image.save(bytestream, extension)

        bytestream.seek(0)
        if bytestream.read(1):
            bytestream.seek(0)
            image = bytestream

        self._log_artifact(key, image, _CommonCommonService.ArtifactTypeEnum.IMAGE, extension, overwrite=overwrite)

    def log_image_path(self, key, image_path):
        """
        Logs the filesystem path of an image to this Experiment Run.

        This function makes no attempt to open a file at `image_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the image.
        image_path : str
            Filesystem path of the image.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        self._log_artifact_path(key, image_path, _CommonCommonService.ArtifactTypeEnum.IMAGE)

    def get_image(self, key):
        """
        Gets the image artifact with name `key` from this Experiment Run.

        If the image was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the image object will be returned. If the object is unable to be deserialized,
        the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the image.

        Returns
        -------
        str or PIL Image or file-like
            Filesystem path of the image, the image object, or a bytestream representing the image.

        """
        image, path_only = self._get_artifact(key)
        if path_only:
            return image
        else:
            PIL = importer.maybe_dependency("PIL")
            if PIL is None:  # Pillow not installed
                return six.BytesIO(image)
            try:
                return PIL.Image.open(six.BytesIO(image))
            except IOError:  # can't be handled by Pillow
                return six.BytesIO(image)

    def log_artifact(self, key, artifact, overwrite=False):
        """
        Logs an artifact to this Experiment Run.

        The ``VERTA_ARTIFACT_DIR`` environment variable can be used to specify a locally-accessible
        directory to store artifacts.

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
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        try:
            extension = _artifact_utils.get_file_ext(artifact)
        except (TypeError, ValueError):
            extension = None

        # zip if `artifact` is directory path
        if isinstance(artifact, six.string_types) and os.path.isdir(artifact):
            tempf = tempfile.TemporaryFile()

            with zipfile.ZipFile(tempf, 'w') as zipf:
                for root, _, files in os.walk(artifact):
                    for filename in files:
                        filepath = os.path.join(root, filename)
                        zipf.write(filepath, os.path.relpath(filepath, artifact))
            tempf.seek(0)

            artifact = tempf
            extension = 'zip'

        self._log_artifact(key, artifact, _CommonCommonService.ArtifactTypeEnum.BLOB, extension, overwrite=overwrite)

    def log_artifact_path(self, key, artifact_path, overwrite=False):
        """
        Logs the filesystem path of an artifact to this Experiment Run.

        This function makes no attempt to open a file at `artifact_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact_path : str
            Filesystem path of the artifact.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        self._log_artifact_path(key, artifact_path, _CommonCommonService.ArtifactTypeEnum.BLOB, overwrite=overwrite)

    def get_artifact(self, key):
        """
        Gets the artifact with name `key` from this Experiment Run.

        If the artifact was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the artifact object will be returned. If the object is unable to be deserialized,
        the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path of the artifact, the artifact object, or a bytestream representing the
            artifact.

        """
        artifact, path_only = self._get_artifact(key)
        if path_only:
            if not os.path.exists(artifact):
                # path-only artifact; `artifact` is its path
                return artifact
            else:
                # clientside storage; `artifact` is its path
                # NOTE: can cause problem if accidentally picks up unrelated file w/ same name
                artifact_stream = open(artifact, 'rb')
        else:
            # uploaded artifact; `artifact` is its bytes
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
        """
        Downloads the artifact with name `key` to path `download_to_path`.

        Parameters
        ----------
        key : str
            Name of the artifact.
        download_to_path : str
            Path to download to.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where artifact was downloaded to. Matches `download_to_path`.

        """
        download_to_path = os.path.abspath(download_to_path)

        # get key-path from ModelDB
        # TODO: consolidate the following ~12 lines with ExperimentRun._get_artifact()
        Message = _CommonService.GetArtifacts
        msg = Message(id=self.id, key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        artifact = {artifact.key: artifact for artifact in response_msg.artifacts}.get(key)
        if artifact is None:
            raise KeyError("no artifact found with key {}".format(key))

        # TODO: unpack dirs logged as artifacts
        #     But we can't distinguish if a ZIP artifact is a directory we've compressed, or if it
        #     was a ZIP file the user already had.

        # create parent dirs
        pathlib2.Path(download_to_path).parent.mkdir(parents=True, exist_ok=True)
        # TODO: clean up empty parent dirs if something later fails

        # get a stream of the file bytes, without loading into memory, and write to file
        # TODO: consolidate this with _get_artifact() and get_artifact()
        print("downloading {} from ModelDB".format(key))
        if artifact.path_only:
            if os.path.exists(artifact.path):
                # copy from clientside storage
                shutil.copyfile(artifact.path, download_to_path)
            else:
                raise ValueError(
                    "artifact {} appears to have been logged as path-only,"
                    " and cannot be downloaded".format(key)
                )
            print("download complete; file written to {}".format(download_to_path))
        else:
            # download artifact from artifact store
            url = self._get_url_for_artifact(key, "GET").url
            with _utils.make_request("GET", url, self._conn, stream=True) as response:
                _utils.raise_for_http_error(response)

                # user-specified filepath, so overwrite
                _request_utils.download(response, download_to_path, overwrite_ok=True)

        return download_to_path

    def get_artifact_parts(self, key):
        endpoint = "{}://{}/api/v1/modeldb/experiment-run/getCommittedArtifactParts".format(
            self._conn.scheme,
            self._conn.socket,
        )
        data = {'id': self.id, 'key': key}
        response = _utils.make_request("GET", endpoint, self._conn, params=data)
        _utils.raise_for_http_error(response)

        committed_parts = _utils.body_to_json(response).get('artifact_parts', [])
        committed_parts = list(sorted(
            committed_parts,
            key=lambda part: int(part['part_number']),
        ))
        return committed_parts

    def log_observation(self, key, value, timestamp=None, epoch_num=None):
        """
        Logs an observation to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the observation.
        value : one of {None, bool, float, int, str}
            Value of the observation.
        timestamp : str or float or int, optional
            String representation of a datetime or numerical Unix timestamp. If not provided, the
            current time will be used.
        epoch_num : non-negative int, optional
            Epoch number associated with this observation. If not provided, it will automatically
            be incremented from prior observations for the same `key`.

        Warnings
        --------
        If `timestamp` is provided by the user, it must contain timezone information. Otherwise,
        it will be interpreted as UTC.

        """
        _utils.validate_flat_key(key)

        if timestamp is None:
            timestamp = _utils.now()
        else:
            timestamp = _utils.ensure_timestamp(timestamp)

        if epoch_num is not None:
            if (not isinstance(epoch_num, six.integer_types)
                    and not (isinstance(epoch_num, float) and epoch_num.is_integer())):
                raise TypeError("`epoch_num` must be int, not {}".format(type(epoch_num)))
            if epoch_num < 0:
                raise ValueError("`epoch_num` must be non-negative")

        attribute = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        observation = _ExperimentRunService.Observation(attribute=attribute, timestamp=timestamp)  # TODO: support Artifacts
        if epoch_num is not None:
            observation.epoch_number.number_value = epoch_num  # pylint: disable=no-member

        msg = _ExperimentRunService.LogObservation(id=self.id, observation=observation)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logObservation".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_observation(self, key):
        """
        Gets the observation series with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of observation series.

        Returns
        -------
        list of {None, bool, float, int, str}
            Values of observation series.

        """
        _utils.validate_flat_key(key)

        Message = _ExperimentRunService.GetObservations
        msg = Message(id=self.id, observation_key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getObservations".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        if len(response_msg.observations) == 0:
            raise KeyError("no observation found with key {}".format(key))
        else:
            return [_utils.unravel_observation(observation)[1:]  # drop key from tuple
                    for observation in response_msg.observations]  # TODO: support Artifacts

    def get_observations(self):
        """
        Gets all observations from this Experiment Run.

        Returns
        -------
        dict of str to list of {None, bool, float, int, str}
            Names and values of all observation series.

        """
        self._refresh_cache()
        return _utils.unravel_observations(self._msg.observations)

    def log_requirements(self, requirements, overwrite=False):
        """
        Logs a pip requirements file for Verta model deployment.

        .. versionadded:: 0.13.13

        Parameters
        ----------
        requirements : str or list of str
            PyPI-installable packages necessary to deploy the model.
                - If str, then it will be interpreted as a filesystem path to a requirements file
                  for upload.
                - If list of str, then it will be interpreted as a list of PyPI package names.
        overwrite : bool, default False
            Whether to allow overwriting existing requirements.

        Raises
        ------
        ValueError
            If a package's name is invalid for PyPI, or its exact version cannot be determined.

        Examples
        --------
        From a file:

        .. code-block:: python

            run.log_requirements("../requirements.txt")
            # upload complete (requirements.txt)
            print(run.get_artifact("requirements.txt").read().decode())
            # cloudpickle==1.2.1
            # jupyter==1.0.0
            # matplotlib==3.1.1
            # pandas==0.25.0
            # scikit-learn==0.21.3
            # verta==0.13.13

        From a list of package names:

        .. code-block:: python

            run.log_requirements(['verta', 'cloudpickle', 'scikit-learn'])
            # upload complete (requirements.txt)
            print(run.get_artifact("requirements.txt").read().decode())
            # verta==0.13.13
            # cloudpickle==1.2.1
            # scikit-learn==0.21.3

        """
        if isinstance(requirements, six.string_types):
            with open(requirements, 'r') as f:
                requirements = _pip_requirements_utils.clean_reqs_file_lines(f.readlines())
        elif (isinstance(requirements, list)
              and all(isinstance(req, six.string_types) for req in requirements)):
            requirements = copy.copy(requirements)
        else:
            raise TypeError("`requirements` must be either str or list of str, not {}".format(type(requirements)))

        _pip_requirements_utils.process_requirements(requirements)

        if self._conf.debug:
            print("[DEBUG] requirements are:")
            print(requirements)

        requirements = six.BytesIO(six.ensure_binary('\n'.join(requirements)))  # as file-like
        self._log_artifact("requirements.txt", requirements, _CommonCommonService.ArtifactTypeEnum.BLOB, 'txt', overwrite=overwrite)

    def log_modules(self, paths, search_path=None):
        """
        Logs local files that are dependencies for a deployed model to this Experiment Run.

        .. deprecated:: 0.13.13
           The behavior of this function has been merged into :meth:`log_model` as its
           ``custom_modules`` parameter; consider using that instead.
        .. deprecated:: 0.12.4
           The `search_path` parameter is no longer necessary and will removed in v0.15.0; consider
           removing it from the function call.

        Parameters
        ----------
        paths : str or list of str
            Paths to local Python modules and other files that the deployed model depends on. If a
            directory is provided, all files within will be included.

        """
        warnings.warn("The behavior of this function has been merged into log_model() as its"
                      " `custom_modules` parameter; consider using that instead",
                      category=FutureWarning)
        if search_path is not None:
            warnings.warn("`search_path` is no longer used and will removed in a later version;"
                          " consider removing it from the function call",
                          category=FutureWarning)

        self._log_modules(paths)

    def _log_modules(self, paths=None, overwrite=False):
        if isinstance(paths, six.string_types):
            paths = [paths]
        if paths is not None:
            paths = list(map(os.path.expanduser, paths))
            paths = list(map(os.path.abspath, paths))

        # collect local sys paths
        local_sys_paths = copy.copy(sys.path)
        ## replace empty first element with cwd
        ##     https://docs.python.org/3/library/sys.html#sys.path
        if local_sys_paths[0] == "":
            local_sys_paths[0] = os.getcwd()
        ## convert to absolute paths
        local_sys_paths = list(map(os.path.abspath, local_sys_paths))
        ## remove paths that don't exist
        local_sys_paths = list(filter(os.path.exists, local_sys_paths))
        ## remove .ipython
        local_sys_paths = list(filter(lambda path: not path.endswith(".ipython"), local_sys_paths))
        ## remove virtual (and real) environments
        def is_in_venv(path):
            """
            Roughly checks for:
                /
                |_ lib/
                |   |_ python*/ <- directory with Python packages, containing `path`
                |
                |_ bin/
                    |_ python*  <- Python executable

            """
            lib_python_str = os.path.join(os.sep, "lib", "python")
            i = path.find(lib_python_str)
            return i != -1 and glob.glob(os.path.join(path[:i], "bin", "python*"))
        local_sys_paths = list(filter(lambda path: not is_in_venv(path), local_sys_paths))

        # get paths to files within
        if paths is None:
            # Python files within filtered sys.path dirs
            paths = local_sys_paths
            extensions = ['py', 'pyc', 'pyo']
        else:
            # all user-specified files
            paths = paths
            extensions = None
        local_filepaths = _utils.find_filepaths(
            paths, extensions=extensions,
            include_hidden=True,
            include_venv=False,  # ignore virtual environments nested within
        )

        # obtain deepest common directory
        #     This directory on the local system will be mirrored in `_CUSTOM_MODULES_DIR` in
        #     deployment.
        curr_dir = os.path.join(os.getcwd(), "")
        paths_plus = list(local_filepaths) + [curr_dir]
        common_prefix = os.path.commonprefix(paths_plus)
        common_dir = os.path.dirname(common_prefix)

        # replace `common_dir` with `_CUSTOM_MODULES_DIR` for deployment sys.path
        depl_sys_paths = list(map(lambda path: os.path.relpath(path, common_dir), local_sys_paths))
        depl_sys_paths = list(map(lambda path: os.path.join(_CUSTOM_MODULES_DIR, path), depl_sys_paths))

        bytestream = six.BytesIO()
        with zipfile.ZipFile(bytestream, 'w') as zipf:
            for filepath in local_filepaths:
                arcname = os.path.relpath(filepath, common_dir)  # filepath relative to archive root
                try:
                    zipf.write(filepath, arcname)
                except:
                    # maybe file has corrupt metadata; try reading then writing contents
                    with open(filepath, 'rb') as f:
                        zipf.writestr(arcname, f.read())

            # add verta config file for sys.path and chdir
            working_dir = os.path.join(_CUSTOM_MODULES_DIR, os.path.relpath(curr_dir, common_dir))
            zipf.writestr(
                "_verta_config.py",
                six.ensure_binary('\n'.join([
                    "import os, sys",
                    "",
                    "",
                    "sys.path = sys.path[:1] + {} + sys.path[1:]".format(depl_sys_paths),
                    "",
                    "try:",
                    "    os.makedirs(\"{}\")".format(working_dir),
                    "except OSError:  # already exists",
                    "    pass",
                    "os.chdir(\"{}\")".format(working_dir),
                ]))
            )

            # add __init__.py
            init_filename = "__init__.py"
            if init_filename not in zipf.namelist():
                zipf.writestr(init_filename, b"")

            if self._conf.debug:
                print("[DEBUG] archive contains:")
                zipf.printdir()
        bytestream.seek(0)

        self._log_artifact("custom_modules", bytestream, _CommonCommonService.ArtifactTypeEnum.BLOB, 'zip', overwrite=overwrite)

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
            line = script.splitlines()[line_no-1]
            six.raise_from(SyntaxError("{} in provided script on line {}:\n{}"
                                       .format(reason, line_no, line)),
                           e)

        # convert into bytes for upload
        script = six.ensure_binary(script)

        # convert to file-like for `_log_artifact()`
        script = six.BytesIO(script)

        self._log_artifact("setup_script", script, _CommonCommonService.ArtifactTypeEnum.BLOB, 'py', overwrite=overwrite)

    def log_training_data(self, train_features, train_targets, overwrite=False):
        """
        Associate training data with this Experiment Run.

        .. versionchanged:: 0.14.4
           Instead of uploading the data itself as a CSV artifact ``'train_data'``, this method now
           generates a histogram for internal use by our deployment data monitoring system.

        Parameters
        ----------
        train_features : pd.DataFrame
            pandas DataFrame representing features of the training data.
        train_targets : pd.DataFrame or pd.Series
            pandas DataFrame representing targets of the training data.
        overwrite : bool, default False
            Whether to allow overwriting existing training data.

        """
        if train_features.__class__.__name__ != "DataFrame":
            raise TypeError("`train_features` must be a pandas DataFrame, not {}".format(type(train_features)))
        if train_targets.__class__.__name__ == "Series":
            train_targets = train_targets.to_frame()
        elif train_targets.__class__.__name__ != "DataFrame":
            raise TypeError("`train_targets` must be a pandas DataFrame or Series, not {}".format(type(train_targets)))

        # check for overlapping column names
        common_column_names = set(train_features.columns) & set(train_targets.columns)
        if common_column_names:
            raise ValueError("`train_features` and `train_targets` combined have overlapping column names;"
                             " please ensure column names are unique")

        train_df = train_features.join(train_targets)

        histograms = _histogram_utils.calculate_histograms(train_df)

        endpoint = "{}://{}/api/v1/monitoring/data/references/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
        )
        response = _utils.make_request("PUT", endpoint, self._conn, json=histograms)
        _utils.raise_for_http_error(response)

    def fetch_artifacts(self, keys):
        """
        Downloads artifacts that are associated with a class model.

        Parameters
        ----------
        keys : list of str
            Keys of artifacts to download.

        Returns
        -------
        dict of str to str
            Map of artifacts' keys to their cache filepaths—for use as the ``artifacts`` parameter
            to a Verta class model.

        Examples
        --------
        .. code-block:: python

            run.log_artifact("weights", open("weights.npz", 'rb'))
            # upload complete (weights)
            run.log_artifact("text_embeddings", open("embedding.csv", 'rb'))
            # upload complete (text_embeddings)
            artifact_keys = ["weights", "text_embeddings"]
            artifacts = run.fetch_artifacts(artifact_keys)
            artifacts
            # {'weights': '/Users/convoliution/.verta/cache/artifacts/50a9726b3666d99aea8af006cf224a7637d0c0b5febb3b0051192ce1e8615f47/weights.npz',
            #  'text_embeddings': '/Users/convoliution/.verta/cache/artifacts/2d2d1d809e9bce229f0a766126ae75df14cadd1e8f182561ceae5ad5457a3c38/embedding.csv'}
            ModelClass(artifacts=artifacts).predict(["Good book.", "Bad book!"])
            # [0.955998517288053, 0.09809996313422353]
            run.log_model(ModelClass, artifacts=artifact_keys)
            # upload complete (custom_modules.zip)
            # upload complete (model.pkl)
            # upload complete (model_api.json)

        """
        if not (isinstance(keys, list)
                and all(isinstance(key, six.string_types) for key in keys)):
            raise TypeError("`keys` must be list of str, not {}".format(type(keys)))

        # validate that `keys` are actually logged
        self._refresh_cache()
        run_msg = self._msg
        existing_artifact_keys = {artifact.key for artifact in run_msg.artifacts}
        unlogged_artifact_keys = set(keys) - existing_artifact_keys
        if unlogged_artifact_keys:
            raise ValueError("`keys` contains keys that have not been logged: {}".format(sorted(unlogged_artifact_keys)))

        # get artifact checksums
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params={'id': self.id})
        _utils.raise_for_http_error(response)
        paths = {artifact['key']: artifact['path']
                 for artifact in _utils.body_to_json(response)['artifacts']}

        artifacts = dict()
        for key in keys:
            filename = os.path.join("artifacts", paths[key])

            # check cache, otherwise write to cache
            #     "try-get-then-create" can lead multiple threads trying to write to the cache
            #     simultaneously, but artifacts being cached at a particular location should be
            #     identical, so multiple writes would be idempotent.
            path = self._get_cached(filename)
            if path is None:
                contents, _ = self._get_artifact(key)  # TODO: raise error if path_only
                path = self._cache(filename, contents)

            artifacts.update({key: path})

        return artifacts

    def get_deployment_status(self):
        """
        Returns the current status of the model deployment associated with this Experiment Run.

        .. versionadded:: 0.13.17

        Returns
        -------
        status : dict
            - ``'status'`` (`str`) – Current status of the model deployment.
            - (if deployed) ``'url'`` (`str`) – Prediction endpoint URL.
            - (if deployed) ``'token'`` (`str or None`) – Token for authorizing prediction requests.
            - (if error during deployment) ``'message'`` (`str`) – Error message from the model.

        """
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
            self._conn,
        )
        _utils.raise_for_http_error(response)

        status = _utils.body_to_json(response)
        if 'api' in status:
            status.update({'url': "{}://{}{}".format(self._conn.scheme, self._conn.socket, status.pop('api'))})
            status.update({'token': status.pop('token', None)})
        return status

    def deploy(self, path=None, token=None, no_token=False, wait=False):
        """
        Deploys the model logged to this Experiment Run.

        .. versionadded:: 0.13.17

        Parameters
        ----------
        path : str, optional
            Suffix for the prediction endpoint URL. If not provided, one will be generated
            automatically.
        token : str, optional
            Token to use to authorize predictions requests. If not provided and `no_token` is
            ``False``, one will be generated automatically.
        no_token : bool, default False
            Whether to not require a token for predictions.
        wait : bool, default False
            Whether to wait for the deployed model to be ready for this function to finish.

        Returns
        -------
        status : dict
            See :meth:`~ExperimentRun.get_deployment_status`.

        Raises
        ------
        RuntimeError
            If the model is already deployed or is being deployed, or if a required deployment
            artifact is missing.

        Examples
        --------
        .. code-block:: python

            status = run.deploy(path="banana", no_token=True, wait=True)
            # waiting for deployment.........
            status
            # {'status': 'deployed',
            #  'url': 'https://app.verta.ai/api/v1/predict/abcdefgh-1234-abcd-1234-abcdefghijkl/banana',
            #  'token': None}
            DeployedModel.from_url(status['url']).predict([x])
            # [0.973340685896]

        """
        data = {}
        if path is not None:
            # get project ID for URL path
            self._refresh_cache()
            run_msg = self._msg
            data.update({'url_path': "{}/{}".format(run_msg.project_id, path)})
        if no_token:
            data.update({'token': ""})
        elif token is not None:
            data.update({'token': token})

        response = _utils.make_request(
            "PUT",
            "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
            self._conn, json=data,
        )
        try:
            _utils.raise_for_http_error(response)
        except requests.HTTPError as e:
            if response.status_code == 400:
                # propagate error caused by missing artifact
                # TODO: recommend user call log_model() / log_requirements()
                error_text = e.response.text.strip()
                six.raise_from(RuntimeError("unable to deploy due to {}".format(error_text)), None)
            else:
                raise e

        if wait:
            print("waiting for deployment...", end='')
            sys.stdout.flush()
            while self.get_deployment_status()['status'] not in ("deployed", "error"):
                print(".", end='')
                sys.stdout.flush()
                time.sleep(5)
            print()
            if self.get_deployment_status()['status'] == "error":
                status = self.get_deployment_status()
                raise RuntimeError("model deployment is failing;\n{}".format(status.get('message', "no error message available")))

        return self.get_deployment_status()

    def undeploy(self, wait=False):
        """
        Undeploys the model logged to this Experiment Run.

        .. versionadded:: 0.13.17

        Parameters
        ----------
        wait : bool, default False
            Whether to wait for the undeployment to complete for this function to finish.

        Returns
        -------
        status : dict
            See :meth:`~ExperimentRun.get_deployment_status`.

        Raises
        ------
        RuntimeError
            If the model is already not deployed.

        """
        # skip calling undeploy on already-undeployed model
        #     This needs to be checked first, otherwise the undeployment endpoint will return an
        #     unhelpful HTTP 404 Not Found.
        if self.get_deployment_status()['status'] != "not deployed":
            response = _utils.make_request(
                "DELETE",
                "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
                self._conn,
            )
            _utils.raise_for_http_error(response)

            if wait:
                print("waiting for undeployment...", end='')
                sys.stdout.flush()
                while self.get_deployment_status()['status'] != "not deployed":
                    print(".", end='')
                    sys.stdout.flush()
                    time.sleep(5)
                print()

        return self.get_deployment_status()

    def get_deployed_model(self):
        """
        Returns an object for making predictions against the deployed model.

        .. versionadded:: 0.13.17

        Returns
        -------
        :class:`~verta.deployment.DeployedModel`

        Raises
        ------
        RuntimeError
            If the model is not currently deployed.

        """
        status = self.get_deployment_status().get('status', "<no status>")
        if status != "deployed":
            raise RuntimeError("model is not currently deployed (status: {})".format(status))

        status = self.get_deployment_status()
        return deployment.DeployedModel.from_url(status['url'], status['token'])

    def download_deployment_yaml(self, download_to_path, path=None, token=None, no_token=False):
        """
        Downloads this Experiment Run's model deployment CRD YAML.

        Parameters
        ----------
        download_to_path : str
            Path to download deployment YAML to.
        path : str, optional
            Suffix for the prediction endpoint URL. If not provided, one will be generated
            automatically.
        token : str, optional
            Token to use to authorize predictions requests. If not provided and `no_token` is
            ``False``, one will be generated automatically.
        no_token : bool, default False
            Whether to not require a token for predictions.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where deployment YAML was downloaded to. Matches `download_to_path`.

        """
        # NOTE: this param-handling block was copied verbatim from deploy()
        data = {}
        if path is not None:
            # get project ID for URL path
            self._refresh_cache()
            run_msg = self._msg
            data.update({'url_path': "{}/{}".format(run_msg.project_id, path)})
        if no_token:
            data.update({'token': ""})
        elif token is not None:
            data.update({'token': token})

        endpoint = "{}://{}/api/v1/deployment/models/{}/crd".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
        )
        with _utils.make_request("POST", endpoint, self._conn, json=data, stream=True) as response:
            try:
                _utils.raise_for_http_error(response)
            except requests.HTTPError as e:
                # propagate error caused by missing artifact
                error_text = e.response.text.strip()
                if error_text.startswith("missing artifact"):
                    new_e = RuntimeError("unable to obtain deployment CRD due to " + error_text)
                    six.raise_from(new_e, None)
                else:
                    raise e

            downloaded_to_path = _request_utils.download(response, download_to_path, overwrite_ok=True)
            return os.path.abspath(downloaded_to_path)

    def download_docker_context(self, download_to_path):
        """
        Downloads this Experiment Run's Docker context ``tgz``.

        Parameters
        ----------
        download_to_path : str
            Path to download Docker context to.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where Docker context was downloaded to. Matches `download_to_path`.

        """
        endpoint = "{}://{}/api/v1/deployment/models/{}/dockercontext".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
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

    def log_commit(self, commit, key_paths=None):
        """
        Associate a Commit with this Experiment Run.

        .. versionadded:: 0.14.1

        Parameters
        ----------
        commit : :class:`verta._repository.commit.Commit`
            Verta Commit.
        key_paths : dict of `key` to `path`, optional
            A mapping between descriptive keys and paths of particular interest within `commit`.
            This can be useful for, say, highlighting a particular file as *the* training dataset
            used for this Experiment Run.

        """
        if commit.id is None:
            raise RuntimeError("Commit must be saved before it can be logged to an Experiment Run")

        msg = _ExperimentRunService.LogVersionedInput()
        msg.id = self.id
        msg.versioned_inputs.repository_id = commit._repo.id
        msg.versioned_inputs.commit = commit.id
        for key, path in six.viewitems(key_paths or {}):
            location = commit_module.path_to_location(path)
            location_msg = msg.versioned_inputs.key_location_map.get_or_create(key)
            location_msg.location.extend(location)

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/experiment-run/logVersionedInput".format(
            self._conn.scheme,
            self._conn.socket,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)

        self._clear_cache()

    def get_commit(self):
        """
        Gets the Commit associated with this Experiment Run.

        .. versionadded:: 0.14.1

        Returns
        -------
        commit : :class:`verta._repository.commit.Commit`
            Verta Commit.
        key_paths : dict of `key` to `path`
            A mapping between descriptive keys and paths of particular interest within `commit`.

        """
        msg = _ExperimentRunService.GetVersionedInput()
        msg.id = self.id

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/experiment-run/getVersionedInput".format(
            self._conn.scheme,
            self._conn.socket,
        )
        response = _utils.make_request("GET", endpoint, self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)
        repo = _repository.Repository(self._conn, response_msg.versioned_inputs.repository_id)
        commit_id = response_msg.versioned_inputs.commit
        commit = commit_module.Commit._from_id(self._conn, repo, commit_id)

        key_paths = {
            key: '/'.join(location_msg.location)
            for key, location_msg
            in response_msg.versioned_inputs.key_location_map.items()
        }

        return commit, key_paths

    def _get_url_for_artifact(self, key, method, artifact_type=0, part_num=0):
        """
        Obtains a URL to use for accessing stored artifacts.

        Parameters
        ----------
        key : str
            Name of the artifact.
        method : {'GET', 'PUT'}
            HTTP method to request for the generated URL.
        artifact_type : int, optional
            Variant of `_CommonCommonService.ArtifactTypeEnum`. This informs the backend what slot to check
            for the artifact, if necessary.
        part_num : int, optional
            If using Multipart Upload, number of part to be uploaded.

        Returns
        -------
        response_msg : `_CommonService.GetUrlForArtifact.Response`
            Backend response.

        """
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _CommonService.GetUrlForArtifact
        msg = Message(
            id=self.id, key=key,
            method=method.upper(),
            artifact_type=artifact_type,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       self._request_url.format("getUrlForArtifact"),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)

        url = response_msg.url
        # accommodate port-forwarded NFS store
        if 'https://localhost' in url[:20]:
            url = 'http' + url[5:]
        if 'localhost%3a' in url[:20]:
            url = url.replace('localhost%3a', 'localhost:')
        if 'localhost%3A' in url[:20]:
            url = url.replace('localhost%3A', 'localhost:')
        response_msg.url = url

        return response_msg

    def _cache(self, filename, contents):
        """
        Caches `contents` to `filename` within ``_CACHE_DIR``.

        If `contents` represents a ZIP file, then it will be unzipped, and the path to the target
        directory will be returned.

        Parameters
        ----------
        filename : str
            Filename within ``_CACHE_DIR`` to write to.
        contents : bytes
            Contents to be cached.

        Returns
        -------
        str
            Full path to cached contents.

        """
        # write contents to temporary file
        with tempfile.NamedTemporaryFile(delete=False) as tempf:
            tempf.write(contents)
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer

        name, extension = os.path.splitext(filename)
        if extension == '.zip':
            temp_path = tempfile.mkdtemp()

            with zipfile.ZipFile(tempf.name, 'r') as zipf:
                zipf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.tgz':
            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r:gz') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.tar':
            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.gz' and os.path.splitext(name)[1] == '.tar':
            name = os.path.splitext(name)[0]

            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r:gz') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        else:
            name = filename
            temp_path = tempf.name

        path = os.path.join(_CACHE_DIR, name)

        # create intermediate dirs
        try:
            os.makedirs(os.path.dirname(path))
        except OSError:  # already exists
            pass

        # move written contents to cache location
        shutil.move(temp_path, path)

        return path

    def _get_cached(self, filename):
        name, extension = os.path.splitext(filename)
        if extension == '.zip':
            pass
        elif extension == '.tgz':
            pass
        elif extension == '.tar':
            pass
        elif extension == '.gz' and os.path.splitext(name)[1] == '.tar':
            name = os.path.splitext(name)[0]
        else:
            name = filename

        path = os.path.join(_CACHE_DIR, name)
        return path if os.path.exists(path) else None
