# -*- coding: utf-8 -*-

from __future__ import print_function

from .._internal_utils._utils import NoneProtoResponse
from .._tracking.entity import _ModelDBEntity
from .._tracking.context import _Context
from .._internal_utils import _utils

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.registry import RegistryService_pb2 as _RegisteredModelService

from .modelversion import RegisteredModelVersion
from .modelversions import RegisteredModelVersions


class RegisteredModel(_ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(RegisteredModel, self).__init__(conn, conf, _RegisteredModelService, "registered_model", msg)

    def __repr__(self):
        return "<Model \"{}\">".format(self.name)

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    def get_or_create_version(self, name=None, desc=None, labels=None, id=None, time_created=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            ctx = _Context(self._conn, self._conf)
            ctx.registered_model = self
            return RegisteredModelVersion._get_or_create_by_name(self._conn, name,
                                                       lambda name: RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id),
                                                       lambda name: RegisteredModelVersion._create(self._conn, self._conf, ctx, name, desc=desc, tags=labels, date_created=time_created))

    def create_version_from_run(self, run_id, name):
        """
        Creates a model registry entry based on an Experiment Run.

        Parameters
        ----------
        run_id : str
        name : str

        Returns
        -------
        :class:`~verta._registry.modelversion.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(self._conn, self._conf, ctx, name, experiment_run_id=run_id)

    def get_version(self, name=None, id=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")

        if id is not None:
            version = RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            version = RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id)
        if version is None:
            raise ValueError("Registered model version not found")
        return version

    @property
    def versions(self):
        return RegisteredModelVersions(self._conn, self._conf).with_model(self)

    @classmethod
    def _generate_default_name(cls):
        return "Model {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _RegisteredModelService.GetRegisteredModelRequest
        response = conn.make_proto_request("GET",
                                           "/api/v1/registry/registered_models/{}".format(id))
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        Message = _RegisteredModelService.GetRegisteredModelRequest
        response = conn.make_proto_request("GET",
                                           "/api/v1/registry/workspaces/{}/registered_models/{}".format(workspace, name))
        return conn.maybe_proto_response(response, Message.Response).registered_model

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, public_within_org=None):
        Message = _RegisteredModelService.RegisteredModel
        msg = Message(name=name, description=desc, labels=tags, time_created=date_created, time_updated=date_created)
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

    def add_labels(self, labels):
        if not labels:
            raise ValueError("label is not specified")

        self._fetch_with_no_cache()
        for label in labels:
            if label not in self._msg.labels:
                self._msg.labels.append(label)
        self._update()

    def add_label(self, label):
        if label is None:
            raise ValueError("label is not specified")
        self._clear_cache()
        self._refresh_cache()
        if label not in self._msg.labels:
            self._msg.labels.append(label)
            self._update()

    def del_label(self, label):
        if label is None:
            raise ValueError("label is not specified")
        self._clear_cache()
        self._refresh_cache()
        if label in self._msg.labels:
            self._msg.labels.remove(label)
            self._update()

    def get_labels(self):
        self._clear_cache()
        self._refresh_cache()
        return self._msg.labels

    def _update(self):
        response = self._conn.make_proto_request("PUT", "/api/v1/registry/registered_models/{}".format(self.id),
                                           body=self._msg)
        Message = _RegisteredModelService.SetRegisteredModel
        if isinstance(self._conn.maybe_proto_response(response, Message.Response), NoneProtoResponse):
            raise ValueError("Model not found")
