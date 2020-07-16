# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from .._protos.public.registry import RegistryService_pb2 as _RegisteredModelService
from .._internal_utils import _utils

from .modelversion import RegisteredModelVersion


class RegisteredModelVersions(_utils.LazyList):
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'registered_model_id',
        'version',
        'time_created',
        'time_updated',
        'labels',
    }

    def __init__(self, conn, conf):
        super(RegisteredModelVersions, self).__init__(
            conn, conf,
            _RegisteredModelService.FindRegisteredModelRequest(),
        )

    def __repr__(self):
        return "<RegisteredModelVersions containing {} versions>".format(self.__len__())

    def _call_back_end(self, msg):
        if msg.workspace_name:
            url = "/api/v1/registry/workspaces/{}/registered_models/find".format(msg.workspace_name)
        else:
            url = "/api/v1/registry/registered_models/find"
        response = self._conn.make_proto_request("POST", url, body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.registered_models, response.total_records
        raise NotImplementedError

    def _create_element(self, msg):
        return RegisteredModelVersion(self._conn, self._conf, msg)

    def with_model(self, registered_model=None):
        new_list = copy.deepcopy(self)
        if registered_model:
            new_list._msg.id.registered_model_id = registered_model.id
        else:
            new_list._msg.id.registered_model_id = None
        return new_list
