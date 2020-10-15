# -*- coding: utf-8 -*-

from __future__ import print_function

import re

import requests
import warnings

from .entity import _ModelDBEntity, _OSS_DEFAULT_WORKSPACE
from .experimentruns import ExperimentRuns
from .experiments import Experiments

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import ProjectService_pb2 as _ProjectService

from ..external import six

from .._internal_utils import (
    _utils,
)

from .._protos.public.uac import Collaborator_pb2 as _Collaborator
from .._protos.public.common import CommonService_pb2 as _CommonCommonService

class Project(_ModelDBEntity):
    """
    Object representing a machine learning Project.

    This class provides read/write functionality for Project metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_project() <verta.client.Client.set_project>`.

    Attributes
    ----------
    id : str
        ID of this Project.
    name : str
        Name of this Project.
    expt_runs : :class:`~verta._tracking.ExperimentRuns`
        Experiment Runs under this Project.

    """
    def __init__(self, conn, conf, msg):
        super(Project, self).__init__(conn, conf, _ProjectService, "project", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return '\n'.join((
            "name: {}".format(msg.name),
            "url: {}://{}/{}/projects/{}/summary".format(self._conn.scheme, self._conn.socket, self.workspace, self.id),
        ))

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
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._get_workspace_name_by_id(self._msg.workspace_id)
        else:
            return _OSS_DEFAULT_WORKSPACE

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
        if response.HasField("project_by_user") and response.project_by_user.id:
            return response.project_by_user
        elif hasattr(response, "shared_projects") and response.shared_projects:
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

    def _add_collaborator(self, email=None, username=None, collaborator_type=None, can_deploy=False, authz_entity_type=None):
        if not email and not username:
            error_message = "`email` or `username` must be provided"
            raise ValueError(error_message)

        if email:
            self._validate_email(email)
            share_with = email
        else:
            self._validate_username(username)
            share_with = username

        try:
            if collaborator_type:
                collaborator_type_value = _CommonCommonService.CollaboratorTypeEnum.CollaboratorType.Value(collaborator_type)
            else:
                collaborator_type_value = _CommonCommonService.CollaboratorTypeEnum.CollaboratorType.READ_ONLY
        except ValueError:
            unknown_value_error = "Unknown value {} specified for collaborator_type. Possible values are READ_ONLY, " \
                                  "READ_WRITE. "
            raise ValueError(unknown_value_error.format(collaborator_type))
        if can_deploy:
            can_deploy_value = _CommonCommonService.TernaryEnum.Ternary.TRUE
        else:
            can_deploy_value = _CommonCommonService.TernaryEnum.Ternary.FALSE
        try:
            if authz_entity_type:
                authz_entity_type_value = _CommonCommonService.EntitiesEnum.EntitiesTypes.Value(authz_entity_type)
            else:
                authz_entity_type_value = _CommonCommonService.EntitiesEnum.EntitiesTypes.USER
        except ValueError:
            unknown_value_error = "Unknown value {} specified for authz_entity_type. Possible values are USER, " \
                                  "ORGANIZATION, TEAM. "
            raise ValueError(unknown_value_error.format(authz_entity_type))

        Message = _Collaborator.AddCollaboratorRequest

        msg = Message(entity_ids=[self.id], share_with=share_with, collaborator_type=collaborator_type_value,
                      can_deploy=can_deploy_value, authz_entity_type=authz_entity_type_value)

        response = self._conn.make_proto_request("POST",
                                           "/api/v1/uac-proxy/collaborator/addOrUpdateProjectCollaborator",
                                           body=msg)
        # no need to return anything
        self._conn.must_proto_response(response, Message.Response)

    def _validate_email(self, email):
        if not isinstance(email, six.string_types):
            error_message_email_type = "email should be string"
            raise TypeError(error_message_email_type)
        if not re.match(r'[^@]+@[^@]+\.[^@]+', email):
            error_message_email_value = "email is not valid"
            raise ValueError(error_message_email_value)

    def _validate_username(self, username):
        if not isinstance(username, six.string_types):
            error_message_username_type = "username should be string"
            raise TypeError(error_message_username_type)

    def delete(self):
        Message = _ProjectService.DeleteProject
        msg = Message(id=self.id)
        response = self._conn.make_proto_request("DELETE",
                                           "/api/v1/modeldb/project/deleteProject",
                                           body=msg)
        self._conn.must_response(response)
