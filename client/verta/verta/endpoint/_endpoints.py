# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from ._endpoint import Endpoint


# a rough copy of LazyList's API, because Endpoints don't use protos, find, or pagination
class Endpoints(object):
    def __init__(self, conn, conf, workspace_name):
        self._conn = conn
        self._conf = conf

        self._workspace_name = workspace_name
        # store state Clientside because we can't make paginated calls anyway
        self._ids = self._get_ids()

    def _get_ids(self):
        endpoints = Endpoint._get_endpoints(self._conn, self._workspace_name)
        return list(map(lambda endpoint: endpoint['id'], endpoints))

    def __repr__(self):
        return "<{} Endpoints>".format(self.__len__())

    def __getitem__(self, index):
        return Endpoint._get_by_id(self._conn, self._conf, self._workspace_name, self._ids[index])

    def __len__(self):
        return len(self._ids)

    def with_workspace(self, workspace_name):  # unlike MDB endpoints, workspace required
        new_list = copy.deepcopy(self)
        new_list._workspace_name = workspace_name
        # store state Clientside because we can't make paginated calls anyway
        new_list._ids = new_list._get_ids()
        return new_list
