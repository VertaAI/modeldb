# -*- coding: utf-8 -*-

from __future__ import print_function

import requests
import warnings

from .entity import _ModelDBEntity
from .experimentruns import ExperimentRuns

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
    def __init__(self, conn, conf,
                 proj_name=None,
                 desc=None, tags=None, attrs=None,
                 workspace=None,
                 public_within_org=None,
                 _proj_id=None):
        if proj_name is not None and _proj_id is not None:
            raise ValueError("cannot specify both `proj_name` and `_proj_id`")

        if workspace is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(workspace)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        if _proj_id is not None:
            proj = Project._get(conn, _proj_id=_proj_id)
            if proj is not None:
                print("set existing Project: {}".format(proj.name))
            else:
                raise ValueError("Project with ID {} not found".format(_proj_id))
        else:
            if proj_name is None:
                proj_name = Project._generate_default_name()
            try:
                proj = Project._create(conn, proj_name, desc, tags, attrs, workspace, public_within_org)
            except requests.HTTPError as e:
                if e.response.status_code == 403:  # cannot create in other workspace
                    proj = Project._get(conn, proj_name, workspace)
                    if proj is not None:
                        print("set existing Project: {} from {}".format(proj.name, WORKSPACE_PRINT_MSG))
                    else:  # no accessible project in other workspace
                        six.raise_from(e, None)
                elif e.response.status_code == 409:  # already exists
                    if any(param is not None for param in (desc, tags, attrs, public_within_org)):
                        warnings.warn(
                            "Project with name {} already exists;"
                            " cannot set `desc`, `tags`, `attrs`, or `public_within_org`".format(proj_name)
                        )
                    proj = Project._get(conn, proj_name, workspace)
                    if proj is not None:
                        print("set existing Project: {} from {}".format(proj.name, WORKSPACE_PRINT_MSG))
                    else:
                        raise RuntimeError("unable to retrieve Project {};"
                                           " please notify the Verta development team".format(proj_name))
                else:
                    raise e
            else:
                print("created new Project: {} in {}".format(proj.name, WORKSPACE_PRINT_MSG))

        super(Project, self).__init__(conn, conf, _ProjectService, "project", proj.id)

    def __repr__(self):
        return "<Project \"{}\">".format(self.name)

    @property
    def name(self):
        Message = _ProjectService.GetProjectById
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/project/getProjectById".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.project.name

    @property
    def expt_runs(self):
        # get runs in this Project
        runs = ExperimentRuns(self._conn, self._conf)
        runs._msg.project_id = self.id
        return runs

    @staticmethod
    def _generate_default_name():
        return "Proj {}".format(_utils.generate_default_name())

    @staticmethod
    def _get(conn, proj_name=None, workspace=None, _proj_id=None):
        if _proj_id is not None:
            Message = _ProjectService.GetProjectById
            msg = Message(id=_proj_id)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/project/getProjectById".format(conn.scheme, conn.socket),
                                           conn, params=data)

            if response.ok:
                response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
                return response_msg.project
            else:
                if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                        or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                    return None
                else:
                    _utils.raise_for_http_error(response)
        elif proj_name is not None:
            Message = _ProjectService.GetProjectByName
            msg = Message(name=proj_name, workspace_name=workspace)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/project/getProjectByName".format(conn.scheme, conn.socket),
                                           conn, params=data)

            if response.ok:
                response_json = _utils.body_to_json(response)
                response_msg = _utils.json_to_proto(response_json, Message.Response)
                if workspace is None or response_json.get('project_by_user'):
                    # user's personal workspace
                    proj = response_msg.project_by_user
                else:
                    proj = response_msg.shared_projects[0]

                if not proj.id:  # 200, but empty message
                    raise RuntimeError("unable to retrieve Project {};"
                                       " please notify the Verta development team".format(proj_name))

                return proj
            else:
                if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                        or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                    return None
                else:
                    _utils.raise_for_http_error(response)
        else:
            raise ValueError("insufficient arguments")

    @staticmethod
    def _create(conn, proj_name, desc=None, tags=None, attrs=None, workspace=None, public_within_org=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        Message = _ProjectService.CreateProject
        msg = Message(name=proj_name, description=desc, tags=tags, attributes=attrs, workspace_name=workspace)
        if public_within_org:
            if workspace is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(workspace, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(workspace)
                )
            else:
                msg.project_visibility = _ProjectService.ORG_SCOPED_PUBLIC
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/project/createProject".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            return response_msg.project
        else:
            _utils.raise_for_http_error(response)
