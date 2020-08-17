# -*- coding: utf-8 -*-

import copy

from .._protos.public.modeldb import DatasetService_pb2 as _DatasetService

from .._internal_utils import _utils


class Datasets(_utils.LazyList):
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'name',
        'tags',
        'time_created',
    }

    def __init__(self, conn, conf):
        super(Datasets, self).__init__(
            conn, conf,
            _DatasetService.FindDatasets(),
        )

    def __repr__(self):
        return "<Datasets containing {} datasets>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                 "/api/v1/modeldb/dataset/findDatasets",
                                                 body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.datasets, response.total_records

    def _create_element(self, msg):
        from .._dataset import Dataset  # TODO: avoid circular import
        # old interface; takes ID instead of msg
        return Dataset(self._conn, self._conf, _dataset_id=msg.id)

    def with_workspace(self, workspace_name=None):
        new_list = copy.deepcopy(self)
        new_list._msg.workspace_name = workspace_name
        return new_list

    def with_ids(self, ids=None):
        new_list = copy.deepcopy(self)
        del new_list._msg.dataset_ids[:]
        if ids:
            new_list._msg.dataset_ids.extend(ids)
        return new_list
