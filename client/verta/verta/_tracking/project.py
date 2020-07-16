# -*- coding: utf-8 -*-

from __future__ import print_function

import requests
import warnings

from .entity import _ModelDBEntity
from .experimentruns import ExperimentRuns
from .experiments import Experiments

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import ProjectService_pb2 as _ProjectService

from ..external import six

from .._internal_utils import (
    _utils,
)


class Project(_ModelDBEntity):
    """
    Object representing a machine learning Project.

    This class provides read/write functionality for Project metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_project`.

    Attributes
    ----------
    id : str
        ID of this Project.
    name : str
        Name of this Project.
    expt_runs : :class:`ExperimentRuns`
        Experiment Runs under this Project.

    """
    def __init__(self, conn, conf, msg):
        super(Project, self).__init__(conn, conf, _ProjectService, "project", msg)

    def __repr__(self):
        return "<Project \"{}\">".format(self.name)

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def experiments(self):
        return Experiments(self._conn, self._conf).with_project(self)

    @property
    def expt_runs(self):
        # get runs in this Project
        return ExperimentRuns(self._conn, self._conf).with_project(self)

    @classmethod
    def _generate_default_name(cls):
        return "Proj {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ProjectService.GetProjectById
        msg = Message(id=id)
        response = conn.make_proto_request("GET",
                                           "/api/v1/modeldb/project/getProjectById",
                                           params=msg)
        return conn.maybe_proto_response(response, Message.Response).project

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _ProjectService.GetProjectByName
        msg = Message(name=name, workspace_name=workspace)
        response = conn.make_proto_request("GET",
                                           "/api/v1/modeldb/project/getProjectByName",
                                           params=msg)
        response = conn.maybe_proto_response(response, Message.Response)
        if workspace is None or response.HasField("project_by_user"):
            return response.project_by_user
        elif response.HasField("shared_projects"):
            return response.shared_projects[0]
        else:
            return None

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, public_within_org=None):
        Message = _ProjectService.CreateProject
        msg = Message(name=name, description=desc, tags=tags, attributes=attrs, workspace_name=ctx.workspace_name)
        if public_within_org:
            if ctx.workspace_name is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(ctx.workspace_name, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(ctx.workspace_name)
                )
            else:
                msg.project_visibility = _ProjectService.ORG_SCOPED_PUBLIC

        response = conn.make_proto_request("POST",
                                           "/api/v1/modeldb/project/createProject",
                                           body=msg)
        proj = conn.must_proto_response(response, Message.Response).project

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print("created new Project: {} in {}".format(proj.name, WORKSPACE_PRINT_MSG))
        return proj
