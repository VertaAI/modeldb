# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from ..._protos.public.registry import RegistryService_pb2 as _RegistryService
from ..._internal_utils import _utils

from ._model import RegisteredModel


class RegisteredModels(_utils.LazyList):
    """Collection object for finding registered models.

    There should not be a need to instantiate this class directly; please use
    :attr:`Client.registered_models <verta.Client>`.

    """

    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'name',
        'time_created',
        'time_updated',
        'labels',
    }

    def __init__(self, conn, conf):
        super(RegisteredModels, self).__init__(
            conn, conf,
            _RegistryService.FindRegisteredModelRequest(),
        )

    def __repr__(self):
        return "<RegisteredModels containing {} models>".format(self.__len__())

    def _call_back_end(self, msg):
        if msg.workspace_name:
            url = "/api/v1/registry/workspaces/{}/registered_models/find".format(msg.workspace_name)
        else:
            url = "/api/v1/registry/registered_models/find"
        response = self._conn.make_proto_request("POST", url, body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.registered_models, response.total_records

    def _create_element(self, msg):
        return RegisteredModel(self._conn, self._conf, msg)

    def with_workspace(self, workspace=None):
        """Returns registered models in the specified workspace.

        Parameters
        ----------
        workspace : str, optional
            Workspace name. If not provided, uses personal workspace.

        Returns
        -------
        :class:`RegisteredModels`
            Filtered registered models.

        """
        new_list = copy.deepcopy(self)
        if workspace is not None:
            new_list._msg.workspace_name = workspace
        else:
            new_list._msg.workspace_name = ''
        return new_list

    def _set_page_limit(self, msg, param):
        msg.pagination.page_limit = param
        return msg

    def _set_page_number(self, msg, param):
        msg.pagination.page_number = param
        return msg

    def _page_limit(self, msg):
        return msg.pagination.page_limit

    def _page_number(self, msg):
        return msg.pagination.page_number
