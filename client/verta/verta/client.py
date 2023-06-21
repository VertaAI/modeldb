# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import re
from urllib.parse import urlparse
from typing import Any, Dict, List
import warnings

import requests
from ._internal_utils._utils import check_unnecessary_params_warning
from ._internal_utils import kafka
from ._uac._organization import OrganizationV2

from ._vendored import six

from ._internal_utils import (
    _config_utils,
    _request_utils,
    _utils,
)

from verta import credentials
from verta.credentials import EmailCredentials, JWTCredentials

from .tracking import _Context
from .tracking.entities import (
    Project,
    Projects,
    Experiment,
    Experiments,
    ExperimentRun,
    ExperimentRuns,
)

from .registry.entities import (
    RegisteredModel,
    RegisteredModels,
    RegisteredModelVersion,
    RegisteredModelVersions,
)
from .dataset.entities import (
    Dataset,
    Datasets,
    DatasetVersion,
)
from .endpoint import Endpoint
from .endpoint import Endpoints
from .endpoint.update import DirectUpdateStrategy
from .visibility import _visibility
from ._protos.public.uac import WorkspaceV2_pb2
from ._uac._workspace import Workspace


VERTA_DISABLE_CLIENT_CONFIG_ENV_VAR = "VERTA_DISABLE_CLIENT_CONFIG"


