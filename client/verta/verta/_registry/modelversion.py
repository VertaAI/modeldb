# -*- coding: utf-8 -*-

from __future__ import print_function

from .._tracking.entity import _ModelDBEntity

from .._protos.public.registry import RegistryService_pb2 as _ModelVersionService
from .._protos.public.common import CommonService_pb2 as _CommonCommonService

from .._internal_utils import _utils


class RegisteredModelVersion(_ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(RegisteredModelVersion, self).__init__(conn, conf, _ModelVersionService, "registered_model_versions", msg)

    def __repr__(self):
        raise NotImplementedError

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.version

    @classmethod
    def _generate_default_name(cls):
        return "ModelVersion {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ModelVersionService.GetModelVersionRequest
        url_endpoint = "/api/v1/registry/registered_model_versions/{}".format(id)
        response = conn.make_proto_request("GET", url_endpoint)

        return conn.maybe_proto_response(response, Message.Response).model_version

    @classmethod
    def _get_proto_by_name(cls, conn, name, registered_model_id):
        # return None # Not implemented yet!

        Message = _ModelVersionService.FindModelVersionRequest
        RegisteredModelIDMessage = _ModelVersionService.RegisteredModelIdentification

        predicates = [
            _CommonCommonService.KeyValueQuery(key="version",
                                               value=_utils.python_to_val_proto(name),
                                               operator=_CommonCommonService.OperatorEnum.EQ)
        ]
        endpoint = "/api/v1/registry/{}/versions".format(registered_model_id)
        msg = Message(id=RegisteredModelIDMessage(registered_model_id=registered_model_id), predicates=predicates)

        proto_response = conn.make_proto_request("POST", endpoint, body=msg)
        response = conn.maybe_proto_response(proto_response, Message.Response)

        if not response.model_versions:
            return None

        return response.model_versions[0] # should only have 1 entry here, as name/version is unique

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, labels=None, attrs=None, date_created=None, registered_model_id=None, registered_model_name=None):
        # ctx is always None here
        ModelVersionMessage = _ModelVersionService.ModelVersion
        SetModelVersionMessage = _ModelVersionService.SetModelVersion

        model_version_msg = ModelVersionMessage(version=name, description=desc, registered_model_id=registered_model_id,
                                                labels=labels, time_created=date_created, time_updated=date_created)
        url_endpoint = "/api/v1/registry/{}/versions".format(registered_model_id)
        response = conn.make_proto_request("POST", url_endpoint, body=model_version_msg)
        model_version = conn.must_proto_response(response, SetModelVersionMessage.Response).model_version

        print("Created new ModelVersion: {}".format(model_version.name))
        return model_version

    def set_model(self, model, overwrite=False):
        # similar to ExperimentRun.log_artifact
        raise NotImplementedError

    def add_asset(self, key, asset, overwrite=False):
        # similar to ExperimentRun.log_artifact
        raise NotImplementedError

    def del_asset(self, key):
        raise NotImplementedError

    def set_environment(self, env):
        # Env must be an EnvironmentBlob. Let's re-use the functionality from there
        raise NotImplementedError

    def del_environment(self):
        raise NotImplementedError
