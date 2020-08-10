# -*- coding: utf-8 -*-

from __future__ import print_function

from .._internal_utils._utils import NoneProtoResponse
from .._tracking.entity import _ModelDBEntity, _OSS_DEFAULT_WORKSPACE
from .._tracking.context import _Context
from .._internal_utils import _utils

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.registry import RegistryService_pb2 as _RegisteredModelService

from .modelversion import RegisteredModelVersion
from .modelversions import RegisteredModelVersions


class RegisteredModel(_ModelDBEntity):
    """
    Object representing a registered model.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_registered_model() <verta.client.Client.set_registered_model>`

    Attributes
    ----------
    id : int
        ID of this Registered Model.
    name : str
        Name of this Registered Model.
    versions : iterable of :class:`~verta._registry.modelversion.RegisteredModelVersion`
        Versions of this RegisteredModel.

    """
    def __init__(self, conn, conf, msg):
        super(RegisteredModel, self).__init__(conn, conf, _RegisteredModelService, "registered_model", msg)

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return '\n'.join((
            "name: {}".format(msg.name),
            "url: {}://{}/{}/registry/{}".format(self._conn.scheme, self._conn.socket, self.workspace, self.id),
            "time created: {}".format(_utils.timestamp_to_str(int(msg.time_created))),
            "time updated: {}".format(_utils.timestamp_to_str(int(msg.time_updated))),
            "description: {}".format(msg.description),
            "labels: {}".format(msg.labels),
            "id: {}".format(msg.id),
        ))

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name

    @property
    def workspace(self):
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._get_workspace_name_by_id(self._msg.workspace_id)
        else:
            return _OSS_DEFAULT_WORKSPACE

    def get_or_create_version(self, name=None, desc=None, labels=None, id=None, time_created=None):
        """
        Gets or creates a Model Version.

        If an accessible Model Version with name `name` does not already exist under this
        Registered Model, it will be created and initialized with specified metadata
        parameters. If such a Model Version does already exist, it will be retrieved.

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Model Version.
        labels : list of str, optional
            Labels of the Model Version.
        id : str, optional
            ID of the Model Version. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`RegisteredModelVersion`

        Raises
        ------
        ValueError
            If `name` and `id` are both passed in.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        if id is not None:
            return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)
        else:
            ctx = _Context(self._conn, self._conf)
            ctx.registered_model = self
            return RegisteredModelVersion._get_or_create_by_name(self._conn, name,
                                                       lambda name: RegisteredModelVersion._get_by_name(self._conn, self._conf, name, self.id),
                                                       lambda name: RegisteredModelVersion._create(self._conn, self._conf, ctx, name=name, desc=desc, tags=labels, date_created=time_created))

    def create_version(self, name=None, desc=None, labels=None, time_created=None):
        """
        Creates a model registry entry.

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Model Version.
        labels : list of str, optional
            Labels of the Model Version.

        Returns
        -------
        :class:`~verta._registry.modelversion.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(self._conn, self._conf, ctx, name=name, desc=desc, tags=labels, date_created=time_created)


    def create_version_from_run(self, run_id, name=None):
        """
        Creates a model registry entry based on an Experiment Run.

        Parameters
        ----------
        run_id : str
        name : str, optional

        Returns
        -------
        :class:`~verta._registry.modelversion.RegisteredModelVersion`

        """
        ctx = _Context(self._conn, self._conf)
        ctx.registered_model = self
        return RegisteredModelVersion._create(self._conn, self._conf, ctx, name=name, experiment_run_id=run_id)

    def get_version(self, name=None, id=None):
        """
        Gets a Model Version of this Registered Model by `name` or `id`

        Parameters
        ----------
        name : str, optional
            Name of the Model Version. If no name is provided, one will be generated.
        id : str, optional
            ID of the Model Version. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta._registry.modelversion.RegisteredModelVersion`

        """
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

    def set_description(self, desc):
        if not desc:
            raise ValueError("desc is not specified")
        self._fetch_with_no_cache()
        self._msg.description = desc
        self._update()

    def get_description(self):
        self._refresh_cache()
        return self._msg.description

    def add_labels(self, labels):
        """
        Adds multiple labels to this Registered Model.

        Parameters
        ----------
        labels : list of str
            Labels to add.

        """
        if not labels:
            raise ValueError("label is not specified")

        self._fetch_with_no_cache()
        for label in labels:
            if label not in self._msg.labels:
                self._msg.labels.append(label)
        self._update()

    def add_label(self, label):
        """
        Adds a label to this Registered Model.

        Parameters
        ----------
        label : str
            Label to add.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._fetch_with_no_cache()
        if label not in self._msg.labels:
            self._msg.labels.append(label)
            self._update()

    def del_label(self, label):
        """
        Deletes a label from this Registered Model.

        Parameters
        ----------
        label : str
            Label to delete.

        """
        if label is None:
            raise ValueError("label is not specified")
        self._fetch_with_no_cache()
        if label in self._msg.labels:
            self._msg.labels.remove(label)
            self._update()

    def get_labels(self):
        """
        Gets all labels of this Registered Model.

        Returns
        -------
        labels : list of str
            List of all labels of this Registered Model.

        """
        self._refresh_cache()
        return self._msg.labels

    def _update(self):
        response = self._conn.make_proto_request("PUT", "/api/v1/registry/registered_models/{}".format(self.id),
                                           body=self._msg)
        Message = _RegisteredModelService.SetRegisteredModel
        if isinstance(self._conn.maybe_proto_response(response, Message.Response), NoneProtoResponse):
            raise ValueError("Model not found")
        self._clear_cache()

    def _get_info_list(self):
        return [self._msg.name, str(self.id), _utils.timestamp_to_str(self._msg.time_updated)]
