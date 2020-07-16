# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from .._internal_utils import _utils

from . import RegisteredModel


class RegisteredModels(_utils.LazyList):
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'name',
        'date_created',
    }

    def __init__(self, conn, conf):
        raise NotImplementedError

    def __repr__(self):
        raise NotImplementedError

    def _call_back_end(self, msg):
        raise NotImplementedError

    def _create_element(self, msg):
        return RegisteredModel(self._conn, self._conf, msg)

    def with_workspace(self, workspace_name=None):
        new_list = copy.deepcopy(self)
        new_list._msg.workspace_name = workspace_name
        return new_list
