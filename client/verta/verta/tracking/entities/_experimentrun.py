# -*- coding: utf-8 -*-

from __future__ import print_function

import ast
import copy
import os
import pathlib
import pickle
import pprint
import shutil
import sys
import time
import warnings

import requests

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import (
    CommonService_pb2 as _CommonService,
    ProjectService_pb2,
)
from verta._protos.public.modeldb import (
    ExperimentRunService_pb2 as _ExperimentRunService,
)

from verta._vendored import six

from verta._internal_utils import (
    _artifact_utils,
    _pip_requirements_utils,
    _request_utils,
    _utils,
    importer,
)

from verta.dataset.entities import (
    _dataset,
    _dataset_version,
)
from verta import data_types
from verta import deployment
from verta import utils
from verta.environment import _Environment

from ._entity import _MODEL_ARTIFACTS_ATTR_KEY
from ._deployable_entity import _DeployableEntity


class ExperimentRun(_DeployableEntity):
    """
    Object representing a machine learning Experiment Run.

    This class provides read/write functionality for Experiment Run metadata.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_experiment_run() <verta.Client.set_experiment_run>`.

    Attributes
    ----------
    id : str
        ID of this Experiment Run.
    name : str
        Name of this Experiment Run.
    has_environment : bool
        Whether there is an environment associated with this Experiment Run.
    url : str
        Verta web app URL.

    """

    def __init__(self, conn, conf, msg):
        super(ExperimentRun, self).__init__(
            conn, conf, _ExperimentRunService, "experiment-run", msg
        )

    def __repr__(self):
        self._refresh_cache()
        run_msg = self._msg
        return "\n".join(
            (
                "name: {}".format(run_msg.name),
                "url: {}".format(self.url),
                "date created: {}".format(
                    _utils.timestamp_to_str(int(run_msg.date_created))
                ),
                "date updated: {}".format(
                    _utils.timestamp_to_str(int(run_msg.date_updated))
                ),
                "start time: {}".format(
                    _utils.timestamp_to_str(int(run_msg.start_time))
                ),
                "end time: {}".format(_utils.timestamp_to_str(int(run_msg.end_time))),
                "description: {}".format(run_msg.description),
                "tags: {}".format(run_msg.tags),
                "attributes: {}".format(_utils.unravel_key_values(run_msg.attributes)),
                "id: {}".format(run_msg.id),
                "experiment id: {}".format(run_msg.experiment_id),
                "project id: {}".format(run_msg.project_id),
                "hyperparameters: {}".format(
                    _utils.unravel_key_values(run_msg.hyperparameters)
                ),
                "observations: {}".format(
                    _utils.unravel_observations(run_msg.observations)
                ),
                "metrics: {}".format(_utils.unravel_key_values(run_msg.metrics)),
                "artifact keys: {}".format(_utils.unravel_artifacts(run_msg.artifacts)),
            )
        )

    def _update_cache(self):
        self._hyperparameters = _utils.unravel_key_values(self._msg.hyperparameters)
        self._metrics = _utils.unravel_key_values(self._msg.metrics)

    @property
    def _MODEL_KEY(self):
        return _artifact_utils.MODEL_KEY

    @property
    def workspace(self):
        self._refresh_cache()

        msg = ProjectService_pb2.GetProjectById(id=self._msg.project_id)
        url = "/api/v1/modeldb/project/getProjectById"
        response = self._conn.make_proto_request("GET", url, params=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        proj_proto = response.project

        if proj_proto.workspace_service_id:
            return self._conn.get_workspace_name_from_id(
                proj_proto.workspace_service_id
            )
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def url(self):
        return "{}://{}/{}/projects/{}/exp-runs/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self._msg.project_id,
            self.id,
        )

    @classmethod
    def _generate_default_name(cls):
        return "Run {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ExperimentRunService.GetExperimentRunById
        msg = Message(id=id)
        response = conn.make_proto_request(
            "GET", "/api/v1/modeldb/experiment-run/getExperimentRunById", params=msg
        )

        return conn.maybe_proto_response(response, Message.Response).experiment_run

    @classmethod
    def _get_proto_by_name(cls, conn, name, expt_id):
        Message = _ExperimentRunService.GetExperimentRunByName
        msg = Message(experiment_id=expt_id, name=name)
        response = conn.make_proto_request(
            "GET", "/api/v1/modeldb/experiment-run/getExperimentRunByName", params=msg
        )

        return conn.maybe_proto_response(response, Message.Response).experiment_run

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
        start_time=None,
        end_time=None,
    ):
        Message = _ExperimentRunService.CreateExperimentRun
        msg = Message(
            project_id=ctx.proj.id,
            experiment_id=ctx.expt.id,
            name=name,
            description=desc,
            tags=tags,
            attributes=attrs,
            date_created=date_created,
            date_updated=date_created,
            start_time=start_time,
            end_time=end_time,
        )
        response = conn.make_proto_request(
            "POST", "/api/v1/modeldb/experiment-run/createExperimentRun", body=msg
        )
        expt_run = conn.must_proto_response(response, Message.Response).experiment_run
        print("created new ExperimentRun: {}".format(expt_run.name))
        return expt_run

    def _log_artifact(
        self,
        key,
        artifact,
        artifact_type,
        extension=None,
        method=None,
        framework=None,
        overwrite=False,
    ):
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
            Serialization method used to produce the bytestream, if `artifact`
            was already serialized by Verta.
        framework : str, optional
            Framework with which the artifact was created. This is
            `model_type` returned by `_artifact_utils.serialize_model()`
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        if isinstance(artifact, six.string_types):
            os.path.expanduser(artifact)
            artifact = open(artifact, "rb")

        if (
            hasattr(artifact, "read") and method is not None
        ):  # already a verta-produced stream
            artifact_stream = artifact
        else:
            artifact_stream, method = _artifact_utils.ensure_bytestream(artifact)

        artifact_msg = self._create_artifact_msg(
            key,
            artifact_stream,
            artifact_type=artifact_type,
            method=method,
            framework=framework,
            extension=extension,
        )

        # log key to ModelDB
        msg = _ExperimentRunService.LogArtifact(id=self.id, artifact=artifact_msg)
        data = _utils.proto_to_json(msg)
        if overwrite:
            response = _utils.make_request(
                "DELETE",
                "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(
                    self._conn.scheme, self._conn.socket
                ),
                self._conn,
                json={"id": self.id, "key": key},
            )
            _utils.raise_for_http_error(response)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logArtifact".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "artifact with key {} already exists;"
                    " consider setting overwrite=True".format(key)
                )
            else:
                _utils.raise_for_http_error(response)

        self._upload_artifact(key, artifact_stream)

        self._clear_cache()

    def _upload_artifact(self, key, artifact_stream, part_size=_artifact_utils._64MB):
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
        env_part_size = os.environ.get("VERTA_ARTIFACT_PART_SIZE", "")
        try:
            part_size = int(float(env_part_size))
        except ValueError:  # not an int
            pass
        else:
            print("set artifact part size {} from environment".format(part_size))

        artifact_stream.seek(0)
        if self._conf.debug:
            print(
                "[DEBUG] uploading {} bytes ({})".format(
                    _artifact_utils.get_stream_length(artifact_stream), key
                )
            )
            artifact_stream.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(key, "PUT", part_num=1)

        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: artifact_stream.read(part_size), b"")
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end="\r")

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
                response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                _utils.raise_for_http_error(response)

                # commit part
                url = "{}://{}/api/v1/modeldb/experiment-run/commitArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
                msg = _CommonService.CommitArtifactPart(id=self.id, key=key)
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers["ETag"]
                data = _utils.proto_to_json(msg)
                # TODO: increase retries
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = (
                "{}://{}/api/v1/modeldb/experiment-run/commitMultipartArtifact".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
            )
            msg = _CommonService.CommitMultipartArtifact(id=self.id, key=key)
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
                    + [("file", artifact_stream)],
                )
            else:
                response = _utils.make_request(
                    "PUT", url_for_artifact.url, self._conn, data=artifact_stream
                )
            _utils.raise_for_http_error(response)

        print("upload complete ({})".format(key))

    def _get_artifact_msg(self, key):
        # get key-path from ModelDB
        msg = _CommonService.GetArtifacts(id=self.id, key=key)
        endpoint = "/api/v1/modeldb/experiment-run/getArtifacts"
        response = self._conn.make_proto_request("GET", endpoint, params=msg)
        response = self._conn.must_proto_response(response, msg.Response)

        artifact_msg = {artifact.key: artifact for artifact in response.artifacts}.get(
            key
        )

        if artifact_msg is None:
            raise KeyError("no artifact found with key {}".format(key))
        return artifact_msg

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
        artifact = self._get_artifact_msg(key)

        # TODO: remove handling of path_only since log_artifact_path() was removed
        # which should also let us consolidate _get_artifact() in _DeployableEntity
        if artifact.path_only:
            return artifact.path, artifact.path_only
        else:
            # download artifact from artifact store
            url = self._get_url_for_artifact(key, "GET").url

            response = _utils.make_request("GET", url, self._conn)
            _utils.raise_for_http_error(response)

            return response.content, artifact.path_only

    def _get_artifact_parts(self, key):
        endpoint = (
            "{}://{}/api/v1/modeldb/experiment-run/getCommittedArtifactParts".format(
                self._conn.scheme,
                self._conn.socket,
            )
        )
        data = {"id": self.id, "key": key}
        response = _utils.make_request("GET", endpoint, self._conn, params=data)
        _utils.raise_for_http_error(response)

        committed_parts = _utils.body_to_json(response).get("artifact_parts", [])
        committed_parts = list(
            sorted(
                committed_parts,
                key=lambda part: int(part["part_number"]),
            )
        )
        return committed_parts

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
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/experiment-run/getDatasets".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            params=data,
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )
        dataset = {dataset.key: dataset for dataset in response_msg.datasets}.get(key)
        if dataset is None:
            # may be old artifact-based dataset
            try:
                dataset, path_only = self._get_artifact(key)
            except KeyError:
                six.raise_from(
                    KeyError("no dataset found with key {}".format(key)), None
                )
            else:
                return dataset, path_only, None
        else:
            return dataset.path, dataset.path_only, dataset.linked_artifact_id

    def clone(self, experiment_id=None):
        """
        Returns a newly-created copy of this experiment run.

        Parameters
        ----------
        experiment_id : str, optional
            ID of experiment to clone this run into. If not provided, the new
            run will be cloned into this run's experiment.

        Returns
        -------
        :class:`~verta.tracking.entities.ExperimentRun`

        """
        # get info for the current run
        Message = _ExperimentRunService.CloneExperimentRun
        msg = Message(src_experiment_run_id=self.id, dest_experiment_id=experiment_id)
        response = self._conn.make_proto_request(
            "POST", "/api/v1/modeldb/experiment-run/cloneExperimentRun", body=msg
        )

        new_run_msg = self._conn.maybe_proto_response(response, Message.Response).run
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
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
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
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
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
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunTags".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            params=data,
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )
        return response_msg.tags

    def log_attribute(self, key, value, overwrite=False):
        """
        Logs an attribute to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the attribute.
        value : one of {None, bool, float, int, str, list, dict}
            Value of the attribute.
        overwrite : bool, default False
            Whether to allow overwriting an existing atribute with key `key`.

        """
        _utils.validate_flat_key(key)
        if isinstance(value, data_types._VertaDataType):
            value = value._as_dict()

        if overwrite:
            self._delete_attributes([key])

        attribute = _CommonCommonService.KeyValue(
            key=key, value=_utils.python_to_val_proto(value, allow_collection=True)
        )
        msg = _ExperimentRunService.LogAttribute(id=self.id, attribute=attribute)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logAttribute".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "attribute with key {} already exists;"
                    " consider using observations instead, or setting overwrite=True.".format(
                        key
                    )
                )
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_attributes(self, attributes, overwrite=False):
        """
        Logs potentially multiple attributes to this Experiment Run.

        Parameters
        ----------
        attributes : dict of str to {None, bool, float, int, str, list, dict}
            Attributes.
        overwrite : bool, default False
            Whether to allow overwriting an existing atributes.

        """
        # validate all keys first
        for key in six.viewkeys(attributes):
            _utils.validate_flat_key(key)
        for key, value in six.viewitems(attributes):
            if isinstance(value, data_types._VertaDataType):
                attributes[key] = value._as_dict()

        if overwrite:
            keys = list(six.viewkeys(attributes))
            self._delete_attributes(keys)

        # build KeyValues
        attribute_keyvals = []
        for key, value in six.viewitems(attributes):
            attribute_keyvals.append(
                _CommonCommonService.KeyValue(
                    key=key,
                    value=_utils.python_to_val_proto(value, allow_collection=True),
                )
            )

        msg = _ExperimentRunService.LogAttributes(
            id=self.id, attributes=attribute_keyvals
        )
        data = _utils.proto_to_json(msg)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logAttributes".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "some attribute with some input key already exists;"
                    " consider using observations instead, or setting overwrite=True."
                )
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
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            params=data,
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )
        attributes = _utils.unravel_key_values(response_msg.attributes)
        try:
            attribute = attributes[key]
            try:
                return data_types._VertaDataType._from_dict(attribute)
            except (KeyError, TypeError, ValueError):
                return attribute
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
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            params=data,
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )
        attributes = _utils.unravel_key_values(response_msg.attributes)
        for key, attribute in attributes.items():
            try:
                attributes[key] = data_types._VertaDataType._from_dict(attribute)
            except (KeyError, TypeError, ValueError):
                pass
        return attributes

    def _delete_attributes(self, keys):
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteExperimentRunAttributes".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json={"id": self.id, "attribute_keys": keys},
        )
        _utils.raise_for_http_error(response)

    def _delete_metrics(self, keys):
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteMetrics".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json={"id": self.id, "metric_keys": keys},
        )
        _utils.raise_for_http_error(response)

    def _delete_observations(self, keys):
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteObservations".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json={"id": self.id, "observation_keys": keys},
        )
        _utils.raise_for_http_error(response)

    def _delete_hyperparameters(self, keys):
        response = _utils.make_request(
            "DELETE",
            "{}://{}/api/v1/modeldb/experiment-run/deleteHyperparameters".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json={"id": self.id, "hyperparameter_keys": keys},
        )
        _utils.raise_for_http_error(response)

    def log_metric(self, key, value, overwrite=False):
        """
        Logs a metric to this Experiment Run.

        If the metadatum of interest might recur, :meth:`.log_observation` should be used instead.

        Parameters
        ----------
        key : str
            Name of the metric.
        value : one of {None, bool, float, int, str}
            Value of the metric.
        overwrite : bool, default False
            Whether to allow overwriting an existing metric with key `key`.

        """
        _utils.validate_flat_key(key)

        metric = _CommonCommonService.KeyValue(
            key=key, value=_utils.python_to_val_proto(value)
        )
        msg = _ExperimentRunService.LogMetric(id=self.id, metric=metric)
        data = _utils.proto_to_json(msg)
        if overwrite:
            self._delete_metrics([key])
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logMetric".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "metric with key {} already exists;"
                    " consider using observations instead".format(key)
                )
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_metrics(self, metrics, overwrite=False):
        """
        Logs potentially multiple metrics to this Experiment Run.

        Parameters
        ----------
        metrics : dict of str to {None, bool, float, int, str}
            Metrics.
        overwrite : bool, default False
            Whether to allow overwriting an existing metric with key `key`.

        """
        # validate all keys first
        for key in six.viewkeys(metrics):
            _utils.validate_flat_key(key)

        # build KeyValues
        metric_keyvals = []
        keys = []
        for key, value in six.viewitems(metrics):
            metric_keyvals.append(
                _CommonCommonService.KeyValue(
                    key=key, value=_utils.python_to_val_proto(value)
                )
            )
            keys.append(key)

        msg = _ExperimentRunService.LogMetrics(id=self.id, metrics=metric_keyvals)
        data = _utils.proto_to_json(msg)
        if overwrite:
            self._delete_metrics(keys)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logMetrics".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "some metric with some input key already exists;"
                    " consider using observations instead"
                )
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

    def log_hyperparameter(self, key, value, overwrite=False):
        """
        Logs a hyperparameter to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the hyperparameter.
        value : one of {None, bool, float, int, str}
            Value of the hyperparameter.
        overwrite : bool, default False
            Whether to allow overwriting an existing hyperparameter with key `key`.

        """
        _utils.validate_flat_key(key)

        hyperparameter = _CommonCommonService.KeyValue(
            key=key, value=_utils.python_to_val_proto(value)
        )
        msg = _ExperimentRunService.LogHyperparameter(
            id=self.id, hyperparameter=hyperparameter
        )
        data = _utils.proto_to_json(msg)
        if overwrite:
            self._delete_hyperparameters([key])
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logHyperparameter".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "hyperparameter with key {} already exists;"
                    " consider using observations instead".format(key)
                )
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

    def log_hyperparameters(self, hyperparams, overwrite=False):
        """
        Logs potentially multiple hyperparameters to this Experiment Run.

        Parameters
        ----------
        hyperparameters : dict of str to {None, bool, float, int, str}
            Hyperparameters.
        overwrite : bool, default False
            Whether to allow overwriting an existing hyperparameter with key `key`.

        """
        # validate all keys first
        for key in six.viewkeys(hyperparams):
            _utils.validate_flat_key(key)

        # build KeyValues
        hyperparameter_keyvals = []
        keys = []
        for key, value in six.viewitems(hyperparams):
            hyperparameter_keyvals.append(
                _CommonCommonService.KeyValue(
                    key=key, value=_utils.python_to_val_proto(value)
                )
            )
            keys.append(key)

        msg = _ExperimentRunService.LogHyperparameters(
            id=self.id, hyperparameters=hyperparameter_keyvals
        )
        data = _utils.proto_to_json(msg)
        if overwrite:
            self._delete_hyperparameters(keys)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logHyperparameters".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "some hyperparameter with some input key already exists;"
                    " consider using observations instead"
                )
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
            six.raise_from(
                KeyError("no hyperparameter found with key {}".format(key)), None
            )

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

    def log_dataset_version(self, key, dataset_version, overwrite=False):
        """
        Logs a Verta DatasetVersion to this ExperimentRun with the given key.

        Parameters
        ----------
        key : str
            Name of the dataset version.
        dataset_version : :class:`~verta.dataset.entities.DatasetVersion`
            Dataset version.
        overwrite : bool, default False
            Whether to allow overwriting a dataset version.

        """
        if not isinstance(dataset_version, _dataset.DatasetVersion):
            raise TypeError("`dataset_version` must be of type DatasetVersion")

        # TODO: hack because path_only artifact needs a placeholder path
        dataset_path = "See attached dataset version"

        # log key-path to ModelDB
        Message = _ExperimentRunService.LogDataset
        artifact_msg = _CommonCommonService.Artifact(
            key=key,
            path=dataset_path,
            path_only=True,
            artifact_type=_CommonCommonService.ArtifactTypeEnum.DATA,
            linked_artifact_id=dataset_version.id,
        )
        msg = Message(id=self.id, dataset=artifact_msg, overwrite=overwrite)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logDataset".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
        if not response.ok:
            if response.status_code == 409:
                raise ValueError(
                    "dataset with key {} already exists;"
                    " consider setting overwrite=True".format(key)
                )
            else:
                _utils.raise_for_http_error(response)

        self._clear_cache()

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
                return _dataset_version.DatasetVersion(
                    self._conn,
                    self._conf,
                    _dataset_version.DatasetVersion._get_proto_by_id(
                        self._conn, linked_id
                    ),
                )
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
        `DatasetVersion <dataset.html>`_
            DatasetVersion associated with the given key.

        """
        return self.get_dataset(key)

    def log_tf_saved_model(self, export_dir):
        tempf = _artifact_utils.zip_dir(export_dir)

        # TODO: change _log_artifact() to not read file into memory
        self._log_artifact(
            "tf_saved_model", tempf, _CommonCommonService.ArtifactTypeEnum.BLOB, "zip"
        )

    def log_model(
        self,
        model,
        custom_modules=None,
        model_api=None,
        artifacts=None,
        overwrite=False,
    ):
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

        # serialize model
        _utils.THREAD_LOCALS.active_experiment_run = self
        try:
            serialized_model, method, model_type = _artifact_utils.serialize_model(
                model
            )
        finally:
            _utils.THREAD_LOCALS.active_experiment_run = None
        try:
            extension = _artifact_utils.get_file_ext(serialized_model)
        except (TypeError, ValueError):
            extension = _artifact_utils.ext_from_method(method)
        if self._conf.debug:
            print("[DEBUG] model is type {}".format(model_type))

        if artifacts and model_type != "class":
            raise ValueError("`artifacts` can only be provided if `model` is a class")

        # associate artifact dependencies
        if artifacts:
            self.log_attribute(_MODEL_ARTIFACTS_ATTR_KEY, artifacts, overwrite)

        # create and upload model API
        if model_type or model_api:  # only if provided or model is deployable
            if model_api is None:
                model_api = utils.ModelAPI()
            if self._conf.debug:
                print("[DEBUG] model API is:")
                pprint.pprint(model_api.to_dict())

            self._log_artifact(
                _artifact_utils.MODEL_API_KEY,
                model_api,
                _CommonCommonService.ArtifactTypeEnum.BLOB,
                "json",
                overwrite=overwrite,
            )

        # create and upload custom modules
        if model_type or custom_modules:  # only if provided or model is deployable
            custom_modules_artifact = self._custom_modules_as_artifact(custom_modules)
            self._log_artifact(
                _artifact_utils.CUSTOM_MODULES_KEY,
                custom_modules_artifact,
                _CommonCommonService.ArtifactTypeEnum.BLOB,
                "zip",
                overwrite=overwrite,
            )

        # upload model
        self._log_artifact(
            self._MODEL_KEY,
            serialized_model,
            _CommonCommonService.ArtifactTypeEnum.MODEL,
            extension,
            method,
            model_type,
            overwrite=overwrite,
        )

    def get_model(self):
        """
        Gets the model artifact for Verta model deployment from this Experiment Run.

        Returns
        -------
        object
            Model for deployment.

        """
        model, _ = self._get_artifact(self._MODEL_KEY)
        return _artifact_utils.deserialize_model(model, error_ok=True)

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
        bytestream, extension = six.BytesIO(), "png"
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

        self._log_artifact(
            key,
            image,
            _CommonCommonService.ArtifactTypeEnum.IMAGE,
            extension,
            overwrite=overwrite,
        )

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
            Image = importer.maybe_dependency("PIL.Image")
            if Image is None:  # Pillow not installed
                return six.BytesIO(image)
            try:
                return Image.open(six.BytesIO(image))
            except IOError:  # can't be handled by Pillow
                return six.BytesIO(image)

    def log_artifact(self, key, artifact, overwrite=False):
        """
        Logs an artifact to this Experiment Run.

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
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        # zip if `artifact` is directory path
        if isinstance(artifact, six.string_types) and os.path.isdir(artifact):
            artifact = _artifact_utils.zip_dir(artifact)

        try:
            extension = _artifact_utils.get_file_ext(artifact)
        except (TypeError, ValueError):
            extension = None

        self._log_artifact(
            key,
            artifact,
            _CommonCommonService.ArtifactTypeEnum.BLOB,
            extension,
            overwrite=overwrite,
        )

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
                artifact_stream = open(artifact, "rb")
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

    def get_artifact_keys(self):
        """
        Gets the artifact keys of this Experiment Run.

        Returns
        -------
        list of str
            List of artifact keys of this Experiment Run.

        """
        self._refresh_cache()
        return list(map(lambda artifact: artifact.key, self._msg.artifacts))

    def download_artifact(self, key, download_to_path):
        download_to_path = os.path.abspath(download_to_path)

        artifact = self._get_artifact_msg(key)

        # create parent dirs
        pathlib.Path(download_to_path).parent.mkdir(parents=True, exist_ok=True)
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

                if (
                    artifact.filename_extension == _artifact_utils.ZIP_EXTENSION
                ):  # verta-created ZIP
                    downloader = _request_utils.download_zipped_dir
                else:
                    downloader = _request_utils.download_file
                downloader(response, download_to_path, overwrite_ok=True)

        return download_to_path

    def download_model(self, download_to_path):
        return self.download_artifact(self._MODEL_KEY, download_to_path)

    def log_observation(
        self, key, value, timestamp=None, epoch_num=None, overwrite=False
    ):
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
        overwrite : bool, default False
            Whether to allow overwriting an existing observation with key `key`.

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
            if not isinstance(epoch_num, six.integer_types) and not (
                isinstance(epoch_num, float) and epoch_num.is_integer()
            ):
                raise TypeError(
                    "`epoch_num` must be int, not {}".format(type(epoch_num))
                )
            if epoch_num < 0:
                raise ValueError("`epoch_num` must be non-negative")

        attribute = _CommonCommonService.KeyValue(
            key=key, value=_utils.python_to_val_proto(value)
        )
        observation = _ExperimentRunService.Observation(
            attribute=attribute, timestamp=timestamp
        )  # TODO: support Artifacts
        if epoch_num is not None:
            observation.epoch_number.number_value = (
                epoch_num  # pylint: disable=no-member
            )

        msg = _ExperimentRunService.LogObservation(id=self.id, observation=observation)
        data = _utils.proto_to_json(msg)
        if overwrite:
            self._delete_observations([key])
        response = _utils.make_request(
            "POST",
            "{}://{}/api/v1/modeldb/experiment-run/logObservation".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            json=data,
        )
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
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/experiment-run/getObservations".format(
                self._conn.scheme, self._conn.socket
            ),
            self._conn,
            params=data,
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )
        if len(response_msg.observations) == 0:
            raise KeyError("no observation found with key {}".format(key))
        else:
            return [
                _utils.unravel_observation(observation)[1:]  # drop key from tuple
                for observation in response_msg.observations
            ]  # TODO: support Artifacts

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

    def log_environment(self, env, overwrite=False):
        if not isinstance(env, _Environment):
            raise TypeError(
                "`env` must be of type Environment, not {}".format(type(env))
            )

        if self.has_environment and not overwrite:
            raise ValueError(
                "environment already exists; consider setting overwrite=True"
            )

        msg = _ExperimentRunService.LogEnvironment(
            id=self.id, environment=env._as_env_proto()
        )
        response = self._conn.make_proto_request(
            "POST",
            "/api/v1/modeldb/experiment-run/logEnvironment",
            body=msg,
        )
        if response.ok:
            # self._refresh_cache()
            self._fetch_with_no_cache()
        else:
            _utils.raise_for_http_error(response)

    def log_modules(self, paths, search_path=None):
        """
        Logs local files that are dependencies for a deployed model to this Experiment Run.

        .. deprecated:: 0.13.13
           The behavior of this function has been merged into :meth:`log_model` as its
           ``custom_modules`` parameter; consider using that instead.
        .. deprecated:: 0.12.4
           The `search_path` parameter is no longer necessary and will be removed in an upcoming version; consider
           removing it from the function call.

        Parameters
        ----------
        paths : str or list of str
            Paths to local Python modules and other files that the deployed model depends on. If a
            directory is provided, all files within will be included.

        """
        warnings.warn(
            "The behavior of this function has been merged into log_model() as its"
            " `custom_modules` parameter; consider using that instead",
            category=FutureWarning,
        )
        if search_path is not None:
            warnings.warn(
                "`search_path` is no longer used and will be removed in a later version;"
                " consider removing it from the function call",
                category=FutureWarning,
            )

        custom_modules_artifact = self._custom_modules_as_artifact(paths)
        self._log_artifact(
            _artifact_utils.CUSTOM_MODULES_KEY,
            custom_modules_artifact,
            _CommonCommonService.ArtifactTypeEnum.BLOB,
            "zip",
        )

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

        self._log_artifact(
            "setup_script",
            script,
            _CommonCommonService.ArtifactTypeEnum.BLOB,
            "py",
            overwrite=overwrite,
        )

    def download_docker_context(self, download_to_path, self_contained=False):
        """
        Downloads this Experiment Run's Docker context ``tgz``.

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
            "run_id": self.id,
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
            id=self.id,
            key=key,
            method=method.upper(),
            artifact_type=artifact_type,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        response = _utils.make_request(
            "POST", self._request_url.format("getUrlForArtifact"), self._conn, json=data
        )
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(
            _utils.body_to_json(response), Message.Response
        )

        url = response_msg.url
        # accommodate port-forwarded NFS store
        if "https://localhost" in url[:20]:
            url = "http" + url[5:]
        if "localhost%3a" in url[:20]:
            url = url.replace("localhost%3a", "localhost:")
        if "localhost%3A" in url[:20]:
            url = url.replace("localhost%3A", "localhost:")
        response_msg.url = url

        return response_msg

    def delete(self):
        """
        Deletes this experiment run.

        """
        request_url = (
            "{}://{}/api/v1/modeldb/experiment-run/deleteExperimentRun".format(
                self._conn.scheme, self._conn.socket
            )
        )
        response = _utils.make_request(
            "DELETE",
            request_url,
            self._conn,
            json={"id": self.id},
        )
        _utils.raise_for_http_error(response)
