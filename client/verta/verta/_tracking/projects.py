# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from .project import Project

from .._protos.public.modeldb import ProjectService_pb2 as _ProjectService

from .._internal_utils import (
    _utils,
)


class Projects(_utils.LazyList):
    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id',
        'name',
        'date_created',
        'date_updated',
        'attributes',
        'tags',
    }

    def __init__(self, conn, conf):
        super(Projects, self).__init__(
            conn, conf,
            _ProjectService.FindProjects(),
        )

    def __repr__(self):
        return "<Projects containing {} projects>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                "/api/v1/modeldb/project/findProjects",
                                                body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.projects, response.total_records

    def _create_element(self, msg):
        return Project(self._conn, self._conf, msg)

    def with_workspace(self, workspace_name=None):
        new_list = copy.deepcopy(self)
        new_list._msg.workspace_name = workspace_name or ''
        return new_list
