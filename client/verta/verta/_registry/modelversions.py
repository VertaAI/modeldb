# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from .._internal_utils import _utils

from .modelversion import RegisteredModelVersion


class RegisteredModelVersions(_utils.LazyList):
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
        return RegisteredModelVersion(self._conn, self._conf, msg)

    def with_model(self, registered_model=None):
        new_list = copy.deepcopy(self)
        if registered_model:
            new_list._msg.id.registered_model_id = registered_model.id
        else:
            new_list._msg.id.registered_model_id = None
        return new_list
