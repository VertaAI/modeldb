# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from ._endpoint import Endpoint


# a rough copy of LazyList's API, because Endpoints don't use protos, find, or pagination
class Endpoints(object):
    """Collection object for finding endpoints.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        client = Client()
        # delete all endpoints in a given workspace
        for endpoint in client.endpoints.with_workspace("Demos"):
            endpoint.delete()

    """
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

    def with_workspace(self, workspace):  # unlike MDB endpoints, workspace required
        """Returns endpoints in the specified workspace.

        Parameters
        ----------
        workspace : str, optional
            Workspace name. If not provided, uses personal workspace.

        Returns
        -------
        :class:`Endpoints`
            Filtered endpoints.

        """
        new_list = copy.deepcopy(self)
        new_list._workspace_name = workspace
        # store state Clientside because we can't make paginated calls anyway
        new_list._ids = new_list._get_ids()
        return new_list
