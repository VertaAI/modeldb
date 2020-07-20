# -*- coding: utf-8 -*-

from __future__ import print_function

from .._internal_utils._utils import NoneProtoResponse
from .._tracking.entity import _ModelDBEntity

from .._protos.public.registry import RegistryService_pb2 as _ModelVersionService
from .._protos.public.common import CommonService_pb2 as _CommonCommonService

from .._internal_utils import _utils
from ..environment import _Environment


class RegisteredModelVersion(_ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(RegisteredModelVersion, self).__init__(conn, conf, _ModelVersionService, "registered_model_version", msg)

    def __repr__(self):
        raise NotImplementedError

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.version

    @property
    def has_environment(self):
        self._refresh_cache()
        return self._msg.environment.HasField("python") or self._msg.environment.HasField("docker")

    @classmethod
    def _generate_default_name(cls):
        return "ModelVersion {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ModelVersionService.GetModelVersionRequest
        endpoint = "/api/v1/registry/registered_model_versions/{}".format(id)
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
        endpoint = "/api/v1/registry/{}/versions/find".format(registered_model_id)
        msg = Message(predicates=predicates)

        proto_response = conn.make_proto_request("POST", endpoint, body=msg)
        response = conn.maybe_proto_response(proto_response, Message.Response)

        if not response.model_versions:
            return None

        # should only have 1 entry here, as name/version is unique
        return response.model_versions[0]

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, labels=None, attrs=None, date_created=None):
        ModelVersionMessage = _ModelVersionService.ModelVersion
        SetModelVersionMessage = _ModelVersionService.SetModelVersion
        registered_model_id = ctx.registered_model.id

        model_version_msg = ModelVersionMessage(version=name, description=desc, registered_model_id=registered_model_id,
                                                labels=labels, time_created=date_created, time_updated=date_created)
        endpoint = "/api/v1/registry/{}/versions".format(registered_model_id)
        response = conn.make_proto_request("POST", endpoint, body=model_version_msg)
        model_version = conn.must_proto_response(response, SetModelVersionMessage.Response).model_version

        print("Created new ModelVersion: {}".format(model_version.version))
        return model_version

    def set_model(self, model, overwrite=False):
        # similar to ExperimentRun.log_artifact
        raise NotImplementedError

    def get_model(self):
        # similar to ExperimentRun.get_model
        raise NotImplementedError

    def add_asset(self, key, asset, overwrite=False):
        # similar to ExperimentRun.log_artifact
        raise NotImplementedError

    def get_asset(self, key):
        # similar to ExperimentRun.get_artifact
        raise NotImplementedError

    def del_asset(self, key):
        raise NotImplementedError

    def log_environment(self, env):
        if not isinstance(env, _Environment):
            raise TypeError("`env` must be of type Environment, not {}".format(type(env)))

        self._refresh_cache()
        self._msg.environment.CopyFrom(env._msg)
        self._update()

    def del_environment(self):
        self._refresh_cache()
        self._msg.ClearField("environment")
        self._update()

    def add_label(self, label):
        if label is None:
            raise ValueError("label is not specified")
        self._clear_cache()
        self._refresh_cache()
        if label not in self._msg.labels:
            self._msg.labels.append(label)
            self._update()

    def del_label(self, label):
        if label is None:
            raise ValueError("label is not specified")
        self._clear_cache()
        self._refresh_cache()
        if label in self._msg.labels:
            self._msg.labels.remove(label)
            self._update()

    def get_labels(self):
        self._clear_cache()
        self._refresh_cache()
        return self._msg.labels

    def _update(self):
        response = self._conn.make_proto_request("PUT", "/api/v1/registry/{}/versions/{}".format(self._msg.registered_model_id, self.id),
                                                 body=self._msg)
        Message = _ModelVersionService.SetModelVersion
        if isinstance(self._conn.maybe_proto_response(response, Message.Response), NoneProtoResponse):
            raise ValueError("Model not found")
