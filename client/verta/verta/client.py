# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import re
import warnings

import requests
from verta.tracking._organization import Organization
from ._internal_utils._utils import check_unnecessary_params_warning

from ._protos.public.modeldb import CommonService_pb2 as _CommonService

from .external import six
from .external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from ._internal_utils import (
    _config_utils,
    _request_utils,
    _utils,
)

from . import repository

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
from .monitoring.client import Client as MonitoringClient

class Client(object):
    """
    Object for interfacing with the Verta backend.

    .. deprecated:: 0.12.0
       The `port` parameter will be removed in an upcoming version; please combine `port` with the first parameter,
       e.g. `Client("localhost:8080")`.
    .. deprecated:: 0.13.3
       The `expt_runs` attribute will be removed in an upcoming version; consider using `proj.expt_runs` and
       `expt.expt_runs` instead.

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
    monitoring : :class:`verta.monitoring.client.Client`
        Monitoring sub-client
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
    def __init__(self, host=None, port=None, email=None, dev_key=None,
                 max_retries=5, ignore_conn_err=False, use_git=True, debug=False, _connect=True):
        self._load_config()

        if host is None and 'VERTA_HOST' in os.environ:
            host = os.environ['VERTA_HOST']
            print("set host from environment")
        host = self._set_from_config_if_none(host, "host")
        if email is None and 'VERTA_EMAIL' in os.environ:
            email = os.environ['VERTA_EMAIL']
            print("set email from environment")
        email = self._set_from_config_if_none(email, "email")
        if dev_key is None and 'VERTA_DEV_KEY' in os.environ:
            dev_key = os.environ['VERTA_DEV_KEY']
            print("set developer key from environment")
        dev_key = self._set_from_config_if_none(dev_key, "dev_key")
        self._workspace = os.environ.get('VERTA_WORKSPACE')
        if self._workspace is not None:
            print("set workspace from environment")

        if host is None:
            raise ValueError("`host` must be provided")
        auth = {_utils._GRPC_PREFIX+'source': "PythonClient"}
        if email is None and dev_key is None:
            if debug:
                print("[DEBUG] email and developer key not found; auth disabled")
        elif email is not None and dev_key is not None:
            if debug:
                print("[DEBUG] using email: {}".format(email))
                print("[DEBUG] using developer key: {}".format(dev_key[:8] + re.sub(r"[^-]", '*', dev_key[8:])))
            auth.update({
                _utils._GRPC_PREFIX+'email': email,
                _utils._GRPC_PREFIX+'developer_key': dev_key,
                # without underscore, for NGINX support
                # https://www.nginx.com/resources/wiki/start/topics/tutorials/config_pitfalls#missing-disappearing-http-headers
                _utils._GRPC_PREFIX+'developer-key': dev_key,
            })
            # save credentials to env for other Verta Client features
            os.environ['VERTA_EMAIL'] = email
            os.environ['VERTA_DEV_KEY'] = dev_key
        else:
            raise ValueError("`email` and `dev_key` must be provided together")

        back_end_url = urlparse(host)
        socket = back_end_url.netloc + back_end_url.path.rstrip('/')
        if port is not None:
            warnings.warn("`port` (the second parameter) will be removed in a later version;"
                          " please combine it with the first parameter, e.g. \"localhost:8080\"",
                          category=FutureWarning)
            socket = "{}:{}".format(socket, port)
        scheme = back_end_url.scheme or ("https" if ".verta.ai" in socket else "http")
        auth[_utils._GRPC_PREFIX+'scheme'] = scheme

        # verify connection
        conn = _utils.Connection(scheme, socket, auth, max_retries, ignore_conn_err)
        if _connect:
            try:
                response = _utils.make_request("GET",
                                               "{}://{}/api/v1/modeldb/project/verifyConnection".format(conn.scheme, conn.socket),
                                               conn)
            except requests.ConnectionError as err:
                err.args = ("connection failed; please check `host` and `port`; error message: \n\n{}".format(err.args[0]),) + err.args[1:]
                six.raise_from(err, None)

            def is_unauthorized(response): return response.status_code == 401

            if is_unauthorized(response):
                # response.reason was "Unauthorized"
                try:
                    response.raise_for_status()
                except requests.HTTPError as e:
                    e.args = ("authentication failed; please check `VERTA_EMAIL` and `VERTA_DEV_KEY`\n\n{}".format(
                        e.args[0]),) + e.args[1:]

                    raise e

            _utils.raise_for_http_error(response)
            print("connection successfully established")

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
    def monitoring(self):
        return MonitoringClient(self)

    @property
    def projects(self):
        return Projects(self._conn, self._conf).with_workspace(self.get_workspace())

    @property
    def experiments(self):
        return Experiments(self._conn, self._conf).with_workspace(self.get_workspace())

    @property
    def expt_runs(self):
        return ExperimentRuns(self._conn, self._conf).with_workspace(self.get_workspace())

    def _load_config(self):
        with _config_utils.read_merged_config() as config:
            self._config = config

    def _set_from_config_if_none(self, var, resource_name):
        if var is None:
            var = self._config.get(resource_name)
            if var:
                print("setting {} from config file".format(resource_name))
        return var or None

    @staticmethod
    def _validate_visibility(visibility):
        """
        Validates the value of `visibility`.

        Parameters
        ----------
        visibility : :mod:`~verta.visibility` or None

        """
        # TODO: consider a decorator for create_*()s that validates common params
        if (visibility is not None
                and not isinstance(visibility, _visibility._Visibility)):
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
            raise ValueError("workspace \"{}\" not found".format(workspace))

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
            user's personal workspace will be used.
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
            self._ctx.proj = Project._get_by_name(self._conn, self._conf, name, self._ctx.workspace_name)

        if self._ctx.proj is None:
            raise ValueError("Project not found")
        return self._ctx.proj

    def set_project(self, name=None, desc=None, tags=None, attrs=None, workspace=None, public_within_org=None, visibility=None, id=None):
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
            user's personal workspace will be used.
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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
            self._ctx.populate()
        else:
            self._ctx.proj = Project._get_or_create_by_name(self._conn, name,
                                                        lambda name: Project._get_by_name(self._conn, self._conf, name, self._ctx.workspace_name),
                                                        lambda name: Project._create(self._conn, self._conf, self._ctx, name=name, desc=desc, tags=tags, attrs=attrs, public_within_org=public_within_org, visibility=visibility),
                                                        lambda: check_unnecessary_params_warning(
                                                            resource_name, "name {}".format(name),
                                                            param_names, params))

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

            self._ctx.expt = Experiment._get_by_name(self._conn, self._conf, name, self._ctx.proj.id)

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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
            self._ctx.populate()
        else:
            if self._ctx.proj is None:
                self.set_project()

            self._ctx.expt = Experiment._get_or_create_by_name(self._conn, name,
                                                            lambda name: Experiment._get_by_name(self._conn, self._conf, name, self._ctx.proj.id),
                                                            lambda name: Experiment._create(self._conn, self._conf, self._ctx, name=name, desc=desc, tags=tags, attrs=attrs),
                                                            lambda: check_unnecessary_params_warning(
                                                                   resource_name,
                                                                   "name {}".format(name),
                                                                   param_names, params))

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

            self._ctx.expt_run = ExperimentRun._get_by_name(self._conn, self._conf, name, self._ctx.expt.id)

        if self._ctx.expt_run is None:
            raise ValueError("ExperimentRun not Found")
        return self._ctx.expt_run

    def set_experiment_run(self, name=None, desc=None, tags=None, attrs=None, id=None, date_created=None):
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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
            self._ctx.populate()
        else:
            if self._ctx.expt is None:
                self.set_experiment()

            self._ctx.expt_run = ExperimentRun._get_or_create_by_name(self._conn, name,
                                                                    lambda name: ExperimentRun._get_by_name(self._conn, self._conf, name, self._ctx.expt.id),
                                                                    lambda name: ExperimentRun._create(self._conn, self._conf, self._ctx, name=name, desc=desc, tags=tags, attrs=attrs, date_created=date_created),
                                                                    lambda: check_unnecessary_params_warning(
                                                                          resource_name,
                                                                          "name {}".format(name),
                                                                          param_names, params))

        return self._ctx.expt_run

    def get_or_create_repository(self, name=None, workspace=None, id=None, public_within_org=None, visibility=None):
        """
        Gets or creates a Repository by `name` and `workspace`, or gets a Repository by `id`.

        Parameters
        ----------
        name : str
            Name of the Repository. This parameter cannot be provided alongside `id`.
        workspace : str, optional
            Workspace under which the Repository with name `name` exists. If not provided, the
            current user's personal workspace will be used.
        id : str, optional
            ID of the Repository, to be provided instead of `name`.
        public_within_org : bool, optional
            If creating a Repository in an organization's workspace: ``True``
            for public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this repository. If not provided,
            an appropriate default will be used. This parameter should be
            preferred over `public_within_org`.

        Returns
        -------
        :class:`~verta.repository.Repository`
            Specified Repository.

        """
        self._validate_visibility(visibility)

        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        elif id is not None:
            repo = repository.Repository._get(self._conn, id_=id)
            if repo is None:
                raise ValueError("no Repository found with ID {}".format(id))
            print("set existing Repository: {}".format(repo.name))
            return repo
        elif name is not None:
            if workspace is None:
                workspace = self.get_workspace()
            workspace_str = "workspace {}".format(workspace)

            repo = repository.Repository._get(self._conn, name=name, workspace=workspace)

            if not repo:  # not found
                try:
                    repo = repository.Repository._create(self._conn, name=name, workspace=workspace,
                                                          public_within_org=public_within_org, visibility=visibility)
                except requests.HTTPError as e:
                    if e.response.status_code == 409:  # already exists
                        raise RuntimeError("unable to get Repository from ModelDB;"
                                           " please notify the Verta development team")
                    else:
                        six.raise_from(e, None)
                print("created new Repository: {} in {}".format(name, workspace_str))
            else:
                print("set existing Repository: {} from {}".format(name, workspace_str))

            return repo
        else:
            raise ValueError("must specify either `name` or `id`")

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

    def set_repository(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_repository()`.

        """
        return self.get_or_create_repository(*args, **kwargs)
    # set aliases for get-or-create functions for API compatibility

    def get_or_create_registered_model(self, name=None, desc=None, labels=None, workspace=None, public_within_org=None, visibility=None, id=None):
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
            user's personal workspace will be used.
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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
        else:
            registered_model = RegisteredModel._get_or_create_by_name(self._conn, name,
                                                                  lambda name: RegisteredModel._get_by_name(self._conn, self._conf, name, ctx.workspace_name),
                                                                  lambda name: RegisteredModel._create(self._conn, self._conf, ctx, name=name, desc=desc, tags=labels, public_within_org=public_within_org, visibility=visibility),
                                                                  lambda: check_unnecessary_params_warning(
                                                                      resource_name,
                                                                      "name {}".format(name),
                                                                      param_names, params))

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
            registered_model =  RegisteredModel._get_by_name(self._conn, self._conf, name, workspace)

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
        return RegisteredModels(self._conn, self._conf).with_workspace(self.get_workspace())

    @property
    def registered_model_versions(self):
        return RegisteredModelVersions(self._conn, self._conf).with_workspace(self.get_workspace())

    def get_or_create_endpoint(self, path=None, description=None, workspace=None, public_within_org=None, visibility=None, id=None):
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
            user's personal workspace will be used.
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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
            return Endpoint._get_by_id(self._conn, self._conf, workspace, id)
        else:
            return Endpoint._get_or_create_by_name(self._conn, path,
                                            lambda path: Endpoint._get_by_path(self._conn, self._conf, workspace, path),
                                            lambda path: Endpoint._create(self._conn, self._conf, workspace, path, description, public_within_org, visibility),
                                            lambda: check_unnecessary_params_warning(
                                                 resource_name,
                                                 "path {}".format(path),
                                                 param_names, params))



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

    def create_project(self, name=None, desc=None, tags=None, attrs=None, workspace=None, public_within_org=None, visibility=None):
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
            user's personal workspace will be used.
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
            self._conn, self._conf, self._ctx,
            name=name, desc=desc, tags=tags, attrs=attrs,
            public_within_org=public_within_org, visibility=visibility,
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

        self._ctx.expt = Experiment._create(self._conn, self._conf, self._ctx, name=name, desc=desc, tags=tags, attrs=attrs)

        return self._ctx.expt


    def create_experiment_run(self, name=None, desc=None, tags=None, attrs=None, date_created=None):
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

        self._ctx.expt_run = ExperimentRun._create(self._conn, self._conf, self._ctx, name=name, desc=desc, tags=tags, attrs=attrs, date_created=date_created)

        return self._ctx.expt_run


    def create_registered_model(self, name=None, desc=None, labels=None, workspace=None, public_within_org=None, visibility=None):
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
            user's personal workspace will be used.
        public_within_org : bool, optional
            If creating a registered_model in an organization's workspace:
            ``True`` for public, ``False`` for private. In older backends,
            default is private; in newer backends, uses the org's settings by
            default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this registered model. If not
            provided, an appropriate default will be used. This parameter
            should be preferred over `public_within_org`.

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
            self._conn, self._conf, ctx,
            name=name, desc=desc, tags=labels,
            public_within_org=public_within_org, visibility=visibility,
        )


    def create_endpoint(self, path, description=None, workspace=None, public_within_org=None, visibility=None):
        """
        Attaches an endpoint to this Client.

        An accessible endpoint with name `name` will be created and initialized with specified metadata parameters.

        Parameters
        ----------
        path : str
            Path for the endpoint.
        description : str, optional
            Description of the endpoint.
        workspace : str, optional
            Workspace under which the endpoint with name `name` exists. If not provided, the current
            user's personal workspace will be used.
        public_within_org : bool, optional
            If creating an endpoint in an organization's workspace: ``True``
            for public, ``False`` for private. In older backends, default is
            private; in newer backends, uses the org's settings by default.
        visibility : :mod:`~verta.visibility`, optional
            Visibility to set when creating this endpoint. If not provided, an
            appropriate default will be used. This parameter should be
            preferred over `public_within_org`.

        Returns
        -------
        :class:`~verta.registry.entities.RegisteredModel`

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
        return Endpoint._create(self._conn, self._conf, workspace, path, description, public_within_org, visibility)

    @property
    def endpoints(self):
        return Endpoints(self._conn, self._conf, self.get_workspace())

    def download_endpoint_manifest(
            self, download_to_path, path, name, strategy=None,
            resources=None, autoscaling=None, env_vars=None,
            workspace=None):
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
            personal workspace will be used.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where deployment YAML was downloaded to. Matches `download_to_path`.

        """
        if not path.startswith('/'):
            path = '/' + path

        if not strategy:
            strategy = DirectUpdateStrategy()
        if not workspace:
            workspace = self.get_workspace()

        data = {
            'endpoint': {'path': path},
            'name': name,
            'update': Endpoint._create_update_body(strategy, resources, autoscaling, env_vars),
            'workspace_name': workspace,
        }

        endpoint = "{}://{}/api/v1/deployment/operations/manifest".format(
            self._conn.scheme,
            self._conn.socket,
        )

        with _utils.make_request("POST", endpoint, self._conn, json=data, stream=True) as response:
            _utils.raise_for_http_error(response)

            downloaded_to_path = _request_utils.download_file(response, download_to_path, overwrite_ok=True)
            return os.path.abspath(downloaded_to_path)

    def get_or_create_dataset(self, name=None, desc=None, tags=None, attrs=None, workspace=None, time_created=None, public_within_org=None, visibility=None, id=None):
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
            user's personal workspace will be used.
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
            check_unnecessary_params_warning(resource_name, "id {}".format(id),
                                                  param_names, params)
        else:
            dataset = Dataset._get_or_create_by_name(self._conn, name,
                                                        lambda name: Dataset._get_by_name(self._conn, self._conf, name, ctx.workspace_name),
                                                        lambda name: Dataset._create(self._conn, self._conf, ctx, name=name, desc=desc, tags=tags, attrs=attrs, time_created=time_created, public_within_org=public_within_org, visibility=visibility),
                                                        lambda: check_unnecessary_params_warning(
                                                         resource_name,
                                                         "name {}".format(name),
                                                         param_names, params))

        return dataset

    def set_dataset(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_dataset()`.

        .. versionchanged:: 0.16.0
            The dataset versioning interface was overhauled.

        """
        return self.get_or_create_dataset(*args, **kwargs)

    def create_dataset(self, name=None, desc=None, tags=None, attrs=None, workspace=None, time_created=None, public_within_org=None, visibility=None):
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
            user's personal workspace will be used.
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
            self._conn, self._conf, ctx,
            name=name, desc=desc, tags=tags, attrs=attrs, time_created=time_created,
            public_within_org=public_within_org, visibility=visibility,
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
            user's personal workspace will be used.
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
    def find_datasets(self,
                      dataset_ids=None, name=None,
                      tags=None,
                      sort_key=None, ascending=False,
                      workspace=None):
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
            predicates.extend(
                "tags == \"{}\"".format(tag)
                for tag in tags
            )
        if name is not None:
            if not isinstance(name, six.string_types):
                raise TypeError("`name` must be str, not {}".format(type(name)))
            predicates.append("name ~= \"{}\"".format(name))
        if predicates:
            datasets = datasets.find(predicates)

        return datasets

    def _create_organization(self, name, desc=None, collaborator_type=None, global_can_deploy=None):
        return Organization._create(self._conn, name, desc, collaborator_type, global_can_deploy)

    def _get_organization(self, name):
        return Organization._get_by_name(self._conn, name)
