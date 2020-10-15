# -*- coding: utf-8 -*-

import copy

from .._protos.public.modeldb import DatasetVersionService_pb2 as _DatasetVersionService

from .._internal_utils import _utils


class DatasetVersions(_utils.LazyList):
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'dataset_id',
        'name',
        'tags',
        'time_logged',
    }

    def __init__(self, conn, conf):
        super(DatasetVersions, self).__init__(
            conn, conf,
            _DatasetVersionService.FindDatasetVersions(),
        )

    def __repr__(self):
        return "<DatasetVersions containing {} dataset versions>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                 "/api/v1/modeldb/dataset-version/findDatasetVersions",
                                                 body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.dataset_versions, response.total_records

    def _create_element(self, msg):
        from .._dataset import DatasetVersion  # TODO: avoid circular import
        # old interface; takes ID instead of msg
        return DatasetVersion(self._conn, self._conf, _dataset_version_id=msg.id)

    def with_dataset(self, dataset=None):
        new_list = copy.deepcopy(self)
        if dataset:
            new_list._msg.dataset_id = dataset.id
        else:
            new_list._msg.dataset_id = ''
        return new_list
