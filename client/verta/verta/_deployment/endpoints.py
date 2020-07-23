# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from . import Endpoint


# a rough copy of LazyList's API, because Endpoints don't use protos, find, or pagination
class Endpoints(object):
    def __init__(self, conn, conf, workspace_name):
        self._conn = conn
        self._conf = conf

        self._workspace_name = workspace_name
        # store state Clientside because we can't make paginated calls anyway
        self._ids = self._get_ids()

    def _get_ids(self):
        raise NotImplementedError
        # TODO: list endpoints with call to call /api/v1/deployment/workspace/{}/endpoints
        # TODO: return [endpoint['id'] for endpoint in response.json().get('endpoints', [])]

    def __repr__(self):
        return "<{} Endpoints>".format(self.__len__())

    def __getitem__(self, index):
        raise NotImplementedError
        # TODO: create Endpoint using self._ids[index]

    def __len__(self):
        return len(self._ids)

    def with_workspace(self, workspace_name):  # unlike MDB endpoints, workspace required
        raise NotImplementedError
        return new_list
