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
        'experiment_run_id',
        'version',
        'time_created',
        'time_updated',
        'labels',
    }

    def __init__(self, conn, conf):
        super(RegisteredModelVersions, self).__init__(
            conn, conf,
            _RegisteredModelService.FindModelVersionRequest(),
        )

    def __repr__(self):
        return "<RegisteredModelVersions containing {} versions>".format(self.__len__())

    def _call_back_end(self, msg):
        if self._msg.id.registered_model_id == 0:
            url = "/api/v1/registry/model_versions/find"
        else:
            url = "/api/v1/registry/registered_models/{}/model_versions/find".format(self._msg.id.registered_model_id)
        response = self._conn.make_proto_request("POST", url, body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.model_versions, response.total_records

    def _create_element(self, msg):
        return RegisteredModelVersion(self._conn, self._conf, msg)

    def with_model(self, registered_model=None):
        new_list = copy.deepcopy(self)
        if registered_model:
            new_list._msg.id.registered_model_id = registered_model.id
        else:
            new_list._msg.id.registered_model_id = 0
        return new_list

    def set_page_limit(self, msg, param):
        msg.pagination.page_limit = param
        return msg

    def set_page_number(self, msg, param):
        msg.pagination.page_number = param
        return msg

    def page_limit(self, msg):
        return msg.pagination.page_limit

    def page_number(self, msg):
        return msg.pagination.page_number
