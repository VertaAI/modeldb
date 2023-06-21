# -*- coding: utf-8 -*-

from __future__ import print_function

import re

from verta._protos.public.common import CommonService_pb2 as _CommonCommonService
from verta._protos.public.modeldb import ProjectService_pb2 as _ProjectService
from verta._protos.public.uac import Collaborator_pb2 as _Collaborator

from verta._vendored import six

from verta._internal_utils import _utils

from ._entity import _ModelDBEntity
from ._experimentruns import ExperimentRuns
from ._experiments import Experiments


class Project(_ModelDBEntity):
    """
    Object representing a machine learning Project.

    This class provides read/write functionality for Project metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_project() <verta.Client.set_project>`.

    Attributes
    ----------
    id : str
        ID of this Project.
    name : str
        Name of this Project.
    expt_runs : :class:`~verta.tracking.entities.ExperimentRuns`
        Experiment Runs under this Project.
    url : str
        Verta web app URL.

    """

    def __init__(self, conn, conf, msg):
        super(Project, self).__init__(conn, conf, _ProjectService, "project", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return "\n".join(
            (
                "name: {}".format(msg.name),
                "url: {}".format(self.url),
            )
        )

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

    @property
    def url(self):
        return "{}://{}/{}/projects/{}/summary".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
        )

    @property
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_service_id:
            return self._conn.get_workspace_name_from_id(self._msg.workspace_service_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @classmethod
    def _generate_default_name(cls):
        return "Proj {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _ProjectService.GetProjectById
        msg = Message(id=id)
        response = conn.make_proto_request(
            "GET", "/api/v1/modeldb/project/getProjectById", params=msg
        )
        return conn.maybe_proto_response(response, Message.Response).project

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _ProjectService.GetProjectByName
        msg = Message(name=name, workspace_name=workspace)
        response = conn.make_proto_request(
            "GET", "/api/v1/modeldb/project/getProjectByName", params=msg
        )
        response = conn.maybe_proto_response(response, Message.Response)
        if response.HasField("project_by_user") and response.project_by_user.id:
            return response.project_by_user
        elif hasattr(response, "shared_projects") and response.shared_projects:
            return response.shared_projects[0]
        else:
            return None

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        desc=None,
        tags=None,
        attrs=None,
        date_created=None,
        public_within_org=None,
        visibility=None,
    ):
        Message = _ProjectService.CreateProject
        msg = Message(
            name=name,
            description=desc,
            tags=tags,
            attributes=attrs,
            workspace_name=ctx.workspace_name,
        )
        if (
            public_within_org
            and ctx.workspace_name is not None  # not user's personal workspace
            and _utils.is_org(ctx.workspace_name, conn)
        ):  # not anyone's personal workspace
            msg.project_visibility = _ProjectService.ORG_SCOPED_PUBLIC
        msg.custom_permission.CopyFrom(visibility._custom_permission)
        msg.visibility = visibility._visibility

        response = conn.make_proto_request(
            "POST", "/api/v1/modeldb/project/createProject", body=msg
        )
        proj = conn.must_proto_response(response, Message.Response).project

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print("created new Project: {} in {}".format(proj.name, WORKSPACE_PRINT_MSG))
        return proj

    def _validate_email(self, email):
        if not isinstance(email, six.string_types):
            error_message_email_type = "email should be string"
            raise TypeError(error_message_email_type)
        if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
            error_message_email_value = "email is not valid"
            raise ValueError(error_message_email_value)

    def _validate_username(self, username):
        if not isinstance(username, six.string_types):
            error_message_username_type = "username should be string"
            raise TypeError(error_message_username_type)

    def delete(self):
        Message = _ProjectService.DeleteProject
        msg = Message(id=self.id)
        response = self._conn.make_proto_request(
            "DELETE", "/api/v1/modeldb/project/deleteProject", body=msg
        )
        self._conn.must_response(response)
