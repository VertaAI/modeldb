# -*- coding: utf-8 -*-

from __future__ import print_function

import requests
import warnings

from .entity import _ModelDBEntity
from .experimentruns import ExperimentRuns
from .experiments import Experiments
from .._registry import RegisteredModelVersion # TODO: update this later on

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.registry import RegistryService_pb2 as _RegisteredModelService

from ..external import six

from .._internal_utils import (
    _utils,
)


class RegisteredModel(_ModelDBEntity):
    """
    Object representing a machine learning RegisteredModel.

    This class provides read/write functionality for RegisteredModel metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_registered_model`.

    Attributes
    ----------
    id : str
        ID of this RegisteredModel.
    name : str
        Name of this RegisteredModel.
    expt_runs : :class:`ExperimentRuns`
        Experiment Runs under this RegisteredModel.

    """

    def __init__(self, conn, conf, msg):
        super(RegisteredModel, self).__init__(conn, conf, _RegisteredModelService, "registered_model", msg)

    def __repr__(self):
        return "<RegisteredModel \"{}\">".format(self.name)

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @classmethod
    def _generate_default_name(cls):
        return "RegisteredModel {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _RegisteredModelService.GetRegisteredModelRequest
        response = conn.make_proto_request("GET",
                                           "/api/v1/registry/{}".format(id))
        return conn.maybe_proto_response(response, Message.Response).registered_model
    
    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _RegisteredModelService.GetRegisteredModelRequest
        if name and workspace :
            response = conn.make_proto_request("GET",
                                               "/api/v1/registry/workspaces/{}/registered_models/{}".format(workspace, name))
        else:
            raise RuntimeError("the Client has encountered an error;"
                               " please notify the Verta development team: registered model name or workspace not specified")
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, labels=None, date_created=None, attrs=None, public_within_org=None):
        Message = _RegisteredModelService.RegisteredModel
        msg = Message(name=name, description=desc, labels=labels)
        if public_within_org:
            if ctx.workspace_name is None:
                raise ValueError("cannot set `public_within_org` for personal workspace")
            elif not _utils.is_org(ctx.workspace_name, conn):
                raise ValueError(
                    "cannot set `public_within_org`"
                    " because workspace \"{}\" is not an organization".format(ctx.workspace_name)
                )
            else:
                msg.visibility = _CommonCommonService.VisibilityEnum.ORG_SCOPED_PUBLIC

        response = conn.make_proto_request("POST",
                                           "/api/v1/registry/workspaces/{}/registered_models".format(ctx.workspace_name),
                                           body=msg)
        registered_model = conn.must_proto_response(response, _RegisteredModelService.SetRegisteredModel.Response).registered_model

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print("created new RegisteredModel: {} in {}".format(registered_model.name, WORKSPACE_PRINT_MSG))
        return registered_model

    def get_or_create_version(self, name=None, desc=None, tags=None, attrs=None, id=None, time_created=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            return RegisteredModelVersion._get_or_create_by_name(self._conn, name,
                                                       lambda name: RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id),
                                                       lambda name: RegisteredModelVersion._create(self._conn, self._conf, None, name, desc=desc, tags=None, attrs=None, date_created=time_created, registered_model_id=self.id))

    def get_version(self, name=None, id=None):
        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:

            return RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id)