class Client(object):
    """
    Object for interfacing with the Verta backend.

    .. deprecated:: 0.12.0
       The `port` parameter will be removed in an upcoming version; please combine `port` with the first parameter,
       e.g. `Client("localhost:8080")`.
    .. deprecated:: 0.13.3
       The `expt_runs` attribute will be removed in an upcoming version; consider using `proj.expt_runs` and
       `expt.expt_runs` instead.
    .. versionadded:: 0.20.4
       The ``VERTA_DISABLE_CLIENT_CONFIG`` environment variable, when set to
       a non-empty value, disables discovery of client config files for use in
       protected filesystems.

    This class provides functionality for starting/resuming Projects, Experiments, and Experiment Runs.

    Parameters
    ----------
    host : str, optional
        Hostname of the Verta Web App.
    email : str, optional
        Authentication credentials for managed service. If this does not sound familiar, then there
        is no need to set it.
    dev_key : str, optional
        Authentication credentials for managed service. If this does not sound familiar, then there
        is no need to set it.
    max_retries : int, default 5
        Maximum number of times to retry a request on a connection failure. This only attempts retries
        on HTTP codes {502, 503, 504} which commonly occur during back end connection lapses.
    ignore_conn_err : bool, default False
        Whether to ignore connection errors and instead return successes with empty contents.
    use_git : bool, default True
        Whether to use a local Git repository for certain operations such as Code Versioning.
    debug : bool, default False
        Whether to print extra verbose information to aid in debugging.
    extra_auth_headers : dict, default {}
        Extra headers to include on requests, like to permit traffic through a restrictive application load balancer
    organization_id : str, optional
        (alpha) Organization to use for the client calls. If not provided, the default organization will be used.
    _connect : str, default True
        Whether to connect to server (``False`` for unit tests).

    Attributes
    ----------
    max_retries : int
        Maximum number of times to retry a request on a connection failure. Changes to this value
        propagate to any objects that are/were created from this client.
    ignore_conn_err : bool
        Whether to ignore connection errors and instead return successes with empty contents. Changes
        to this value propagate to any objects that are/were created from this client.
    debug : bool
        Whether to print extra verbose information to aid in debugging. Changes to this value propagate
        to any objects that are/were created from this client.
    proj : :class:`~verta.tracking.entities.Project` or None
        Currently active project.
    projects : :class:`~verta.tracking.entities.Projects`
        Projects in the current default workspace.
    expt : :class:`~verta.tracking.entities.Experiment` or None
        Currently active experiment.
    experiments : :class:`~verta.tracking.entities.Experiments`
        Experiments in the current default workspace.
    expt_runs : :class:`~verta.tracking.entities.ExperimentRuns`
        Experiment runs in the current default workspace.
    registered_models : :class:`~verta.registry.entities.RegisteredModels`
        Registered models in the current default workspace.
    registered_model_versions : :class:`~verta.registry.entities.RegisteredModelVersions`
        Registered model versions in the current default workspace.
    endpoints : :class:`~verta.endpoint.Endpoints`
        Endpoints in the current default workspace.
    datasets : :class:`~verta.dataset.entities.Datasets`
        Datasets in the current default workspace.

    """

    def __init__(
        self,
        host=None,
        port=None,
        email=None,
        dev_key=None,
        max_retries=5,
        ignore_conn_err=False,
        use_git=True,
        debug=False,
        extra_auth_headers={},
        jwt_token=None,
        jwt_token_sig=None,
        organization_id=None,
        _connect=True,
    ):
        self._load_config()

        host = self._get_with_fallback(host, env_var="VERTA_HOST", config_var="host")
        if host is None:
            raise ValueError("`host` must be provided")

        email = self._get_with_fallback(
            email, env_var=EmailCredentials.EMAIL_ENV, config_var="email"
        )
        dev_key = self._get_with_fallback(
            dev_key, env_var=EmailCredentials.DEV_KEY_ENV, config_var="dev_key"
        )
        jwt_token = self._get_with_fallback(
            jwt_token, env_var=JWTCredentials.JWT_TOKEN_ENV, config_var="jwt_token"
        )
        jwt_token_sig = self._get_with_fallback(
            jwt_token_sig,
            env_var=JWTCredentials.JWT_TOKEN_SIG_ENV,
            config_var="jwt_token_sig",
        )

        self.auth_credentials = credentials._build(
            email=email,
            dev_key=dev_key,
            jwt_token=jwt_token,
            jwt_token_sig=jwt_token_sig,
            organization_id=organization_id,  # TODO: add organization_name as parameter and resolve that
        )
        self._workspace = self._get_with_fallback(None, env_var="VERTA_WORKSPACE")

        if self.auth_credentials is None:
            if debug:
                print("[DEBUG] credentials not found; auth disabled")
        else:
            if debug:
                print(
                    "[DEBUG] using credentials: {}".format(repr(self.auth_credentials))
                )
            # save credentials to env for other Verta Client features
            self.auth_credentials.export_env_vars_to_os()

        # TODO: Perhaps these things should move into Connection as well?
        back_end_url = urlparse(host)
        socket = back_end_url.netloc + back_end_url.path.rstrip("/")
        if port is not None:
            warnings.warn(
                "`port` (the second parameter) will be removed in a later version;"
                ' please combine it with the first parameter, e.g. "localhost:8080"',
                category=FutureWarning,
            )
            socket = "{}:{}".format(socket, port)
        scheme = back_end_url.scheme or ("https" if ".verta.ai" in socket else "http")

        conn = _utils.Connection(
            scheme=scheme,
            socket=socket,
            max_retries=max_retries,
            ignore_conn_err=ignore_conn_err,
            credentials=self.auth_credentials,
            headers=extra_auth_headers,
        )

        # verify connection
        if _connect:
            conn.test()

        self._conn = conn
        self._conf = _utils.Configuration(use_git, debug)
        self._ctx = _Context(self._conn, self._conf)

    @property
    def proj(self):
        return self._ctx.proj

    @property
    def expt(self):
        return self._ctx.expt

    @property
    def max_retries(self):
        return self._conn.retry.total

    @max_retries.setter
    def max_retries(self, value):
        self._conn.retry.total = value

    @property
    def ignore_conn_err(self):
        return self._conn.ignore_conn_err

    @ignore_conn_err.setter
    def ignore_conn_err(self, value):
        self._conn.ignore_conn_err = value

    @property
    def use_git(self):
        return self._conf.use_git

    @use_git.setter
    def use_git(self, _):
        """This would mess with state in safe but unexpected ways."""
        raise AttributeError("cannot set `use_git` after Client has been initialized")

    @property
    def debug(self):
        return self._conf.debug

    @debug.setter
    def debug(self, value):
        self._conf.debug = value

    @property
    def projects(self):
        return Projects(self._conn, self._conf).with_workspace(self.get_workspace())

    @property
    def experiments(self):
        return Experiments(self._conn, self._conf).with_workspace(self.get_workspace())

    @property
    def expt_runs(self):
        return ExperimentRuns(self._conn, self._conf).with_workspace(
            self.get_workspace()
        )

    def _load_config(self):
        if (
            VERTA_DISABLE_CLIENT_CONFIG_ENV_VAR in os.environ
            and os.environ[VERTA_DISABLE_CLIENT_CONFIG_ENV_VAR] != ""
        ):
            self._config = dict()
        else:
            with _config_utils.read_merged_config() as config:
                self._config = config

    def _set_from_config_if_none(self, var, resource_name):
        if var is None:
            var = self._config.get(resource_name)
            if var:
                print("setting {} from config file".format(resource_name))
        return var or None

    def _get_with_fallback(self, parameter, env_var=None, config_var=None):
        if parameter:
            return parameter
        if env_var:
            from_env = os.environ.get(env_var)
            if from_env:
                print("got {} from environment".format(env_var))
                return from_env
        if config_var:
            from_config = self._config.get(config_var)
            if from_config:
                print("got {} from config file".format(config_var))
                return from_config
        return None

    @staticmethod
    def _validate_visibility(visibility):
        """
        Validates the value of `visibility`.

        Parameters
        ----------
        visibility : :mod:`~verta.visibility` or None

        """
        # TODO: consider a decorator for create_*()s that validates common params
        if visibility is not None and not isinstance(
            visibility, _visibility._Visibility
        ):
            raise TypeError(
                "`visibility` must be an object from `verta.visibility`,"
                " not {}".format(type(visibility))
            )

        return visibility

    def get_workspace(self):
        """
        Gets the active workspace for this client instance.

        .. versionadded:: 0.17.0

        The active workspace is determined by this order of precedence:

        1) value set in :meth:`~Client.set_workspace`
        2) value set in client config file
        3) default workspace set in web app

        Returns
        -------
        workspace : str
            Verta workspace.

        """
        workspace = self._workspace

        if not workspace:
            workspace = self._config.get("workspace")

        if not workspace:
            workspace = self._conn.get_default_workspace()

        return workspace

    def set_workspace(self, workspace):
        """
        Sets the active workspace for this client instance.

        .. versionadded:: 0.17.0

        Parameters
        ----------
        workspace : str
            Verta workspace.

        """
        if not isinstance(workspace, six.string_types):
            raise TypeError("`workspace` must be a string")
        if not self._conn.is_workspace(workspace):
            raise ValueError('workspace "{}" not found'.format(workspace))

        self._workspace = workspace

    def get_project(self, name=None, workspace=None, id=None):
        """
        Retrieves an already created Project. Only one of `name` or `id` can be provided.

        Parameters
        ----------
        name : str, optional
            Name of the Project.
        workspace : str, optional
            Workspace under which the Project with name `name` exists. If not provided, the current
            user's default workspace will be used.
        id : str, optional
            ID of the Project. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`~verta.tracking.entities.Project`

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "project")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")
        if workspace is None:
            workspace = self.get_workspace()

        self._ctx = _Context(self._conn, self._conf)
        self._ctx.workspace_name = workspace

        if id is not None:
            self._ctx.proj = Project._get_by_id(self._conn, self._conf, id)
            self._ctx.populate()
        else:
            self._ctx.proj = Project._get_by_name(
                self._conn, self._conf, name, self._ctx.workspace_name
            )

        if self._ctx.proj is None:
            raise ValueError("Project not found")
        return self._ctx.proj

    def set_project(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
        id=None,
    ):
        """
        Attaches a Project to this Client.

        If an accessible Project with name `name` does not already exist, it will be created
        and initialized with specified metadata parameters. If such a Project does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise a warning.

        If an Experiment is already attached to this Client, it will be detached.

        Parameters
        ----------
        name : str, optional
            Name of the Project. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Project.
        tags : list of str, optional
            Tags of the Project.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Project.
        workspace : str, optional
            Workspace under which the Project with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a Project in an organization's workspace: ``True`` for
            public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this project. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.
        id : str, optional
            ID of the Project. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.tracking.entities.Project`

        Raises
        ------
        ValueError
            If a Project with `name` already exists, but metadata parameters are passed in.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "project")
        if workspace is None:
            workspace = self.get_workspace()

        self._ctx = _Context(self._conn, self._conf)
        self._ctx.workspace_name = workspace

        resource_name = "Project"
        param_names = "`desc`, `tags`, `attrs`, `public_within_org`, or `visibility`"
        params = (desc, tags, attrs, public_within_org, visibility)
        if id is not None:
            self._ctx.proj = Project._get_by_id(self._conn, self._conf, id)
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
            self._ctx.populate()
        else:
            self._ctx.proj = Project._get_or_create_by_name(
                self._conn,
                name,
                lambda name: Project._get_by_name(
                    self._conn, self._conf, name, self._ctx.workspace_name
                ),
                lambda name: Project._create(
                    self._conn,
                    self._conf,
                    self._ctx,
                    name=name,
                    desc=desc,
                    tags=tags,
                    attrs=attrs,
                    public_within_org=public_within_org,
                    visibility=visibility,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "name {}".format(name), param_names, params
                ),
            )

        return self._ctx.proj

    def get_experiment(self, name=None, id=None):
        """
        Retrieves an already created Experiment. Only one of `name` or `id` can be provided.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment.
        id : str, optional
            ID of the Experiment. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`~verta.tracking.entities.Experiment`

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "experiment")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")

        if id is not None:
            self._ctx.expt = Experiment._get_by_id(self._conn, self._conf, id)
            self._ctx.populate()
        else:
            if self._ctx.proj is None:
                self.set_project()

            self._ctx.expt = Experiment._get_by_name(
                self._conn, self._conf, name, self._ctx.proj.id
            )

        if self._ctx.expt is None:
            raise ValueError("Experment not found")
        return self._ctx.expt

    def set_experiment(self, name=None, desc=None, tags=None, attrs=None, id=None):
        """
        Attaches an Experiment under the currently active Project to this Client.

        If an accessible Experiment with name `name` does not already exist under the currently
        active Project, it will be created and initialized with specified metadata parameters. If
        such an Experiment does already exist, it will be retrieved; specifying metadata parameters
        in this case will raise a warning.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Experiment.
        tags : list of str, optional
            Tags of the Experiment.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Experiment.
        id : str, optional
            ID of the Experiment. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.tracking.entities.Experiment`

        Raises
        ------
        ValueError
            If an Experiment with `name` already exists, but metadata parameters are passed in.
        AttributeError
            If a Project is not yet in progress.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "experiment")

        resource_name = "Experiment"
        param_names = "`desc`, `tags`, or `attrs`"
        params = (desc, tags, attrs)
        if id is not None:
            self._ctx.expt = Experiment._get_by_id(self._conn, self._conf, id)
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
            self._ctx.populate()
        else:
            if self._ctx.proj is None:
                self.set_project()

            self._ctx.expt = Experiment._get_or_create_by_name(
                self._conn,
                name,
                lambda name: Experiment._get_by_name(
                    self._conn, self._conf, name, self._ctx.proj.id
                ),
                lambda name: Experiment._create(
                    self._conn,
                    self._conf,
                    self._ctx,
                    name=name,
                    desc=desc,
                    tags=tags,
                    attrs=attrs,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "name {}".format(name), param_names, params
                ),
            )

        return self._ctx.expt

    def get_experiment_run(self, name=None, id=None):
        """
        Retrieves an already created Experiment Run. Only one of `name` or `id` can be provided.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment Run.
        id : str, optional
            ID of the Experiment Run. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`~verta.tracking.entities.ExperimentRun`

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")

        if id is not None:
            self._ctx.expt_run = ExperimentRun._get_by_id(self._conn, self._conf, id)
            self._ctx.populate()
        else:
            if self._ctx.expt is None:
                self.set_experiment()

            self._ctx.expt_run = ExperimentRun._get_by_name(
                self._conn, self._conf, name, self._ctx.expt.id
            )

        if self._ctx.expt_run is None:
            raise ValueError("ExperimentRun not Found")
        return self._ctx.expt_run

    def set_experiment_run(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        id=None,
        date_created=None,
        start_time=None,
        end_time=None,
    ):
        """
        Attaches an Experiment Run under the currently active Experiment to this Client.

        If an accessible Experiment Run with name `name` does not already exist under the
        currently active Experiment, it will be created and initialized with specified metadata
        parameters. If such a Experiment Run does already exist, it will be retrieved; specifying
        metadata parameters in this case will raise a warning.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment Run. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Experiment Run.
        tags : list of str, optional
            Tags of the Experiment Run.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Experiment Run.
        id : str, optional
            ID of the Experiment Run. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.tracking.entities.ExperimentRun`

        Raises
        ------
        ValueError
            If an Experiment Run with `name` already exists, but metadata parameters are passed in.
        AttributeError
            If an Experiment is not yet in progress.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        resource_name = "Experiment Run"
        param_names = "`desc`, `tags`, `attrs`, or `date_created`"
        params = (desc, tags, attrs, date_created)
        if id is not None:
            self._ctx.expt_run = ExperimentRun._get_by_id(self._conn, self._conf, id)
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
            self._ctx.populate()
        else:
            if self._ctx.expt is None:
                self.set_experiment()

            self._ctx.expt_run = ExperimentRun._get_or_create_by_name(
                self._conn,
                name,
                lambda name: ExperimentRun._get_by_name(
                    self._conn, self._conf, name, self._ctx.expt.id
                ),
                lambda name: ExperimentRun._create(
                    self._conn,
                    self._conf,
                    self._ctx,
                    name=name,
                    desc=desc,
                    tags=tags,
                    attrs=attrs,
                    date_created=date_created,
                    start_time=start_time,
                    end_time=end_time,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "name {}".format(name), param_names, params
                ),
            )

        return self._ctx.expt_run

    # set aliases for get-or-create functions for API compatibility
    def get_or_create_project(self, *args, **kwargs):
        """
        Alias for :meth:`Client.set_project()`.

        """
        return self.set_project(*args, **kwargs)

    def get_or_create_experiment(self, *args, **kwargs):
        """
        Alias for :meth:`Client.set_experiment()`.

        """
        return self.set_experiment(*args, **kwargs)

    def get_or_create_experiment_run(self, *args, **kwargs):
        """
        Alias for :meth:`Client.set_experiment_run()`.

        """
        return self.set_experiment_run(*args, **kwargs)

    def get_or_create_registered_model(
        self,
        name=None,
        desc=None,
        labels=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
        id=None,
        task_type=None,
        data_type=None,
        pii=False,
    ):
        """
        Attaches a registered_model to this Client.

        If an accessible registered_model with name `name` does not already exist, it will be created
        and initialized with specified metadata parameters. If such a registered_model does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise a warning.

        Parameters
        ----------
        name : str, optional
            Name of the registered_model. If no name is provided, one will be generated.
        desc : str, optional
            Description of the registered_model.
        labels: list of str, optional
            Labels of the registered_model.
        workspace : str, optional
            Workspace under which the registered_model with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a registered_model in an organization's workspace:
            ``True`` for public, ``False`` for private. In older backends,
            default is private; in newer backends, uses the org's settings by
            default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this registered model. If not
            provided, an appropriate default will be used. This parameter
            should be preferred over `public_within_org`.
        id : str, optional
            ID of the registered_model. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.
        task_type : :mod:`~verta.registry.task_type`, optional
            Task type of the registered_model.
        data_type : :mod:`~verta.registry.data_type`, optional
            Data type of the registered_model.
        pii : bool, default False
            Whether the registered_model ingests personally identifiable information.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModel`

        Raises
        ------
        ValueError
            If a registered_model with `name` already exists, but metadata parameters are passed in.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "registered_model")
        if workspace is None:
            workspace = self.get_workspace()

        ctx = _Context(self._conn, self._conf)
        ctx.workspace_name = workspace

        resource_name = "Registered Model"
        param_names = "`desc`, `labels`, `public_within_org`, or `visibility`"
        params = (desc, labels, public_within_org, visibility)
        if id is not None:
            registered_model = RegisteredModel._get_by_id(self._conn, self._conf, id)
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
        else:
            registered_model = RegisteredModel._get_or_create_by_name(
                self._conn,
                name,
                lambda name: RegisteredModel._get_by_name(
                    self._conn, self._conf, name, ctx.workspace_name
                ),
                lambda name: RegisteredModel._create(
                    self._conn,
                    self._conf,
                    ctx,
                    name=name,
                    desc=desc,
                    tags=labels,
                    public_within_org=public_within_org,
                    visibility=visibility,
                    task_type=task_type,
                    data_type=data_type,
                    pii=pii,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "name {}".format(name), param_names, params
                ),
            )

        return registered_model

    def get_registered_model(self, name=None, workspace=None, id=None):
        """
        Retrieve an already created Registered Model. Only one of name or id can be provided.

        Parameters
        ----------
        name : str, optional
            Name of the Registered Model.
        id : str, optional
            ID of the Registered Model. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModel`
        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "registered_model")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")
        if workspace is None:
            workspace = self.get_workspace()

        if id is not None:
            registered_model = RegisteredModel._get_by_id(self._conn, self._conf, id)
        else:
            registered_model = RegisteredModel._get_by_name(
                self._conn, self._conf, name, workspace
            )

        if registered_model is None:
            raise ValueError("Registered model not found")

        return registered_model

    def set_registered_model(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_registered_model()`.

        """
        return self.get_or_create_registered_model(*args, **kwargs)

    def get_registered_model_version(self, id):
        """
        Retrieve an already created Model Version.

        Parameters
        ----------
        id : str
            ID of the Model Version.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModelVersion`
        """
        return RegisteredModelVersion._get_by_id(self._conn, self._conf, id)

    @property
    def registered_models(self):
        return RegisteredModels(self._conn, self._conf).with_workspace(
            self.get_workspace()
        )

    @property
    def registered_model_versions(self):
        return RegisteredModelVersions(self._conn, self._conf).with_workspace(
            self.get_workspace()
        )

    def get_or_create_endpoint(
        self,
        path=None,
        description=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
        id=None,
    ):
        """
        Attaches an endpoint to this Client.

        If an accessible endpoint with name `path` does not already exist, it will be created
        and initialized with specified metadata parameters. If such an endpoint does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise a warning.

        Parameters
        ----------
        path : str, optional
            Path for the endpoint.
        description : str, optional
            Description of the endpoint.
        workspace : str, optional
            Workspace under which the endpoint with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating an endpoint in an organization's workspace: ``True``
            for public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this endpoint. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.
        id : str, optional
            ID of the endpoint. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.endpoint.Endpoint`

        Raises
        ------
        ValueError
            If an endpoint with `path` already exists, but metadata parameters are passed in.

        """
        if path is not None and id is not None:
            raise ValueError("cannot specify both `path` and `id`")
        if path is None and id is None:
            raise ValueError("must specify either `path` or `id`")
        self._validate_visibility(visibility)

        if workspace is None:
            workspace = self.get_workspace()
        resource_name = "Endpoint"
        param_names = "`description`, `public_within_org`, or `visibility`"
        params = [description, public_within_org, visibility]
        if id is not None:
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
            return Endpoint._get_by_id(self._conn, self._conf, workspace, id)
        else:
            return Endpoint._get_or_create_by_name(
                self._conn,
                path,
                lambda path: Endpoint._get_by_path(
                    self._conn, self._conf, workspace, path
                ),
                lambda path: Endpoint._create(
                    self._conn,
                    self._conf,
                    workspace,
                    path,
                    description,
                    public_within_org,
                    visibility,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "path {}".format(path), param_names, params
                ),
            )

    def get_endpoint(self, path=None, workspace=None, id=None):
        """
        Retrieves an already created Endpoint. Only one of `path` or `id` can be provided.

        Parameters
        ----------
        path : str, optional
            Path of the Endpoint.
        workspace : str, optional
            Name of the workspace of the Endpoint.
        id : str, optional
            ID of the Endpoint. This parameter cannot be provided alongside `path`.

        Returns
        -------
        :class:`~verta.endpoint.Endpoint`

        """
        if path is not None and id is not None:
            raise ValueError("cannot specify both `path` and `id`")
        if path is None and id is None:
            raise ValueError("must specify either `path` or `id`")

        if workspace is None:
            workspace = self.get_workspace()

        if id is not None:
            endpoint = Endpoint._get_by_id(self._conn, self._conf, workspace, id)
        else:
            endpoint = Endpoint._get_by_path(self._conn, self._conf, workspace, path)

        if endpoint is None:
            raise ValueError("Endpoint not found")
        return endpoint

    def set_endpoint(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_endpoint()`.

        """
        return self.get_or_create_endpoint(*args, **kwargs)

    def create_project(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
    ):
        """
        Creates a new Project.

        A Project with name `name` will be created and initialized with specified metadata parameters.

        If an Experiment is already attached to this Client, it will be detached.

        Parameters
        ----------
        name : str, optional
            Name of the Project. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Project.
        tags : list of str, optional
            Tags of the Project.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Project.
        workspace : str, optional
            Workspace under which the Project with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a Project in an organization's workspace: ``True`` for
            public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this project. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.

        Returns
        -------
        :class:`~verta.tracking.entities.Project`

        Raises
        ------
        ValueError
            If a Project with `name` already exists.

        """
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "project")
        if workspace is None:
            workspace = self.get_workspace()

        self._ctx = _Context(self._conn, self._conf)
        self._ctx.workspace_name = workspace
        self._ctx.proj = Project._create(
            self._conn,
            self._conf,
            self._ctx,
            name=name,
            desc=desc,
            tags=tags,
            attrs=attrs,
            public_within_org=public_within_org,
            visibility=visibility,
        )
        return self._ctx.proj

    def create_experiment(self, name=None, desc=None, tags=None, attrs=None):
        """
        Creates a new Experiment under the currently active Project.

        Experiment with name `name` will be created and initialized with specified metadata parameters.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Experiment.
        tags : list of str, optional
            Tags of the Experiment.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Experiment.

        Returns
        -------
        :class:`~verta.tracking.entities.Experiment`

        Raises
        ------
        ValueError
            If an Experiment with `name` already exists.
        AttributeError
            If a Project is not yet in progress.

        """
        name = self._set_from_config_if_none(name, "experiment")

        if self._ctx.proj is None:
            self.set_project()

        self._ctx.expt = Experiment._create(
            self._conn,
            self._conf,
            self._ctx,
            name=name,
            desc=desc,
            tags=tags,
            attrs=attrs,
        )

        return self._ctx.expt

    def create_experiment_run(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        date_created=None,
        start_time=None,
        end_time=None,
    ):
        """
        Creates a new Experiment Run under the currently active Experiment.

        An Experiment Run with name `name` will be created and initialized with specified metadata
        parameters.

        Parameters
        ----------
        name : str, optional
            Name of the Experiment Run. If no name is provided, one will be generated.
        desc : str, optional
            Description of the Experiment Run.
        tags : list of str, optional
            Tags of the Experiment Run.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Experiment Run.

        Returns
        -------
        :class:`~verta.tracking.entities.ExperimentRun`

        Raises
        ------
        ValueError
            If an Experiment Run with `name` already exists.
        AttributeError
            If an Experiment is not yet in progress.

        """
        if self._ctx.expt is None:
            self.set_experiment()

        self._ctx.expt_run = ExperimentRun._create(
            self._conn,
            self._conf,
            self._ctx,
            name=name,
            desc=desc,
            tags=tags,
            attrs=attrs,
            date_created=date_created,
            start_time=start_time,
            end_time=end_time,
        )

        return self._ctx.expt_run

    def create_registered_model(
        self,
        name=None,
        desc=None,
        labels=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
        task_type=None,
        data_type=None,
        pii=False,
    ):
        """
        Creates a new Registered Model.

        A registered_model with name `name` does will be created and initialized with specified metadata parameters.

        Parameters
        ----------
        name : str, optional
            Name of the registered_model. If no name is provided, one will be generated.
        desc : str, optional
            Description of the registered_model.
        labels: list of str, optional
            Labels of the registered_model.
        workspace : str, optional
            Workspace under which the registered_model with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a registered_model in an organization's workspace:
            ``True`` for public, ``False`` for private. In older backends,
            default is private; in newer backends, uses the org's settings by
            default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this registered model. If not
            provided, an appropriate default will be used. This parameter
            should be preferred over `public_within_org`.
        task_type : :mod:`~verta.registry.task_type`, optional
            Task type of the registered_model.
        data_type : :mod:`~verta.registry.data_type`, optional
            Data type of the registered_model.
        pii : bool, default False
            Whether the registered_model ingests personally identifiable information.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModel`

        Raises
        ------
        ValueError
            If a registered_model with `name` already exists.

        """
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "registered_model")

        if workspace is None:
            workspace = self.get_workspace()

        ctx = _Context(self._conn, self._conf)
        ctx.workspace_name = workspace

        return RegisteredModel._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            desc=desc,
            tags=labels,
            public_within_org=public_within_org,
            visibility=visibility,
            task_type=task_type,
            data_type=data_type,
            pii=pii,
        )

    def create_endpoint(
        self,
        path,
        description=None,
        workspace=None,
        public_within_org=None,
        visibility=None,
        kafka_settings=None,
    ):
        """
        Attaches an endpoint to this Client.

        An accessible endpoint with name `name` will be created and initialized with specified metadata parameters.

        .. versionadded:: 0.19.0
            The `kafka_settings` parameter.

        Parameters
        ----------
        path : str
            Path for the endpoint.
        description : str, optional
            Description of the endpoint.
        workspace : str, optional
            Workspace under which the endpoint with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating an endpoint in an organization's workspace: ``True``
            for public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this endpoint. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.
        kafka_settings : :class:`verta.endpoint.KafkaSettings`, optional
            Kafka settings.

        Returns
        -------
        :class:`~verta.endpoint.Endpoint`

        Raises
        ------
        ValueError
            If an endpoint with `path` already exists.

        """
        if path is None:
            raise ValueError("Must specify `path`")
        self._validate_visibility(visibility)

        if workspace is None:
            workspace = self.get_workspace()
        return Endpoint._create(
            self._conn,
            self._conf,
            workspace,
            path,
            description,
            public_within_org,
            visibility,
            kafka_settings,
        )

    @property
    def endpoints(self):
        return Endpoints(self._conn, self._conf, self.get_workspace())

    def download_endpoint_manifest(
        self,
        download_to_path,
        path,
        name,
        strategy=None,
        resources=None,
        autoscaling=None,
        env_vars=None,
        workspace=None,
    ):
        """
        Downloads this endpoint's Kubernetes manifest YAML.

        Parameters
        ----------
        download_to_path : str
            Local path to download manifest YAML to.
        path : str
            Path of the endpoint.
        name : str
            Name of the endpoint.
        strategy : :mod:`~verta.endpoint.update`, default DirectUpdateStrategy()
            Strategy (direct or canary) for updating the endpoint.
        resources : :class:`~verta.endpoint.resources.Resources`, optional
            Resources allowed for the updated endpoint.
        autoscaling : :class:`~verta.endpoint.autoscaling.Autoscaling`, optional
            Autoscaling condition for the updated endpoint.
        env_vars : dict of str to str, optional
            Environment variables.
        workspace : str, optional
            Workspace for the endpoint. If not provided, the current user's
            default workspace will be used.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where deployment YAML was downloaded to. Matches `download_to_path`.

        """
        if not path.startswith("/"):
            path = "/" + path

        if not strategy:
            strategy = DirectUpdateStrategy()
        if not workspace:
            workspace = self.get_workspace()

        data = {
            "endpoint": {"path": path},
            "name": name,
            "update": Endpoint._create_update_body(
                strategy, resources, autoscaling, env_vars
            ),
            "workspace_name": workspace,
        }

        endpoint = "{}://{}/api/v1/deployment/operations/manifest".format(
            self._conn.scheme,
            self._conn.socket,
        )

        with _utils.make_request(
            "POST", endpoint, self._conn, json=data, stream=True
        ) as response:
            _utils.raise_for_http_error(response)

            downloaded_to_path = _request_utils.download_file(
                response, download_to_path, overwrite_ok=True
            )
            return os.path.abspath(downloaded_to_path)

    def get_or_create_dataset(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        workspace=None,
        time_created=None,
        public_within_org=None,
        visibility=None,
        id=None,
    ):
        """
        Gets or creates a dataset.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        If an accessible dataset with name `name` does not already exist, it will be created
        and initialized with specified metadata parameters. If such a dataset does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise a warning.

        Parameters
        ----------
        name : str, optional
            Name of the dataset. If no name is provided, one will be generated.
        desc : str, optional
            Description of the dataset.
        tags : list of str, optional
            Tags of the dataset.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the dataset.
        workspace : str, optional
            Workspace under which the dataset with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a dataset in an organization's workspace: ``True`` for
            public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this dataset. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.
        id : str, optional
            ID of the dataset. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`~verta.dataset.entities.Dataset`

        Raises
        ------
        ValueError
            If a dataset with `name` already exists, but metadata parameters are passed in.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "dataset")
        if workspace is None:
            workspace = self.get_workspace()

        ctx = _Context(self._conn, self._conf)
        ctx.workspace_name = workspace

        resource_name = "Dataset"
        param_names = "`desc`, `tags`, `attrs`, `time_created`, `public_within_org`, or `visibility`"
        params = (desc, tags, attrs, time_created, public_within_org, visibility)
        if id is not None:
            dataset = Dataset._get_by_id(self._conn, self._conf, id)
            check_unnecessary_params_warning(
                resource_name, "id {}".format(id), param_names, params
            )
        else:
            dataset = Dataset._get_or_create_by_name(
                self._conn,
                name,
                lambda name: Dataset._get_by_name(
                    self._conn, self._conf, name, ctx.workspace_name
                ),
                lambda name: Dataset._create(
                    self._conn,
                    self._conf,
                    ctx,
                    name=name,
                    desc=desc,
                    tags=tags,
                    attrs=attrs,
                    time_created=time_created,
                    public_within_org=public_within_org,
                    visibility=visibility,
                ),
                lambda: check_unnecessary_params_warning(
                    resource_name, "name {}".format(name), param_names, params
                ),
            )

        return dataset

    def set_dataset(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_dataset()`.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        """
        return self.get_or_create_dataset(*args, **kwargs)

    def create_dataset(
        self,
        name=None,
        desc=None,
        tags=None,
        attrs=None,
        workspace=None,
        time_created=None,
        public_within_org=None,
        visibility=None,
    ):
        """
        Creates a dataset, initialized with specified metadata parameters.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        Parameters
        ----------
        name : str, optional
            Name of the dataset. If no name is provided, one will be generated.
        desc : str, optional
            Description of the dataset.
        tags : list of str, optional
            Tags of the dataset.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the dataset.
        workspace : str, optional
            Workspace under which the dataset with name `name` exists. If not provided, the current
            user's default workspace will be used.
        public_within_org : bool, optional
            If creating a dataset in an organization's workspace: ``True`` for
            public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this dataset. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.

        Returns
        -------
        :class:`~verta.dataset.entities.Dataset`

        Raises
        ------
        ValueError
            If a dataset with `name` already exists.

        """
        self._validate_visibility(visibility)

        name = self._set_from_config_if_none(name, "dataset")
        if workspace is None:
            workspace = self.get_workspace()

        ctx = _Context(self._conn, self._conf)
        ctx.workspace_name = workspace
        return Dataset._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            desc=desc,
            tags=tags,
            attrs=attrs,
            time_created=time_created,
            public_within_org=public_within_org,
            visibility=visibility,
        )

    def get_dataset(self, name=None, workspace=None, id=None):
        """
        Gets a dataset.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        Parameters
        ----------
        name : str, optional
            Name of the dataset. This parameter cannot be provided alongside `id`.
        workspace : str, optional
            Workspace under which the dataset with name `name` exists. If not provided, the current
            user's default workspace will be used.
        id : str, optional
            ID of the dataset. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`~verta.dataset.entities.Dataset`

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "dataset")
        if name is None and id is None:
            raise ValueError("must specify either `name` or `id`")
        if workspace is None:
            workspace = self.get_workspace()

        if id is not None:
            dataset = Dataset._get_by_id(self._conn, self._conf, id)
        else:
            dataset = Dataset._get_by_name(self._conn, self._conf, name, workspace)

        if dataset is None:
            raise ValueError("Dataset not found")
        return dataset

    @property
    def datasets(self):
        return Datasets(self._conn, self._conf).with_workspace(self.get_workspace())

    def get_dataset_version(self, id):
        """
        Gets a dataset version.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        Parameters
        ----------
        id : str
            ID of the dataset version.

        Returns
        -------
        :class:`~verta.dataset.entities.DatasetVersion`

        """
        dataset_version = DatasetVersion._get_by_id(self._conn, self._conf, id)

        if dataset_version is None:
            raise ValueError("Dataset Version not found")
        return dataset_version

    # holdover for backwards compatibility
    def find_datasets(
        self,
        dataset_ids=None,
        name=None,
        tags=None,
        sort_key=None,
        ascending=False,
        workspace=None,
    ):
        warnings.warn(
            "this method is deprecated and will be removed in an upcoming version;"
            " consider using `client.datasets.find()` instead",
            category=FutureWarning,
        )
        datasets = self.datasets
        if dataset_ids:
            datasets = datasets.with_ids(_utils.as_list_of_str(dataset_ids))
        if sort_key:
            datasets = datasets.sort(sort_key, not ascending)
        if workspace:
            datasets = datasets.with_workspace(workspace)

        predicates = []
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
            predicates.extend('tags == "{}"'.format(tag) for tag in tags)
        if name is not None:
            if not isinstance(name, six.string_types):
                raise TypeError("`name` must be str, not {}".format(type(name)))
            predicates.append('name ~= "{}"'.format(name))
        if predicates:
            datasets = datasets.find(predicates)

        return datasets

    def _create_workspace(
        self,
        org_id,
        workspace_name,
        resource_action_groups,
        namespace="",
    ):
        """
        creates a workspace with a custom role for all users.

        Parameters
        ----------
        org_id : str
            ID of organization in which workspace is created.
        workspace_name : str
            name of workspace.
        resource_action_groups : list of RoleV2_pb2.RoleResourceActions
            Resource actions for non-admins in this workspace.
        namespace: str, optional
            namespace where workspace endpoints will be hosted.
            Must follow `cluster--namespace` format.

        Returns
        -------
        :class:`~verta._uac._workspace.Workspace`

        """
        org = OrganizationV2(self._conn, org_id)
        groups = org.get_groups()
        all_users_group_id = next(
            iter(set(group.id for group in groups if group.name == "All Users"))
        )
        admins_group_id = next(
            iter(set(group.id for group in groups if group.name == "Admins"))
        )
        super_user_role_id = next(
            iter(set(role.id for role in org.get_roles() if role.name == "Super User"))
        )
        custom_role_id: str = org.create_role(
            workspace_name + "role", resource_action_groups
        )
        return Workspace._create(
            self._conn,
            workspace_name,
            org_id,
            [
                WorkspaceV2_pb2.Permission(
                    group_id=admins_group_id, role_id=super_user_role_id
                ),
                WorkspaceV2_pb2.Permission(
                    group_id=all_users_group_id, role_id=custom_role_id
                ),
            ],
            namespace,
        )

    def get_kafka_topics(self) -> List[str]:
        """
        Get available topics for the current Kafka configuration, for associating
        with an endpoint via :class:`~verta.endpoint.KafkaSettings`.

        .. versionadded:: 0.23.0

        Returns
        -------
        topics: list of str
            List of topic names.

        Raises
        ------
        HTTPError
            If no valid Kafka configuration can be found.
        """
        kafka_configs: List[Dict[str, Any]] = kafka.list_kafka_configurations(
            self._conn
        )
        if kafka_configs:
            return kafka.list_kafka_topics(self._conn, kafka_configs[0])
        return []
