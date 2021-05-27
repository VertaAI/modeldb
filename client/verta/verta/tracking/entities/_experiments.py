# -*- coding: utf-8 -*-

from __future__ import print_function

import copy

from verta._protos.public.modeldb import ExperimentService_pb2 as _ExperimentService

from verta._internal_utils import _utils

from ._experiment import Experiment


class Experiments(_utils.LazyList):
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
        super(Experiments, self).__init__(
            conn, conf,
            _ExperimentService.FindExperiments(),
        )

    def __repr__(self):
        return "<Experiment containing {} experiments>".format(self.__len__())

    def _call_back_end(self, msg):
        response = self._conn.make_proto_request("POST",
                                                "/api/v1/modeldb/experiment/findExperiments",
                                                body=msg)
        response = self._conn.must_proto_response(response, msg.Response)
        return response.experiments, response.total_records

    def _create_element(self, msg):
        return Experiment(self._conn, self._conf, msg)

    def with_project(self, proj=None):
        new_list = copy.deepcopy(self)
        if proj:
            new_list._msg.project_id = proj.id
        else:
            new_list._msg.project_id = ''
        return new_list

    def with_workspace(self, workspace=None):
        """Returns experiments in the specified workspace.

        Parameters
        ----------
        workspace : str, optional
            Workspace name. If not provided, uses personal workspace.

        Returns
        -------
        :class:`Experiments`
            Filtered experiments.

        """
        new_list = copy.deepcopy(self)
        new_list._msg.ClearField('project_id')
        new_list._msg.workspace_name = workspace or ''
        return new_list
