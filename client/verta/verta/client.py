# -*- coding: utf-8 -*-

from __future__ import print_function

import ast
import copy
import glob
import importlib
import os
import pathlib2
import pprint
import re
import shutil
import sys
import tarfile
import tempfile
import time
import warnings
import zipfile

import requests
import yaml

try:
    import PIL
except ImportError:  # Pillow not installed
    PIL = None

from ._protos.public.common import CommonService_pb2 as _CommonCommonService
from ._protos.public.modeldb import CommonService_pb2 as _CommonService
from ._protos.public.modeldb import ProjectService_pb2 as _ProjectService
from ._protos.public.modeldb import ExperimentService_pb2 as _ExperimentService
from ._protos.public.modeldb import ExperimentRunService_pb2 as _ExperimentRunService
from ._protos.public.client import Config_pb2 as _ConfigProtos

from .external import six
from .external.six.moves import cPickle as pickle  # pylint: disable=import-error, no-name-in-module
from .external.six.moves.urllib.parse import urlparse  # pylint: disable=import-error, no-name-in-module

from ._internal_utils import _artifact_utils
from ._internal_utils import _config_utils
from ._internal_utils import _git_utils
from ._internal_utils import _histogram_utils
from ._internal_utils import _pip_requirements_utils
from ._internal_utils import _utils

from . import _dataset
from . import _repository
from ._repository import commit as commit_module
from . import deployment
from . import utils


_OSS_DEFAULT_WORKSPACE = "personal"

# for ExperimentRun._log_modules()
_CUSTOM_MODULES_DIR = "/app/custom_modules/"  # location in DeploymentService model container

# for ExperimentRun.log_model()
_MODEL_ARTIFACTS_ATTR_KEY = "verta_model_artifacts"

_CACHE_DIR = os.path.join(
    os.path.expanduser("~"),
    ".verta",
    "cache",
)


class Client(object):
    """
    Object for interfacing with the ModelDB backend.

    .. deprecated:: 0.12.0
       The `port` parameter will removed in v0.15.0; please combine `port` with the first parameter,
       e.g. `Client("localhost:8080")`.
    .. deprecated:: 0.13.3
       The `expt_runs` attribute will removed in v0.15.0; consider using `proj.expt_runs` and
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
        propagate to any objects that are/were created from this Client.
    ignore_conn_err : bool
        Whether to ignore connection errors and instead return successes with empty contents. Changes
        to this value propagate to any objects that are/were created from this Client.
    debug : bool
        Whether to print extra verbose information to aid in debugging. Changes to this value propagate
        to any objects that are/were created from this Client.
    proj : :class:`Project` or None
        Currently active Project.
    expt : :class:`Experiment` or None
        Currently active Experiment.

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
            })
            # save credentials to env for other Verta Client features
            os.environ['VERTA_EMAIL'] = email
            os.environ['VERTA_DEV_KEY'] = dev_key
        else:
            raise ValueError("`email` and `dev_key` must be provided together")

        back_end_url = urlparse(host)
        socket = back_end_url.netloc + back_end_url.path.rstrip('/')
        if port is not None:
            warnings.warn("`port` (the second parameter) will removed in a later version;"
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
            except requests.ConnectionError:
                six.raise_from(requests.ConnectionError("connection failed; please check `host` and `port`"),
                               None)

            def is_unauthorized(response): return response.status_code == 401

            if is_unauthorized(response):
                auth_error_msg = "authentication failed; please check `VERTA_EMAIL` and `VERTA_DEV_KEY`"
                six.raise_from(requests.HTTPError(auth_error_msg), None)

            _utils.raise_for_http_error(response)
            print("connection successfully established")

        self._conn = conn
        self._conf = _utils.Configuration(use_git, debug)

        self.proj = None
        self.expt = None

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
    def expt_runs(self):
        warnings.warn("`client.expt_runs` is deprecated and will removed in a later version;"
                      " consider using `proj.expt_runs` and `expt.expt_runs` instead",
                      category=FutureWarning)

        if self.expt is None:
            return None
        else:
            return self.expt.expt_runs

    def _get_personal_workspace(self):
        email = self._conn.auth.get('Grpc-Metadata-email')
        if email is not None:
            response = _utils.make_request(
                "GET",
                "{}://{}/api/v1/uac-proxy/uac/getUser".format(self._conn.scheme, self._conn.socket),
                self._conn, params={'email': email},
            )

            if response.ok:
                try:
                    response_json = _utils.body_to_json(response)
                except ValueError:  # not JSON response
                    pass
                else:
                    return response_json['verta_info']['username']
            else:
                if response.status_code == 404:  # UAC not found
                    pass
                else:
                    _utils.raise_for_http_error(response)
        return _OSS_DEFAULT_WORKSPACE

    def _load_config(self):
        config_file = self._find_config_in_all_dirs()
        if config_file is not None:
            stream = open(config_file, 'r')
            self._config = yaml.load(stream, Loader=yaml.FullLoader)
            # validate config against proto spec
            _utils.json_to_proto(
                self._config,
                _ConfigProtos.Config,
                ignore_unknown_fields=False,
            )
        else:
            self._config = {}

    def _find_config_in_all_dirs(self):
        res = self._find_config('./', True)
        if res is None:
            return self._find_config("{}/.verta/".format(os.path.expanduser("~")))
        return res

    def _find_config(self, prefix, recursive=False):
        for ff in _config_utils.CONFIG_FILENAMES:
            if os.path.isfile(prefix + ff):
                return prefix + ff
        if recursive:
            for dir in [os.path.join(prefix, o) for o in os.listdir(prefix) if os.path.isdir(os.path.join(prefix, o))]:
                config_file = self._find_config(dir + "/", True)
                if config_file is not None:
                    return config_file
        return None

    def _set_from_config_if_none(self, var, resource_name):
        if var is None:
            var = self._config.get(resource_name)
            if var:
                print("setting {} from config file".format(resource_name))
        return var or None

    def set_project(self, name=None, desc=None, tags=None, attrs=None, workspace=None, public_within_org=None, id=None):
        """
        Attaches a Project to this Client.

        If an accessible Project with name `name` does not already exist, it will be created
        and initialized with specified metadata parameters. If such a Project does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise an exception.

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
        public_within_org : bool, default False
            If creating a Project in an organization's workspace, whether to make this Project
            accessible to all members of that organization.
        id : str, optional
            ID of the Project. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`Project`

        Raises
        ------
        ValueError
            If a Project with `name` already exists, but metadata parameters are passed in.

        """
        # if proj already in progress, reset expt
        if self.proj is not None:
            self.expt = None

        if name is None and id is None:
            name = self._set_from_config_if_none(name, "project")
        workspace = self._set_from_config_if_none(workspace, "workspace")

        self.proj = Project(self._conn, self._conf,
                            name,
                            desc, tags, attrs,
                            workspace,
                            public_within_org,
                            id)

        return self.proj

    def set_experiment(self, name=None, desc=None, tags=None, attrs=None, id=None):
        """
        Attaches an Experiment under the currently active Project to this Client.

        If an accessible Experiment with name `name` does not already exist under the currently
        active Project, it will be created and initialized with specified metadata parameters. If
        such an Experiment does already exist, it will be retrieved; specifying metadata parameters
        in this case will raise an exception.

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
        :class:`Experiment`

        Raises
        ------
        ValueError
            If an Experiment with `name` already exists, but metadata parameters are passed in.
        AttributeError
            If a Project is not yet in progress.

        """
        if name is None and id is None:
            name = self._set_from_config_if_none(name, "experiment")

        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        elif id is not None:
            # find Experiment by ID
            expt_msg = Experiment._get(self._conn, _expt_id=id)
            if expt_msg is None:
                raise ValueError("no Experiment found with ID {}".format(id))
            # set parent Project by ID
            try:
                self.proj = Project(self._conn, self._conf, _proj_id=expt_msg.project_id)
            except ValueError:  # parent Project not found
                raise RuntimeError("unable to find parent Project of Experiment with ID {};"
                                   " this should only ever occur due to a back end error".format(id))
            # set Experiment
            self.expt = Experiment(self._conn, self._conf,
                                   _expt_id=id)
        else:
            # set Experiment by name under current Project
            if self.proj is None:
                # automatically set Project if in config
                proj_name = self._set_from_config_if_none(None, "project")
                if proj_name is not None:
                    self.set_project(proj_name)
                else:
                    raise AttributeError("a Project must first be in progress")

            self.expt = Experiment(self._conn, self._conf,
                                   self.proj.id, name,
                                   desc, tags, attrs)

        return self.expt

    def set_experiment_run(self, name=None, desc=None, tags=None, attrs=None, id=None, date_created=None):
        """
        Attaches an Experiment Run under the currently active Experiment to this Client.

        If an accessible Experiment Run with name `name` does not already exist under the
        currently active Experiment, it will be created and initialized with specified metadata
        parameters. If such a Experiment Run does already exist, it will be retrieved; specifying
        metadata parameters in this case will raise an exception.

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
        :class:`ExperimentRun`

        Raises
        ------
        ValueError
            If an Experiment Run with `name` already exists, but metadata parameters are passed in.
        AttributeError
            If an Experiment is not yet in progress.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        elif id is not None:
            # find ExperimentRun by ID
            expt_run_msg = ExperimentRun._get(self._conn, _expt_run_id=id)
            if expt_run_msg is None:
                raise ValueError("no ExperimentRun found with ID {}".format(id))
            # set parent Project by ID
            try:
                self.proj = Project(self._conn, self._conf, _proj_id=expt_run_msg.project_id)
            except ValueError:  # parent Project not found
                raise RuntimeError("unable to find parent Project of ExperimentRun with ID {};"
                                   " this should only ever occur due to a back end error".format(id))
            # set parent Experiment by ID
            try:
                self.expt = Experiment(self._conn, self._conf, _expt_id=expt_run_msg.experiment_id)
            except ValueError:  # parent Experiment not found
                raise RuntimeError("unable to find parent Experiment of ExperimentRun with ID {};"
                                   " this should only ever occur due to a back end error".format(id))
            # set ExperimentRun
            expt_run = ExperimentRun(self._conn, self._conf,
                                     _expt_run_id=id)
        else:
            # set ExperimentRun by name under current Experiment
            if self.expt is None:
                # automatically set Experiment if in config
                expt_name = self._set_from_config_if_none(None, "experiment")
                if expt_name is not None:
                    self.set_experiment(expt_name)
                else:
                    raise AttributeError("an Experiment must first be in progress")

            expt_run = ExperimentRun(self._conn, self._conf,
                                     self.proj.id, self.expt.id, name,
                                     desc, tags, attrs, date_created=date_created)

        return expt_run

    def get_or_create_repository(self, name=None, workspace=None, id=None):
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

        Returns
        -------
        :class:`~verta._repository.Repository`
            Specified Repository.

        """
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        elif id is not None:
            repo = _repository.Repository._get(self._conn, id_=id)
            if repo is None:
                raise ValueError("no Repository found with ID {}".format(id))
            print("set existing Repository: {}".format(repo.name))
            return repo
        elif name is not None:
            if workspace is None:
                workspace_str = "personal workspace"
                workspace = self._get_personal_workspace()
            else:
                workspace_str = "workspace {}".format(workspace)

            try:
                repo = _repository.Repository._create(self._conn, name, workspace)
            except requests.HTTPError as e:
                if e.response.status_code == 403:  # cannot create in other workspace
                    repo = _repository.Repository._get(self._conn, name=name, workspace=workspace)
                    if repo is None:  # not accessible in other workspace
                        six.raise_from(e, None)
                elif e.response.status_code == 409:  # already exists
                    repo = _repository.Repository._get(self._conn, name=name, workspace=workspace)
                    if repo is None:  # already exists, but couldn't get it
                        raise RuntimeError("unable to get Repository from ModelDB;"
                                           " please notify the Verta development team")
                else:
                    six.raise_from(e, None)
                print("set existing Repository: {} from {}".format(name, workspace_str))
            else:
                print("created new Repository: {} in {}".format(name, workspace_str))
            return repo
        else:
            raise ValueError("must specify either `name` or `id`")

    # NOTE: dataset visibility cannot be set via a client
    def set_dataset(self, name=None, type="local",
                    desc=None, tags=None, attrs=None,
                    workspace=None,
                    public_within_org=None,
                    id=None):
        """
        Attaches a Dataset to this Client.

        If an accessible Dataset with name `name` does not already exist, it will be created
        and initialized with specified metadata parameters. If such a Dataset does already exist,
        it will be retrieved; specifying metadata parameters in this case will raise an exception.

        Parameters
        ----------
        name : str, optional
            Name of the Dataset. If no name is provided, one will be generated.
        type : str, one of {'local', 's3', 'big query', 'atlas hive', 'postgres'}
            The type of the dataset so we can collect the right type of metadata
        desc : str, optional
            Description of the Dataset.
        tags : list of str, optional
            Tags of the Dataset.
        attrs : dict of str to {None, bool, float, int, str}, optional
            Attributes of the Dataset.
        workspace : str, optional
            Workspace under which the Dataset with name `name` exists. If not provided, the current
            user's personal workspace will be used.
        public_within_org : bool, default False
            If creating a Dataset in an organization's workspace, whether to make this Dataset
            accessible to all members of that organization.
        id : str, optional
            ID of the Dataset. This parameter cannot be provided alongside `name`, and other
            parameters will be ignored.

        Returns
        -------
        :class:`Dataset`

        Raises
        ------
        ValueError
            If a Dataset with `name` already exists, but metadata parameters are passed in.

        """
        # Note: If a dataset with `name` already exists,
        #       there is no way to determine its type/subclass from back end,
        #       so it is assumed that the user has passed in the correct `type`.
        if type == "local":
            DatasetSubclass = _dataset.LocalDataset
        elif type == "s3":
            DatasetSubclass = _dataset.S3Dataset
        elif type == "big query":
            DatasetSubclass = _dataset.BigQueryDataset
        elif type == "atlas hive":
            DatasetSubclass = _dataset.AtlasHiveDataset
        elif type == "postgres":
            DatasetSubclass = _dataset.RDBMSDataset
        else:
            raise ValueError("`type` must be one of {'local', 's3', 'big query', 'atlas hive', 'postgres'}")

        name = self._set_from_config_if_none(name, "dataset")
        workspace = self._set_from_config_if_none(workspace, "workspace")
        return DatasetSubclass(self._conn, self._conf,
                               name=name, desc=desc, tags=tags, attrs=attrs,
                               workspace=workspace,
                               public_within_org=public_within_org,
                               _dataset_id=id)

    def get_dataset(self, name=None, id=None):
        """
        Retrieve an already created Dataset. Only one of name or id can be provided.

        Parameters
        ----------
        name : str, optional
            Name of the Dataset.
        id : str, optional
            ID of the Dataset. This parameter cannot be provided alongside `name`.

        Returns
        -------
        :class:`Dataset`
        """
        return _dataset.Dataset(self._conn, self._conf, name=name, _dataset_id=id)

    def find_datasets(self,
                      dataset_ids=None, name=None,
                      tags=None,
                      sort_key=None, ascending=False,
                      workspace=None):
        """
        Gets the Datasets in `workspace` that match the given query parameters. If no parameters
        are specified, we return all datasets.

        Parameters
        ----------
        dataset_ids : list of str, optional
            IDs of datasets that we wish to retrieve
        name: str, optional
            Name of dataset we wish to retrieve. Fuzzy matches supported.
        tags: list of str, optional
            List of tags by which we'd like to query datasets.
        sort_key: string, optional
            Key by which the resulting list of datasets should be sorted.
        ascending: bool, default: False
            Whether to sort returned datasets in ascending or descending order.
        workspace : str, optional
            Workspace in which to look for datasets. If not provided, the current user's personal
            workspace will be used.

        Returns
        -------
        list of :class:`Dataset`

        """
        predicates = []
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
            for tag in tags:
                predicates.append(
                    _CommonService.KeyValueQuery(key="tags",
                                                 value=_utils.python_to_val_proto(tag),
                                                 operator=_CommonService.OperatorEnum.EQ))
        if name is not None:
            if not isinstance(name, six.string_types):
                raise TypeError("`name` must be str, not {}".format(type(name)))
            predicates.append(
                _CommonService.KeyValueQuery(key="name",
                                             value=_utils.python_to_val_proto(name),
                                             operator=_CommonService.OperatorEnum.CONTAIN))
        Message = _dataset._DatasetService.FindDatasets
        msg = Message(dataset_ids=dataset_ids, predicates=predicates,
                      ascending=ascending, sort_key=sort_key,
                      workspace_name=workspace)
        endpoint = "{}://{}/api/v1/modeldb/dataset/findDatasets"
        return _dataset.DatasetLazyList(self._conn, self._conf, msg, endpoint, "POST")

    def get_dataset_version(self, id):
        """
        Retrieve an already created DatasetVersion.

        Parameters
        ----------
        id : str
            ID of the DatasetVersion.

        Returns
        -------
        :class:`DatasetVersion`
        """
        return _dataset.DatasetVersion(self._conn, self._conf, _dataset_version_id=id)

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

    def get_or_create_dataset(self, *args, **kwargs):
        """
        Alias for :meth:`Client.set_dataset()`.

        """
        return self.set_dataset(*args, **kwargs)

    def set_repository(self, *args, **kwargs):
        """
        Alias for :meth:`Client.get_or_create_repository()`.

        """
        return self.get_or_create_repository(*args, **kwargs)



class _ModelDBEntity(object):
    def __init__(self, conn, conf, service_module, service_url_component, id):
        self._conn = conn
        self._conf = conf

        self._service = service_module
        self._request_url = "{}://{}/api/v1/modeldb/{}/{}".format(self._conn.scheme,
                                                      self._conn.socket,
                                                      service_url_component,
                                                      '{}')  # endpoint placeholder

        self.id = id

    def __getstate__(self):
        state = self.__dict__.copy()

        state['_service_module_name'] = state['_service'].__name__
        del state['_service']

        return state

    def __setstate__(self, state):
        state['_service'] = importlib.import_module(state['_service_module_name'])
        del state['_service_module_name']

        self.__dict__.update(state)

    def _get_url_for_artifact(self, key, method, artifact_type=0, part_num=0):
        """
        Obtains a URL to use for accessing stored artifacts.

        Parameters
        ----------
        key : str
            Name of the artifact.
        method : {'GET', 'PUT'}
            HTTP method to request for the generated URL.
        artifact_type : int, optional
            Variant of `_CommonCommonService.ArtifactTypeEnum`. This informs the backend what slot to check
            for the artifact, if necessary.
        part_num : int, optional
            If using Multipart Upload, number of part to be uploaded.

        Returns
        -------
        response_msg : `_CommonService.GetUrlForArtifact.Response`
            Backend response.

        """
        if method.upper() not in ("GET", "PUT"):
            raise ValueError("`method` must be one of {'GET', 'PUT'}")

        Message = _CommonService.GetUrlForArtifact
        msg = Message(
            id=self.id, key=key,
            method=method.upper(),
            artifact_type=artifact_type,
            part_number=part_num,
        )
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       self._request_url.format("getUrlForArtifact"),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)

        url = response_msg.url
        # accommodate port-forwarded NFS store
        if 'https://localhost' in url[:20]:
            url = 'http' + url[5:]
        if 'localhost%3a' in url[:20]:
            url = url.replace('localhost%3a', 'localhost:')
        if 'localhost%3A' in url[:20]:
            url = url.replace('localhost%3A', 'localhost:')
        response_msg.url = url

        return response_msg

    def _cache(self, filename, contents):
        """
        Caches `contents` to `filename` within ``_CACHE_DIR``.

        If `contents` represents a ZIP file, then it will be unzipped, and the path to the target
        directory will be returned.

        Parameters
        ----------
        filename : str
            Filename within ``_CACHE_DIR`` to write to.
        contents : bytes
            Contents to be cached.

        Returns
        -------
        str
            Full path to cached contents.

        """
        # write contents to temporary file
        with tempfile.NamedTemporaryFile(delete=False) as tempf:
            tempf.write(contents)
            tempf.flush()  # flush object buffer
            os.fsync(tempf.fileno())  # flush OS buffer

        name, extension = os.path.splitext(filename)
        if extension == '.zip':
            temp_path = tempfile.mkdtemp()

            with zipfile.ZipFile(tempf.name, 'r') as zipf:
                zipf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.tgz':
            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r:gz') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.tar':
            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        elif extension == '.gz' and os.path.splitext(name)[1] == '.tar':
            name = os.path.splitext(name)[0]

            temp_path = tempfile.mkdtemp()

            with tarfile.open(tempf.name, 'r:gz') as tarf:
                tarf.extractall(temp_path)
            os.remove(tempf.name)
        else:
            name = filename
            temp_path = tempf.name

        path = os.path.join(_CACHE_DIR, name)

        # create intermediate dirs
        try:
            os.makedirs(os.path.dirname(path))
        except OSError:  # already exists
            pass

        # move written contents to cache location
        os.rename(temp_path, path)

        return path

    def _get_cached(self, filename):
        name, extension = os.path.splitext(filename)
        if extension == '.zip':
            pass
        elif extension == '.tgz':
            pass
        elif extension == '.tar':
            pass
        elif extension == '.gz' and os.path.splitext(name)[1] == '.tar':
            name = os.path.splitext(name)[0]
        else:
            name = filename

        path = os.path.join(_CACHE_DIR, name)
        return path if os.path.exists(path) else None

    def log_code(self, exec_path=None, repo_url=None, commit_hash=None, overwrite=False):
        """
        Logs the code version.

        A code version is either information about a Git snapshot or a bundle of Python source code files.

        `repo_url` and `commit_hash` can only be set if `use_git` was set to ``True`` in the Client.

        Parameters
        ----------
        exec_path : str, optional
            Filepath to the executable Python script or Jupyter notebook. If no filepath is provided,
            the Client will make its best effort to find the currently running script/notebook file.
        repo_url : str, optional
            URL for a remote Git repository containing `commit_hash`. If no URL is provided, the Client
            will make its best effort to find it.
        commit_hash : str, optional
            Git commit hash associated with this code version. If no hash is provided, the Client will
            make its best effort to find it.
        overwrite : bool, default False
            Whether to allow overwriting a code version.

        Examples
        --------
        With ``Client(use_git=True)`` (default):

            Log Git snapshot information, plus the location of the currently executing notebook/script
            relative to the repository root:

            .. code-block:: python

                proj.log_code()
                proj.get_code()
                # {'exec_path': 'comparison/outcomes/classification.ipynb',
                #  'repo_url': 'git@github.com:VertaAI/experiments.git',
                #  'commit_hash': 'f99abcfae6c3ce6d22597f95ad6ef260d31527a6',
                #  'is_dirty': False}

            Log Git snapshot information, plus the location of a specific source code file relative
            to the repository root:

            .. code-block:: python

                proj.log_code("../trainer/training_pipeline.py")
                proj.get_code()
                # {'exec_path': 'comparison/trainer/training_pipeline.py',
                #  'repo_url': 'git@github.com:VertaAI/experiments.git',
                #  'commit_hash': 'f99abcfae6c3ce6d22597f95ad6ef260d31527a6',
                #  'is_dirty': False}

        With ``Client(use_git=False)``:

            Find and upload the currently executing notebook/script:

            .. code-block:: python

                proj.log_code()
                zip_file = proj.get_code()
                zip_file.printdir()
                # File Name                          Modified             Size
                # classification.ipynb        2019-07-10 17:18:24        10287

            Upload a specific source code file:

            .. code-block:: python

                proj.log_code("../trainer/training_pipeline.py")
                zip_file = proj.get_code()
                zip_file.printdir()
                # File Name                          Modified             Size
                # training_pipeline.py        2019-05-31 10:34:44          964

        """
        if self._conf.use_git:
            # verify Git
            try:
                repo_root_dir = _git_utils.get_git_repo_root_dir()
            except OSError:
                # don't halt execution
                print("unable to locate git repository; you may be in an unsupported environment")
                return
                # six.raise_from(OSError("failed to locate git repository; please check your working directory"),
                #                None)
            print("Git repository successfully located at {}".format(repo_root_dir))
        elif repo_url is not None or commit_hash is not None:
            raise ValueError("`repo_url` and `commit_hash` can only be set if `use_git` was set to True in the Client")

        if exec_path is None:
            # find dynamically
            try:
                exec_path = _utils.get_notebook_filepath()
            except OSError:  # notebook not found
                try:
                    exec_path = _utils.get_script_filepath()
                except OSError:  # script not found
                    print("unable to find code file; skipping")
        else:
            exec_path = os.path.expanduser(exec_path)
            if not os.path.isfile(exec_path):
                raise ValueError("`exec_path` \"{}\" must be a valid filepath".format(exec_path))

        if isinstance(self, Project):  # TODO: not this
            Message = self._service.LogProjectCodeVersion
            endpoint = "logProjectCodeVersion"
        elif isinstance(self, Experiment):
            Message = self._service.LogExperimentCodeVersion
            endpoint = "logExperimentCodeVersion"
        elif isinstance(self, ExperimentRun):
            Message = self._service.LogExperimentRunCodeVersion
            endpoint = "logExperimentRunCodeVersion"
        msg = Message(id=self.id)

        if overwrite:
            if isinstance(self, ExperimentRun):
                msg.overwrite = True
            else:
                raise ValueError("`overwrite=True` is currently only supported for ExperimentRun")

        if self._conf.use_git:
            try:
                # adjust `exec_path` to be relative to repo root
                exec_path = os.path.relpath(exec_path, _git_utils.get_git_repo_root_dir())
            except OSError as e:
                print("{}; logging absolute path to file instead")
                exec_path = os.path.abspath(exec_path)
            msg.code_version.git_snapshot.filepaths.append(exec_path)

            try:
                msg.code_version.git_snapshot.repo = repo_url or _git_utils.get_git_remote_url()
            except OSError as e:
                print("{}; skipping".format(e))

            try:
                msg.code_version.git_snapshot.hash = commit_hash or _git_utils.get_git_commit_hash()
            except OSError as e:
                print("{}; skipping".format(e))

            try:
                is_dirty = _git_utils.get_git_commit_dirtiness(commit_hash)
            except OSError as e:
                print("{}; skipping".format(e))
            else:
                if is_dirty:
                    msg.code_version.git_snapshot.is_dirty = _CommonCommonService.TernaryEnum.TRUE
                else:
                    msg.code_version.git_snapshot.is_dirty = _CommonCommonService.TernaryEnum.FALSE
        else:  # log code as Artifact
            if exec_path is None:
                # don't halt execution
                print("unable to find code file; you may be in an unsupported environment")
                return
                # raise RuntimeError("unable to find code file; you may be in an unsupported environment")

            # write ZIP archive
            zipstream = six.BytesIO()
            with zipfile.ZipFile(zipstream, 'w') as zipf:
                filename = os.path.basename(exec_path)
                if exec_path.endswith(".ipynb"):
                    try:
                        saved_notebook = _utils.save_notebook(exec_path)
                    except:  # failed to save
                        print("unable to automatically save Notebook;"
                              " logging latest checkpoint from disk")
                        zipf.write(exec_path, filename)
                    else:
                        zipf.writestr(filename, six.ensure_binary(saved_notebook.read()))
                else:
                    zipf.write(exec_path, filename)
            zipstream.seek(0)

            key = 'code'
            extension = 'zip'

            artifact_hash = _artifact_utils.calc_sha256(zipstream)
            zipstream.seek(0)
            basename = key + os.extsep + extension
            artifact_path = os.path.join(artifact_hash, basename)

            msg.code_version.code_archive.path = artifact_path
            msg.code_version.code_archive.path_only = False
            msg.code_version.code_archive.artifact_type = _CommonCommonService.ArtifactTypeEnum.CODE
            msg.code_version.code_archive.filename_extension = extension
        # TODO: check if we actually have any loggable information
        msg.code_version.date_logged = _utils.now()

        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       self._request_url.format(endpoint),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("a code version has already been logged")
            else:
                _utils.raise_for_http_error(response)

        if msg.code_version.WhichOneof("code") == 'code_archive':
            # upload artifact to artifact store
            url = self._get_url_for_artifact("verta_code_archive", "PUT", msg.code_version.code_archive.artifact_type).url

            response = _utils.make_request("PUT", url, self._conn, data=zipstream)
            _utils.raise_for_http_error(response)

    def get_code(self):
        """
        Gets the code version.

        Returns
        -------
        dict or zipfile.ZipFile
            Either:
                - a dictionary containing Git snapshot information with at most the following items:
                    - **filepaths** (*list of str*)
                    - **repo** (*str*) – Remote repository URL
                    - **hash** (*str*) – Commit hash
                    - **is_dirty** (*bool*)
                - a `ZipFile <https://docs.python.org/3/library/zipfile.html#zipfile.ZipFile>`_
                  containing Python source code files

        """
        if isinstance(self, Project):  # TODO: not this
            Message = self._service.GetProjectCodeVersion
            endpoint = "getProjectCodeVersion"
        elif isinstance(self, Experiment):
            Message = self._service.GetExperimentCodeVersion
            endpoint = "getExperimentCodeVersion"
        elif isinstance(self, ExperimentRun):
            Message = self._service.GetExperimentRunCodeVersion
            endpoint = "getExperimentRunCodeVersion"
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       self._request_url.format(endpoint),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        code_ver_msg = response_msg.code_version
        which_code = code_ver_msg.WhichOneof('code')
        if which_code == 'git_snapshot':
            git_snapshot_msg = code_ver_msg.git_snapshot
            git_snapshot = {}
            if git_snapshot_msg.filepaths:
                git_snapshot['filepaths'] = git_snapshot_msg.filepaths
            if git_snapshot_msg.repo:
                git_snapshot['repo_url'] = git_snapshot_msg.repo
            if git_snapshot_msg.hash:
                git_snapshot['commit_hash'] = git_snapshot_msg.hash
                if git_snapshot_msg.is_dirty != _CommonCommonService.TernaryEnum.UNKNOWN:
                    git_snapshot['is_dirty'] = git_snapshot_msg.is_dirty == _CommonCommonService.TernaryEnum.TRUE
            return git_snapshot
        elif which_code == 'code_archive':
            # download artifact from artifact store
            url = self._get_url_for_artifact("verta_code_archive", "GET", code_ver_msg.code_archive.artifact_type).url

            response = _utils.make_request("GET", url, self._conn)
            _utils.raise_for_http_error(response)

            code_archive = six.BytesIO(response.content)
            return zipfile.ZipFile(code_archive, 'r')  # TODO: return a util class instead, maybe
        else:
            raise RuntimeError("unable find code in response")


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


class Experiment(_ModelDBEntity):
    """
    Object representing a machine learning Experiment.

    This class provides read/write functionality for Experiment metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_experiment`.

    Attributes
    ----------
    id : str
        ID of this Experiment.
    name : str
        Name of this Experiment.
    expt_runs : :class:`ExperimentRuns`
        Experiment Runs under this Experiment.

    """
    def __init__(self, conn, conf,
                 proj_id=None, expt_name=None,
                 desc=None, tags=None, attrs=None,
                 _expt_id=None):
        if expt_name is not None and _expt_id is not None:
            raise ValueError("cannot specify both `expt_name` and `_expt_id`")

        if _expt_id is not None:
            expt = Experiment._get(conn, _expt_id=_expt_id)
            if expt is not None:
                print("set existing Experiment: {}".format(expt.name))
            else:
                raise ValueError("Experiment with ID {} not found".format(_expt_id))
        elif proj_id is not None:
            if expt_name is None:
                expt_name = Experiment._generate_default_name()
            try:
                expt = Experiment._create(conn, proj_id, expt_name, desc, tags, attrs)
            except requests.HTTPError as e:
                if e.response.status_code == 409:  # already exists
                    if any(param is not None for param in (desc, tags, attrs)):
                        warnings.warn("Experiment with name {} already exists;"
                                      " cannot initialize `desc`, `tags`, or `attrs`".format(expt_name))
                    expt = Experiment._get(conn, proj_id, expt_name)
                    if expt is not None:
                        print("set existing Experiment: {}".format(expt.name))
                    else:
                        raise RuntimeError("unable to retrieve Experiment {};"
                                           " please notify the Verta development team".format(expt_name))
                else:
                    raise e
            else:
                print("created new Experiment: {}".format(expt.name))
        else:
            raise ValueError("insufficient arguments")

        super(Experiment, self).__init__(conn, conf, _ExperimentService, "experiment", expt.id)

    def __repr__(self):
        return "<Experiment \"{}\">".format(self.name)

    @property
    def name(self):
        Message = _ExperimentService.GetExperimentById
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment/getExperimentById".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.experiment.name

    @property
    def expt_runs(self):
        # get runs in this Experiment
        runs = ExperimentRuns(self._conn, self._conf)
        runs._msg.experiment_id = self.id
        return runs

    @staticmethod
    def _generate_default_name():
        return "Expt {}".format(_utils.generate_default_name())

    @staticmethod
    def _get(conn, proj_id=None, expt_name=None, _expt_id=None):
        if _expt_id is not None:
            Message = _ExperimentService.GetExperimentById
            msg = Message(id=_expt_id)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/experiment/getExperimentById".format(conn.scheme, conn.socket),
                                           conn, params=data)
        elif None not in (proj_id, expt_name):
            Message = _ExperimentService.GetExperimentByName
            msg = Message(project_id=proj_id, name=expt_name)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/experiment/getExperimentByName".format(conn.scheme, conn.socket),
                                           conn, params=data)
        else:
            raise ValueError("insufficient arguments")

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            expt = response_msg.experiment

            if not expt.id:  # 200, but empty message
                raise RuntimeError("unable to retrieve Experiment {};"
                                   " please notify the Verta development team".format(expt_name))

            return expt
        else:
            if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                    or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                return None
            else:
                _utils.raise_for_http_error(response)

    @staticmethod
    def _create(conn, proj_id, expt_name, desc=None, tags=None, attrs=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        Message = _ExperimentService.CreateExperiment
        msg = Message(project_id=proj_id, name=expt_name,
                      description=desc, tags=tags, attributes=attrs)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment/createExperiment".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            return response_msg.experiment
        else:
            _utils.raise_for_http_error(response)


class ExperimentRuns(_utils.LazyList):
    r"""
    ``list``-like object representing a collection of machine learning Experiment Runs.

    This class provides functionality for filtering and sorting its contents.

    There should not be a need to instantiate this class directly; please use other classes'
    attributes to access Experiment Runs.

    Examples
    --------
    .. code-block:: python

        runs = expt.find("hyperparameters.hidden_size == 256")
        len(runs)
        # 12
        runs += expt.find("hyperparameters.hidden_size == 512")
        len(runs)
        # 24
        runs = runs.find("metrics.accuracy >= .8")
        len(runs)
        # 5
        runs[0].get_metric("accuracy")
        # 0.8921755939794525

    """
    _OP_MAP = {'==': _CommonService.OperatorEnum.EQ,
               '!=': _CommonService.OperatorEnum.NE,
               '>':  _CommonService.OperatorEnum.GT,
               '>=': _CommonService.OperatorEnum.GTE,
               '<':  _CommonService.OperatorEnum.LT,
               '<=': _CommonService.OperatorEnum.LTE}
    _OP_PATTERN = re.compile(r"({})".format('|'.join(sorted(six.viewkeys(_OP_MAP), key=lambda s: len(s), reverse=True))))

    # keys that yield predictable, sensible results
    _VALID_QUERY_KEYS = {
        'id', 'project_id', 'experiment_id',
        'name',
        'date_created',
        'attributes', 'hyperparameters', 'metrics',
    }

    def __init__(self, conn, conf):
        super(ExperimentRuns, self).__init__(
            conn, conf,
            _ExperimentRunService.FindExperimentRuns(ids_only=True),
            "{}://{}/api/v1/modeldb/experiment-run/findExperimentRuns",
            "POST",
        )

    def __repr__(self):
        return "<ExperimentRuns containing {} runs>".format(self.__len__())

    def _get_records(self, response_msg):
        return response_msg.experiment_runs

    def _create_element(self, id_):
        return ExperimentRun(self._conn, self._conf, _expt_run_id=id_)

    def find(self, where, ret_all_info=False):
        """
        Gets the Experiment Runs from this collection that match predicates `where`.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A predicate in `where` is a string containing a simple boolean expression consisting of:

            - a dot-delimited Experiment Run property such as ``metrics.accuracy``
            - a Python boolean operator such as ``>=``
            - a literal value such as ``.8``

        Parameters
        ----------
        where : str or list of str
            Predicates specifying Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.find(["hyperparameters.hidden_size == 256",
                       "metrics.accuracy >= .8"])
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        new_runs = copy.deepcopy(self)

        if isinstance(where, six.string_types):
            where = [where]
        for predicate in where:
            # split predicate
            try:
                key, operator, value = map(lambda token: token.strip(), self._OP_PATTERN.split(predicate, maxsplit=1))
            except ValueError:
                six.raise_from(ValueError("predicate `{}` must be a two-operand comparison".format(predicate)),
                               None)

            if key.split('.')[0] not in self._VALID_QUERY_KEYS:
                raise ValueError("key `{}` is not a valid key for querying;"
                                 " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

            # cast operator into protobuf enum variant
            operator = self._OP_MAP[operator]

            try:
                value = float(value)
            except ValueError:  # not a number
                # parse value
                try:
                    expr_node = ast.parse(value, mode='eval')
                except SyntaxError:
                    e = ValueError("value `{}` must be a number or string literal".format(value))
                    six.raise_from(e, None)
                value_node = expr_node.body
                if type(value_node) is ast.Str:
                    value = value_node.s
                elif type(value_node) is ast.Compare:
                    e = ValueError("predicate `{}` must be a two-operand comparison".format(predicate))
                    six.raise_from(e, None)
                else:
                    e = ValueError("value `{}` must be a number or string literal".format(value))
                    six.raise_from(e, None)

            new_runs._msg.predicates.append(  # pylint: disable=no-member
                _CommonService.KeyValueQuery(
                    key=key, value=_utils.python_to_val_proto(value),
                    operator=operator,
                )
            )

        return new_runs

    def sort(self, key, descending=False, ret_all_info=False):
        """
        Sorts the Experiment Runs from this collection by `key`.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as
        ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        descending : bool, default False
            Order in which to return sorted Experiment Runs.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.sort("metrics.accuracy")
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        new_runs = copy.deepcopy(self)

        new_runs._msg.sort_key = key
        new_runs._msg.ascending = not descending

        return new_runs

    def top_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` highest `key`\ s.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as
        ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        k : int
            Number of Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.top_k("metrics.accuracy", 3)
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        # apply sort to new Runs
        new_runs = copy.deepcopy(self)
        new_runs._msg.sort_key = key
        new_runs._msg.ascending = False

        # copy msg to avoid mutating `new_runs`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(new_runs._msg)
        msg.page_limit = k
        msg.page_number = 1

        response_msg = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in response_msg.experiment_runs)

        return new_runs

    def bottom_k(self, key, k, ret_all_info=False):
        r"""
        Gets the Experiment Runs from this collection with the `k` lowest `key`\ s.

        .. deprecated:: 0.13.3
           The `ret_all_info` parameter will removed in v0.15.0.

        A `key` is a string containing a dot-delimited Experiment Run property such as ``metrics.accuracy``.

        Parameters
        ----------
        key : str
            Dot-delimited Experiment Run property.
        k : int
            Number of Experiment Runs to get.

        Returns
        -------
        :class:`ExperimentRuns`

        Warnings
        --------
        This feature is still in active development. It is completely safe to use, but may exhibit
        unintuitive behavior. Please report any oddities to the Verta team!

        Examples
        --------
        .. code-block:: python

            runs.bottom_k("metrics.loss", 3)
            # <ExperimentRuns containing 3 runs>

        """
        if ret_all_info:
            warnings.warn("`ret_all_info` is deprecated and will removed in a later version",
                          category=FutureWarning)

        if key.split('.')[0] not in self._VALID_QUERY_KEYS:
            raise ValueError("key `{}` is not a valid key for querying;"
                             " currently supported keys are: {}".format(key, self._VALID_QUERY_KEYS))

        # apply sort to new Runs
        new_runs = copy.deepcopy(self)
        new_runs._msg.sort_key = key
        new_runs._msg.ascending = True

        # copy msg to avoid mutating `new_runs`'s state
        msg = self._msg.__class__()
        msg.CopyFrom(new_runs._msg)
        msg.page_limit = k
        msg.page_number = 1

        response_msg = self._call_back_end(msg)

        # cannot assign to `experiment_run_ids` because Protobuf fields don't allow it
        del new_runs._msg.experiment_run_ids[:]
        new_runs._msg.experiment_run_ids.extend(record.id for record in response_msg.experiment_runs)

        return new_runs


class ExperimentRun(_ModelDBEntity):
    """
    Object representing a machine learning Experiment Run.

    This class provides read/write functionality for Experiment Run metadata.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_experiment_run`.

    Attributes
    ----------
    id : str
        ID of this Experiment Run.
    name : str
        Name of this Experiment Run.

    """
    def __init__(self, conn, conf,
                 proj_id=None, expt_id=None, expt_run_name=None,
                 desc=None, tags=None, attrs=None,
                 date_created=None,
                 _expt_run_id=None):
        if expt_run_name is not None and _expt_run_id is not None:
            raise ValueError("cannot specify both `expt_run_name` and `_expt_run_id`")

        if _expt_run_id is not None:
            expt_run = ExperimentRun._get(conn, _expt_run_id=_expt_run_id)
            if expt_run is not None:
                print("set existing ExperimentRun: {}".format(expt_run.name))
            else:
                raise ValueError("ExperimentRun with ID {} not found".format(_expt_run_id))
        elif None not in (proj_id, expt_id):
            if expt_run_name is None:
                expt_run_name = ExperimentRun._generate_default_name()
            try:
                expt_run = ExperimentRun._create(conn, proj_id, expt_id, expt_run_name, desc, tags, attrs, date_created=date_created)
            except requests.HTTPError as e:
                if e.response.status_code == 409:  # already exists
                    if any(param is not None for param in (desc, tags, attrs)):
                        warnings.warn("ExperimentRun with name {} already exists;"
                                      " cannot initialize `desc`, `tags`, or `attrs`".format(expt_run_name))
                    expt_run = ExperimentRun._get(conn, expt_id, expt_run_name)
                    if expt_run is not None:
                        print("set existing ExperimentRun: {}".format(expt_run.name))
                    else:
                        raise RuntimeError("unable to retrieve ExperimentRun {};"
                                           " please notify the Verta development team".format(expt_run_name))
                else:
                    raise e
            else:
                print("created new ExperimentRun: {}".format(expt_run.name))
        else:
            raise ValueError("insufficient arguments")

        super(ExperimentRun, self).__init__(conn, conf, _ExperimentRunService, "experiment-run", expt_run.id)

    def __repr__(self):
        run_msg = self._get_self_as_msg()
        return '\n'.join((
            "name: {}".format(run_msg.name),
            "url: {}://{}/{}/projects/{}/exp-runs/{}".format(self._conn.scheme, self._conn.socket, self.workspace, run_msg.project_id, self.id),
            "date created: {}".format(_utils.timestamp_to_str(int(run_msg.date_created))),
            "date updated: {}".format(_utils.timestamp_to_str(int(run_msg.date_updated))),
            "description: {}".format(run_msg.description),
            "tags: {}".format(run_msg.tags),
            "attributes: {}".format(_utils.unravel_key_values(run_msg.attributes)),
            "id: {}".format(run_msg.id),
            "experiment id: {}".format(run_msg.experiment_id),
            "project id: {}".format(run_msg.project_id),
            "hyperparameters: {}".format(_utils.unravel_key_values(run_msg.hyperparameters)),
            "observations: {}".format(_utils.unravel_observations(run_msg.observations)),
            "metrics: {}".format(_utils.unravel_key_values(run_msg.metrics)),
            "artifact keys: {}".format(_utils.unravel_artifacts(run_msg.artifacts)),
        ))

    @property
    def workspace(self):
        proj_id = self._get_self_as_msg().project_id
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/modeldb/project/getProjectById".format(self._conn.scheme, self._conn.socket),
            self._conn, params={'id': proj_id},
        )
        _utils.raise_for_http_error(response)

        project_json = _utils.body_to_json(response)['project']
        if 'workspace_id' not in project_json:
            # workspace is OSS default
            return _OSS_DEFAULT_WORKSPACE
        else:
            workspace_id = project_json['workspace_id']
        # try getting organization
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/uac-proxy/organization/getOrganizationById".format(self._conn.scheme, self._conn.socket),
            self._conn, params={'org_id': workspace_id},
        )
        try:
            _utils.raise_for_http_error(response)
        except requests.HTTPError:
            # try getting user
            response = _utils.make_request(
                "GET",
                "{}://{}/api/v1/uac-proxy/uac/getUser".format(self._conn.scheme, self._conn.socket),
                self._conn, params={'user_id': workspace_id},
            )
            _utils.raise_for_http_error(response)

            # workspace is user
            return _utils.body_to_json(response)['verta_info']['username']
        else:
            # workspace is organization
            return _utils.body_to_json(response)['organization']['name']

    @property
    def name(self):
        return self._get_self_as_msg().name

    @staticmethod
    def _generate_default_name():
        return "Run {}".format(_utils.generate_default_name())

    @staticmethod
    def _get(conn, expt_id=None, expt_run_name=None, _expt_run_id=None):
        if _expt_run_id is not None:
            Message = _ExperimentRunService.GetExperimentRunById
            msg = Message(id=_expt_run_id)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunById".format(conn.scheme, conn.socket),
                                           conn, params=data)
        elif None not in (expt_id, expt_run_name):
            Message = _ExperimentRunService.GetExperimentRunByName
            msg = Message(experiment_id=expt_id, name=expt_run_name)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("GET",
                                           "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunByName".format(conn.scheme, conn.socket),
                                           conn, params=data)
        else:
            raise ValueError("insufficient arguments")

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            expt_run = response_msg.experiment_run

            if not expt_run.id:  # 200, but empty message
                raise RuntimeError("unable to retrieve ExperimentRun {};"
                                   " please notify the Verta development team".format(expt_run_name))

            return expt_run
        else:
            if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                    or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                return None
            else:
                _utils.raise_for_http_error(response)

    @staticmethod
    def _create(conn, proj_id, expt_id, expt_run_name, desc=None, tags=None, attrs=None, date_created=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        Message = _ExperimentRunService.CreateExperimentRun
        msg = Message(project_id=proj_id, experiment_id=expt_id, name=expt_run_name,
                      description=desc, tags=tags, attributes=attrs,
                      date_created=date_created, date_updated=date_created)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/createExperimentRun".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            return response_msg.experiment_run
        else:
            _utils.raise_for_http_error(response)

    # TODO: use this throughout `ExperimentRun`
    def _get_self_as_msg(self):
        """
        Gets the full protobuf message representation of this Experiment Run.

        Returns
        -------
        run_msg : ExperimentRun protobuf message

        """
        Message = _ExperimentRunService.GetExperimentRunById
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        url = "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunById".format(
            self._conn.scheme,
            self._conn.socket,
        )

        response = _utils.make_request("GET", url, self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.experiment_run

    def _log_artifact(self, key, artifact, artifact_type, extension=None, method=None, overwrite=False):
        """
        Logs an artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact : str or file-like or object
            Artifact or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        artifact_type : int
            Variant of `_CommonCommonService.ArtifactTypeEnum`.
        extension : str, optional
            Filename extension associated with the artifact.
        method : str, optional
            Serialization method used to produce the bytestream, if `artifact` was already serialized by verta.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        if isinstance(artifact, six.string_types):
            os.path.expanduser(artifact)
            artifact = open(artifact, 'rb')

        if hasattr(artifact, 'read') and method is not None:  # already a verta-produced stream
            artifact_stream = artifact
        else:
            artifact_stream, method = _artifact_utils.ensure_bytestream(artifact)

        if extension is None:
            extension = _artifact_utils.ext_from_method(method)

        # calculate checksum
        artifact_hash = _artifact_utils.calc_sha256(artifact_stream)
        artifact_stream.seek(0)

        # determine basename
        #     The key might already contain the file extension, thanks to our hard-coded deployment
        #     keys e.g. "model.pkl" and "model_api.json".
        if extension is None:
            basename = key
        elif key.endswith(os.extsep + extension):
            basename = key
        else:
            basename = key + os.extsep + extension

        # build upload path from checksum and basename
        artifact_path = os.path.join(artifact_hash, basename)

        # TODO: incorporate into config
        VERTA_ARTIFACT_DIR = os.environ.get('VERTA_ARTIFACT_DIR', "")
        VERTA_ARTIFACT_DIR = os.path.expanduser(VERTA_ARTIFACT_DIR)
        if VERTA_ARTIFACT_DIR:
            print("set artifact directory from environment:")
            print("    " + VERTA_ARTIFACT_DIR)
            artifact_path = os.path.join(VERTA_ARTIFACT_DIR, artifact_path)
            pathlib2.Path(artifact_path).parent.mkdir(parents=True, exist_ok=True)

        # log key to ModelDB
        Message = _ExperimentRunService.LogArtifact
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=artifact_path,
                                               path_only=True if VERTA_ARTIFACT_DIR else False,
                                               artifact_type=artifact_type,
                                               filename_extension=extension)
        msg = Message(id=self.id, artifact=artifact_msg)
        data = _utils.proto_to_json(msg)
        if overwrite:
            response = _utils.make_request("DELETE",
                                           "{}://{}/api/v1/modeldb/experiment-run/deleteArtifact".format(self._conn.scheme, self._conn.socket),
                                           self._conn, json={'id': self.id, 'key': key})
            _utils.raise_for_http_error(response)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logArtifact".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("artifact with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

        if VERTA_ARTIFACT_DIR:
            print("logging artifact")
            with open(artifact_path, 'wb') as f:
                shutil.copyfileobj(artifact_stream, f)
            print("log complete; file written to {}".format(artifact_path))
        else:
            self._upload_artifact(key, artifact_stream)

    def _upload_artifact(self, key, artifact_stream, part_size=64*(10**6)):
        """
        Uploads `artifact_stream` to ModelDB artifact store.

        Parameters
        ----------
        key : str
        artifact_stream : file-like
        part_size : int, default 64 MB
            If using multipart upload, number of bytes to upload per part.

        """
        artifact_stream.seek(0)
        if self._conf.debug:
            print("[DEBUG] uploading {} bytes ({})".format(_artifact_utils.get_stream_length(artifact_stream), key))
            artifact_stream.seek(0)

        # check if multipart upload ok
        url_for_artifact = self._get_url_for_artifact(key, "PUT", part_num=1)

        if url_for_artifact.multipart_upload_ok:
            # TODO: parallelize this
            file_parts = iter(lambda: artifact_stream.read(part_size), b'')
            for part_num, file_part in enumerate(file_parts, start=1):
                print("uploading part {}".format(part_num), end='\r')

                # get presigned URL
                url = self._get_url_for_artifact(key, "PUT", part_num=part_num).url

                # wrap file part into bytestream to avoid OverflowError
                #     Passing a bytestring >2 GB (num bytes > max val of int32) directly to
                #     ``requests`` will overwhelm CPython's SSL lib when it tries to sign the
                #     payload. But passing a buffered bytestream instead of the raw bytestring
                #     indicates to ``requests`` that it should perform a streaming upload via
                #     HTTP/1.1 chunked transfer encoding and avoid this issue.
                #     https://github.com/psf/requests/issues/2717
                part_stream = six.BytesIO(file_part)

                # upload part
                #     Retry connection errors, to make large multipart uploads more robust.
                for _ in range(3):
                    try:
                        response = _utils.make_request("PUT", url, self._conn, data=part_stream)
                    except requests.ConnectionError:  # e.g. broken pipe
                        time.sleep(1)
                        continue  # try again
                    else:
                        break
                response.raise_for_status()

                # commit part
                url = "{}://{}/api/v1/modeldb/experiment-run/commitArtifactPart".format(
                    self._conn.scheme,
                    self._conn.socket,
                )
                msg = _CommonService.CommitArtifactPart(id=self.id, key=key)
                msg.artifact_part.part_number = part_num
                msg.artifact_part.etag = response.headers['ETag']
                data = _utils.proto_to_json(msg)
                # TODO: increase retries
                response = _utils.make_request("POST", url, self._conn, json=data)
                _utils.raise_for_http_error(response)
            print()

            # complete upload
            url = "{}://{}/api/v1/modeldb/experiment-run/commitMultipartArtifact".format(
                self._conn.scheme,
                self._conn.socket,
            )
            msg = _CommonService.CommitMultipartArtifact(id=self.id, key=key)
            data = _utils.proto_to_json(msg)
            response = _utils.make_request("POST", url, self._conn, json=data)
            _utils.raise_for_http_error(response)
        else:
            # upload full artifact
            response = _utils.make_request("PUT", url_for_artifact.url, self._conn, data=artifact_stream)
            _utils.raise_for_http_error(response)

        print("upload complete ({})".format(key))

    def _log_artifact_path(self, key, artifact_path, artifact_type):
        """
        Logs the filesystem path of an artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact_path : str
            Filesystem path of the artifact.
        artifact_type : int
            Variant of `_CommonCommonService.ArtifactTypeEnum`.
        """
        # log key-path to ModelDB
        Message = _ExperimentRunService.LogArtifact
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=artifact_path,
                                               path_only=True,
                                               artifact_type=artifact_type)
        msg = Message(id=self.id, artifact=artifact_msg)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logArtifact".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("artifact with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

    def _get_artifact(self, key):
        """
        Gets the artifact with name `key` from this Experiment Run.

        If the artifact was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the artifact object will be returned.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path or bytes representing the artifact.
        bool
            True if the artifact was only logged as its filesystem path.

        """
        # get key-path from ModelDB
        Message = _CommonService.GetArtifacts
        msg = Message(id=self.id, key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        artifact = {artifact.key: artifact for artifact in response_msg.artifacts}.get(key)
        if artifact is None:
            raise KeyError("no artifact found with key {}".format(key))
        if artifact.path_only:
            return artifact.path, artifact.path_only
        else:
            # download artifact from artifact store
            url = self._get_url_for_artifact(key, "GET").url

            response = _utils.make_request("GET", url, self._conn)
            _utils.raise_for_http_error(response)

            return response.content, artifact.path_only

    # TODO: Fix up get dataset to handle the Dataset class when logging dataset
    # version
    def _get_dataset(self, key):
        """
        Gets the dataset with name `key` from this Experiment Run.

        If the dataset was originally logged as just a filesystem path, that path will be returned.
        Otherwise, bytes representing the dataset object will be returned.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path or bytes representing the artifact.
        bool
            True if the artifact was only logged as its filesystem path.

        """
        # get key-path from ModelDB
        Message = _ExperimentRunService.GetDatasets
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getDatasets".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        dataset = {dataset.key: dataset for dataset in response_msg.datasets}.get(key)
        if dataset is None:
            # may be old artifact-based dataset
            try:
                dataset, path_only = self._get_artifact(key)
            except KeyError:
                six.raise_from(KeyError("no dataset found with key {}".format(key)),
                               None)
            else:
                return dataset, path_only, None
        else:
            return dataset.path, dataset.path_only, dataset.linked_artifact_id

    def clone(self, copy_artifacts=False, copy_code_version=False, copy_datasets=False):
        """
        Returns a newly-created copy of this Experiment Run.

        Parameters
        ----------
        copy_artifacts : bool, default False
            Whether to also copy this Experiment Run's artifacts.
        copy_code_version : bool, default False
            Whether to also copy this Experiment Run's code version.
        copy_datasets : bool, default False
            Whether to also copy this Experiment Run's dataset versions.

        Returns
        -------
        :class:`ExperimentRun`

        """
        # get info for the current run
        current_run = self._get(self._conn, _expt_run_id=self.id)

        # clone the current run
        Message = _ExperimentRunService.CreateExperimentRun
        msg = Message(
            project_id=current_run.project_id,
            experiment_id=current_run.experiment_id,
            name=ExperimentRun._generate_default_name(),
            description=current_run.description,
            tags=current_run.tags,
            attributes=current_run.attributes,
            observations=current_run.observations,
            metrics=current_run.metrics,
            hyperparameters=current_run.hyperparameters,
            parent_id=current_run.parent_id,
        )

        msg_artifact = Message()
        msg_code_version = Message()
        msg_datasets = Message()

        if copy_artifacts:
            msg_artifact = Message(
                artifacts=current_run.artifacts,
            )

        if copy_code_version:
            msg_code_version = Message(
                code_version_snapshot=current_run.code_version_snapshot,
            )

        if copy_datasets:
            msg_datasets = Message(
                datasets=current_run.datasets,
            )

        msg.MergeFrom(msg_artifact)
        msg.MergeFrom(msg_code_version)
        msg.MergeFrom(msg_datasets)

        # create the new run
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/createExperimentRun".format(
                                           self._conn.scheme, self._conn.socket), self._conn, json=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        new_run_msg = response_msg.experiment_run
        print("created new ExperimentRun: {}".format(new_run_msg.name))
        new_run = ExperimentRun(self._conn, self._conf, _expt_run_id=new_run_msg.id)

        return new_run

    def get_date_created(self):
        """
        Gets a timestamp representing the time (in UTC) this Experiment Run was created.

        Returns
        -------
        timestamp : int
            Unix timestamp in milliseconds.

        """
        run_msg = self._get_self_as_msg()
        return int(run_msg.date_created)

    def get_date_updated(self):
        """
        Gets a timestamp representing the time (in UTC) this Experiment Run was updated.

        Returns
        -------
        timestamp : int
            Unix timestamp in milliseconds.

        """
        run_msg = self._get_self_as_msg()
        return int(run_msg.date_updated)

    def log_tag(self, tag):
        """
        Logs a tag to this Experiment Run.

        Parameters
        ----------
        tag : str
            Tag.

        """
        if not isinstance(tag, six.string_types):
            raise TypeError("`tag` must be a string")

        Message = _ExperimentRunService.AddExperimentRunTags
        msg = Message(id=self.id, tags=[tag])
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

    def log_tags(self, tags):
        """
        Logs multiple tags to this Experiment Run.

        Parameters
        ----------
        tags : list of str
            Tags.

        """
        tags = _utils.as_list_of_str(tags)

        Message = _ExperimentRunService.AddExperimentRunTags
        msg = Message(id=self.id, tags=tags)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/addExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

    def get_tags(self):
        """
        Gets all tags from this Experiment Run.

        Returns
        -------
        list of str
            All tags.

        """
        Message = _CommonService.GetTags
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getExperimentRunTags".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.tags

    def log_attribute(self, key, value):
        """
        Logs an attribute to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the attribute.
        value : one of {None, bool, float, int, str, list, dict}
            Value of the attribute.

        """
        _utils.validate_flat_key(key)

        attribute = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
        msg = _ExperimentRunService.LogAttribute(id=self.id, attribute=attribute)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logAttribute".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("attribute with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

    def log_attributes(self, attributes):
        """
        Logs potentially multiple attributes to this Experiment Run.

        Parameters
        ----------
        attributes : dict of str to {None, bool, float, int, str, list, dict}
            Attributes.

        """
        # validate all keys first
        for key in six.viewkeys(attributes):
            _utils.validate_flat_key(key)

        # build KeyValues
        attribute_keyvals = []
        for key, value in six.viewitems(attributes):
            attribute_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True)))

        msg = _ExperimentRunService.LogAttributes(id=self.id, attributes=attribute_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some attribute with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

    def get_attribute(self, key):
        """
        Gets the attribute with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the attribute.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the attribute.

        """
        _utils.validate_flat_key(key)

        Message = _CommonService.GetAttributes
        msg = Message(id=self.id, attribute_keys=[key])
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        attributes = _utils.unravel_key_values(response_msg.attributes)
        try:
            return attributes[key]
        except KeyError:
            six.raise_from(KeyError("no attribute found with key {}".format(key)), None)

    def get_attributes(self):
        """
        Gets all attributes from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all attributes.

        """
        Message = _CommonService.GetAttributes
        msg = Message(id=self.id, get_all=True)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getAttributes".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return _utils.unravel_key_values(response_msg.attributes)

    def log_metric(self, key, value):
        """
        Logs a metric to this Experiment Run.

        If the metadatum of interest might recur, :meth:`.log_observation` should be used instead.

        Parameters
        ----------
        key : str
            Name of the metric.
        value : one of {None, bool, float, int, str}
            Value of the metric.

        """
        _utils.validate_flat_key(key)

        metric = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        msg = _ExperimentRunService.LogMetric(id=self.id, metric=metric)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logMetric".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("metric with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

    def log_metrics(self, metrics):
        """
        Logs potentially multiple metrics to this Experiment Run.

        Parameters
        ----------
        metrics : dict of str to {None, bool, float, int, str}
            Metrics.

        """
        # validate all keys first
        for key in six.viewkeys(metrics):
            _utils.validate_flat_key(key)

        # build KeyValues
        metric_keyvals = []
        for key, value in six.viewitems(metrics):
            metric_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value)))

        msg = _ExperimentRunService.LogMetrics(id=self.id, metrics=metric_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logMetrics".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some metric with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

    def get_metric(self, key):
        """
        Gets the metric with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the metric.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the metric.

        """
        _utils.validate_flat_key(key)

        Message = _ExperimentRunService.GetMetrics
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getMetrics".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        metrics = _utils.unravel_key_values(response_msg.metrics)
        try:
            return metrics[key]
        except KeyError:
            six.raise_from(KeyError("no metric found with key {}".format(key)), None)

    def get_metrics(self):
        """
        Gets all metrics from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all metrics.

        """
        Message = _ExperimentRunService.GetMetrics
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getMetrics".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return _utils.unravel_key_values(response_msg.metrics)

    def log_hyperparameter(self, key, value):
        """
        Logs a hyperparameter to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the hyperparameter.
        value : one of {None, bool, float, int, str}
            Value of the hyperparameter.

        """
        _utils.validate_flat_key(key)

        hyperparameter = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        msg = _ExperimentRunService.LogHyperparameter(id=self.id, hyperparameter=hyperparameter)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logHyperparameter".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("hyperparameter with key {} already exists;"
                                 " consider using observations instead".format(key))
            else:
                _utils.raise_for_http_error(response)

    def log_hyperparameters(self, hyperparams):
        """
        Logs potentially multiple hyperparameters to this Experiment Run.

        Parameters
        ----------
        hyperparameters : dict of str to {None, bool, float, int, str}
            Hyperparameters.

        """
        # validate all keys first
        for key in six.viewkeys(hyperparams):
            _utils.validate_flat_key(key)

        # build KeyValues
        hyperparameter_keyvals = []
        for key, value in six.viewitems(hyperparams):
            hyperparameter_keyvals.append(_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value)))

        msg = _ExperimentRunService.LogHyperparameters(id=self.id, hyperparameters=hyperparameter_keyvals)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logHyperparameters".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("some hyperparameter with some input key already exists;"
                                 " consider using observations instead")
            else:
                _utils.raise_for_http_error(response)

    def get_hyperparameter(self, key):
        """
        Gets the hyperparameter with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the hyperparameter.

        Returns
        -------
        one of {None, bool, float, int, str}
            Value of the hyperparameter.

        """
        _utils.validate_flat_key(key)

        Message = _ExperimentRunService.GetHyperparameters
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getHyperparameters".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        hyperparameters = _utils.unravel_key_values(response_msg.hyperparameters)
        try:
            return hyperparameters[key]
        except KeyError:
            six.raise_from(KeyError("no hyperparameter found with key {}".format(key)), None)

    def get_hyperparameters(self):
        """
        Gets all hyperparameters from this Experiment Run.

        Returns
        -------
        dict of str to {None, bool, float, int, str}
            Names and values of all hyperparameters.

        """
        Message = _ExperimentRunService.GetHyperparameters
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getHyperparameters".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return _utils.unravel_key_values(response_msg.hyperparameters)

    def log_dataset(self, key, dataset, overwrite=False):
        """
        Alias for :meth:`~ExperimentRun.log_dataset_version`.

        .. deprecated:: 0.14.12
            ``log_dataset()`` can no longer be used to log artifacts.
            :meth:`~ExperimentRun.log_artifact` should be used instead.

        """
        if isinstance(dataset, _dataset.Dataset):
            raise TypeError(
                "directly logging a Dataset is not supported;"
                " please create a DatasetVersion for logging"
            )

        if not isinstance(dataset, _dataset.DatasetVersion):
            raise TypeError(
                "`dataset` must be of type DatasetVersion;"
                " to log an artifact, consider using run.log_artifact() instead"
            )

        self.log_dataset_version(key=key, dataset_version=dataset, overwrite=overwrite)

    def log_dataset_version(self, key, dataset_version, overwrite=False):
        """
        Logs a Verta DatasetVersion to this ExperimentRun with the given key.

        Parameters
        ----------
        key : str
        dataset_version : :class:`~verta._dataset.DatasetVersion`
        overwrite : bool, default False
            Whether to allow overwriting a dataset version.

        """
        if not isinstance(dataset_version, _dataset.DatasetVersion):
            raise TypeError("`dataset_version` must be of type DatasetVersion")

        # TODO: hack because path_only artifact needs a placeholder path
        dataset_path = "See attached dataset version"

        # log key-path to ModelDB
        Message = _ExperimentRunService.LogDataset
        artifact_msg = _CommonCommonService.Artifact(key=key,
                                               path=dataset_path,
                                               path_only=True,
                                               artifact_type=_CommonCommonService.ArtifactTypeEnum.DATA,
                                               linked_artifact_id=dataset_version.id)
        msg = Message(id=self.id, dataset=artifact_msg, overwrite=overwrite)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logDataset".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        if not response.ok:
            if response.status_code == 409:
                raise ValueError("dataset with key {} already exists;"
                                 " consider setting overwrite=True".format(key))
            else:
                _utils.raise_for_http_error(response)

    def log_dataset_path(self, key, path):
        """
        Logs the filesystem path of an dataset to this Experiment Run.

        .. deprecated:: 0.13.0
           The `log_dataset_path()` method will removed in v0.15.0; consider using
           `client.set_dataset(…, type="local")` and `run.log_dataset_version()` instead.

        This function makes no attempt to open a file at `dataset_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the dataset.
        dataset_path : str
            Filesystem path of the dataset.

        """
        _utils.validate_flat_key(key)

        warnings.warn("`log_dataset_path()` is deprecated and will removed in a later version;"
                      " consider using `client.set_dataset(..., type=\"local\")`"
                      " and `run.log_dataset_version()` instead",
                      category=FutureWarning)

        # create impromptu DatasetVersion
        dataset = _dataset.LocalDataset(self._conn, self._conf, name=key)
        dataset_version = dataset.create_version(path=path)

        self.log_dataset_version(key, dataset_version)

    def get_dataset(self, key):
        """
        Gets the dataset artifact with name `key` from this Experiment Run.

        If the dataset was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the dataset object itself will be returned. If the object is unable to be
        deserialized, the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the dataset.

        Returns
        -------
        str or object or file-like
            DatasetVersion if logged using :meth:`log_dataset_version()`.
            Filesystem path of the dataset, the dataset object, or a bytestream representing the
            dataset.

        """
        dataset, path_only, linked_id = self._get_dataset(key)
        if path_only:
            if linked_id:
                return _dataset.DatasetVersion(self._conn, self._conf, _dataset_version_id=linked_id)
            else:
                return dataset
        else:
            # TODO: may need to be updated for raw
            try:
                return pickle.loads(dataset)
            except pickle.UnpicklingError:
                return six.BytesIO(dataset)

    def get_dataset_version(self, key):
        """
        Gets the DatasetVersion with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the dataset version.

        Returns
        -------
        DatasetVersion
            DatasetVersion associated with the given key.

        """
        return self.get_dataset(key)

    def log_model_for_deployment(self, model, model_api, requirements, train_features=None, train_targets=None):
        """
        Logs a model artifact, a model API, requirements, and a dataset CSV to deploy on Verta.

        .. deprecated:: 0.13.13
           This function has been superseded by :meth:`log_model`, :meth:`log_requirements`, and
           :meth:`log_training_data`; consider using them instead.

        Parameters
        ----------
        model : str or file-like or object
            Model or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        model_api : str or file-like
            Model API, specifying model deployment and predictions.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
        requirements : str or file-like
            pip requirements file specifying packages necessary to deploy the model.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
        train_features : pd.DataFrame, optional
            pandas DataFrame representing features of the training data. If provided, `train_targets`
            must also be provided.
        train_targets : pd.DataFrame, optional
            pandas DataFrame representing targets of the training data. If provided, `train_features`
            must also be provided.

        Warnings
        --------
        Due to the way deployment currently works, `train_features` and `train_targets` will be joined
        together and then converted into a CSV. Retrieving the dataset through the Client will return
        a file-like bytestream of this CSV that can be passed directly into `pd.read_csv()
        <https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_csv.html>`_.

        """
        if sum(arg is None for arg in (train_features, train_targets)) == 1:
            raise ValueError("`train_features` and `train_targets` must be provided together")

        # open files
        if isinstance(model, six.string_types):
            model = open(model, 'rb')
        if isinstance(model_api, six.string_types):
            model_api = open(model_api, 'rb')
        if isinstance(requirements, six.string_types):
            requirements = open(requirements, 'rb')

        # prehandle model
        if self._conf.debug:
            if not hasattr(model, 'read'):
                print("[DEBUG] model is type: {}".format(model.__class__))
        _artifact_utils.reset_stream(model)  # reset cursor to beginning in case user forgot
        # obtain serialized model and info
        try:
            model_extension = _artifact_utils.get_file_ext(model)
        except (TypeError, ValueError):
            model_extension = None
        # serialize model
        _utils.THREAD_LOCALS.active_experiment_run = self
        try:
            model, method, model_type = _artifact_utils.serialize_model(model)
        finally:
            _utils.THREAD_LOCALS.active_experiment_run = None
        # check serialization method
        if method is None:
            raise ValueError("will not be able to deploy model due to unknown serialization method")
        if model_extension is None:
            model_extension = _artifact_utils.ext_from_method(method)

        # prehandle model_api
        _artifact_utils.reset_stream(model_api)  # reset cursor to beginning in case user forgot
        model_api = utils.ModelAPI.from_file(model_api)
        if 'model_packaging' not in model_api:
            # add model serialization info to model_api
            model_api['model_packaging'] = {
                'python_version': _utils.get_python_version(),
                'type': model_type,
                'deserialization': method,
            }
        if self._conf.debug:
            print("[DEBUG] model API is:")
            pprint.pprint(model_api.to_dict())

        # handle requirements
        _artifact_utils.reset_stream(requirements)  # reset cursor to beginning in case user forgot
        req_deps = six.ensure_str(requirements.read()).splitlines()  # get list repr of reqs
        _artifact_utils.reset_stream(requirements)  # reset cursor to beginning as a courtesy
        try:
            self.log_requirements(req_deps)
        except ValueError as e:
            if "artifact with key requirements.txt already exists" in e.args[0]:
                print("requirements.txt already logged; skipping")
            else:
                six.raise_from(e, None)

        # prehandle train_features and train_targets
        if train_features is not None and train_targets is not None:
            stringstream = six.StringIO()
            train_df = train_features.join(train_targets)
            train_df.to_csv(stringstream, index=False)  # write as CSV
            stringstream.seek(0)
            train_data = stringstream
        else:
            train_data = None

        self._log_artifact("model.pkl", model, _CommonCommonService.ArtifactTypeEnum.MODEL, model_extension, method)
        self._log_artifact("model_api.json", model_api, _CommonCommonService.ArtifactTypeEnum.BLOB, 'json')
        if train_data is not None:
            self._log_artifact("train_data", train_data, _CommonCommonService.ArtifactTypeEnum.DATA, 'csv')

    def log_tf_saved_model(self, export_dir):
        with tempfile.TemporaryFile() as tempf:
            with zipfile.ZipFile(tempf, 'w') as zipf:
                for root, _, files in os.walk(export_dir):
                    for filename in files:
                        filepath = os.path.join(root, filename)
                        zipf.write(filepath, os.path.relpath(filepath, export_dir))
            tempf.seek(0)
            # TODO: change _log_artifact() to not read file into memory
            self._log_artifact("tf_saved_model", tempf, _CommonCommonService.ArtifactTypeEnum.BLOB, 'zip')

    def log_model(self, model, custom_modules=None, model_api=None, artifacts=None, overwrite=False):
        """
        Logs a model artifact for Verta model deployment.

        Parameters
        ----------
        model : str or object
            Model for deployment.
                - If str, then it will be interpreted as a filesystem path to a serialized model file
                  for upload.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        custom_modules : list of str, optional
            Paths to local Python modules and other files that the deployed model depends on.
                - If directories are provided, all files within—excluding virtual environments—will
                  be included.
                - If not provided, all Python files located within `sys.path`—excluding virtual
                  environments—will be included.
        model_api : :class:`~utils.ModelAPI`, optional
            Model API specifying details about the model and its deployment.
        artifacts : list of str, optional
            Keys of logged artifacts to be used by a class model.
        overwrite : bool, default False
            Whether to allow overwriting existing artifacts.

        """
        if (artifacts is not None
                and not (isinstance(artifacts, list)
                         and all(isinstance(artifact_key, six.string_types) for artifact_key in artifacts))):
            raise TypeError("`artifacts` must be list of str, not {}".format(type(artifacts)))

        # validate that `artifacts` are actually logged
        if artifacts:
            run_msg = self._get_self_as_msg()
            existing_artifact_keys = {artifact.key for artifact in run_msg.artifacts}
            unlogged_artifact_keys = set(artifacts) - existing_artifact_keys
            if unlogged_artifact_keys:
                raise ValueError("`artifacts` contains keys that have not been logged: {}".format(sorted(unlogged_artifact_keys)))

        # serialize model
        try:
            extension = _artifact_utils.get_file_ext(model)
        except (TypeError, ValueError):
            extension = None
        _utils.THREAD_LOCALS.active_experiment_run = self
        try:
            serialized_model, method, model_type = _artifact_utils.serialize_model(model)
        finally:
            _utils.THREAD_LOCALS.active_experiment_run = None
        if method is None:
            raise ValueError("will not be able to deploy model due to unknown serialization method")
        if extension is None:
            extension = _artifact_utils.ext_from_method(method)
        if self._conf.debug:
            print("[DEBUG] model is type {}".format(model_type))

        if artifacts and model_type != "class":
            raise ValueError("`artifacts` can only be provided if `model` is a class")

        # build model API
        if model_api is None:
            model_api = utils.ModelAPI()
        elif not isinstance(model_api, utils.ModelAPI):
            raise ValueError("`model_api` must be `verta.utils.ModelAPI`, not {}".format(type(model_api)))
        if 'model_packaging' not in model_api:
            # add model serialization info to model_api
            model_api['model_packaging'] = {
                'python_version': _utils.get_python_version(),
                'type': model_type,
                'deserialization': method,
            }
        if self._conf.debug:
            print("[DEBUG] model API is:")
            pprint.pprint(model_api.to_dict())

        # associate artifact dependencies
        if artifacts:
            self.log_attribute(_MODEL_ARTIFACTS_ATTR_KEY, artifacts)

        self._log_modules(custom_modules, overwrite=overwrite)
        self._log_artifact("model.pkl", serialized_model, _CommonCommonService.ArtifactTypeEnum.MODEL, extension, method, overwrite=overwrite)
        self._log_artifact("model_api.json", model_api, _CommonCommonService.ArtifactTypeEnum.BLOB, 'json', overwrite=overwrite)

    def get_model(self):
        """
        Gets the model artifact for Verta model deployment from this Experiment Run.

        Returns
        -------
        object
            Model for deployment.

        """
        model, _ = self._get_artifact("model.pkl")
        return _artifact_utils.deserialize_model(model)

    def log_image(self, key, image, overwrite=False):
        """
        Logs a image artifact to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the image.
        image : one of {str, file-like, pyplot, matplotlib Figure, PIL Image, object}
            Image or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - If matplotlib pyplot, then the image will be serialized and uploaded as an artifact.
                - If matplotlib Figure, then the image will be serialized and uploaded as an artifact.
                - If PIL Image, then the image will be serialized and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        overwrite : bool, default False
            Whether to allow overwriting an existing image with key `key`.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        # convert pyplot, Figure or Image to bytestream
        bytestream, extension = six.BytesIO(), 'png'
        try:  # handle matplotlib
            image.savefig(bytestream, format=extension)
        except AttributeError:
            try:  # handle PIL Image
                colors = image.getcolors()
            except AttributeError:
                try:
                    extension = _artifact_utils.get_file_ext(image)
                except (TypeError, ValueError):
                    extension = None
            else:
                if len(colors) == 1 and all(val == 255 for val in colors[0][1]):
                    warnings.warn("the image being logged is blank")
                image.save(bytestream, extension)

        bytestream.seek(0)
        if bytestream.read(1):
            bytestream.seek(0)
            image = bytestream

        self._log_artifact(key, image, _CommonCommonService.ArtifactTypeEnum.IMAGE, extension, overwrite=overwrite)

    def log_image_path(self, key, image_path):
        """
        Logs the filesystem path of an image to this Experiment Run.

        This function makes no attempt to open a file at `image_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the image.
        image_path : str
            Filesystem path of the image.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        self._log_artifact_path(key, image_path, _CommonCommonService.ArtifactTypeEnum.IMAGE)

    def get_image(self, key):
        """
        Gets the image artifact with name `key` from this Experiment Run.

        If the image was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the image object will be returned. If the object is unable to be deserialized,
        the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the image.

        Returns
        -------
        str or PIL Image or file-like
            Filesystem path of the image, the image object, or a bytestream representing the image.

        """
        image, path_only = self._get_artifact(key)
        if path_only:
            return image
        else:
            if PIL is None:  # Pillow not installed
                return six.BytesIO(image)
            try:
                return PIL.Image.open(six.BytesIO(image))
            except IOError:  # can't be handled by Pillow
                return six.BytesIO(image)

    def log_artifact(self, key, artifact, overwrite=False):
        """
        Logs an artifact to this Experiment Run.

        The ``VERTA_ARTIFACT_DIR`` environment variable can be used to specify a locally-accessible
        directory to store artifacts.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact : str or file-like or object
            Artifact or some representation thereof.
                - If str, then it will be interpreted as a filesystem path, its contents read as bytes,
                  and uploaded as an artifact. If it is a directory path, its contents will be zipped.
                - If file-like, then the contents will be read as bytes and uploaded as an artifact.
                - Otherwise, the object will be serialized and uploaded as an artifact.
        overwrite : bool, default False
            Whether to allow overwriting an existing artifact with key `key`.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        try:
            extension = _artifact_utils.get_file_ext(artifact)
        except (TypeError, ValueError):
            extension = None

        # zip if `artifact` is directory path
        if isinstance(artifact, six.string_types) and os.path.isdir(artifact):
            tempf = tempfile.TemporaryFile()

            with zipfile.ZipFile(tempf, 'w') as zipf:
                for root, _, files in os.walk(artifact):
                    for filename in files:
                        filepath = os.path.join(root, filename)
                        zipf.write(filepath, os.path.relpath(filepath, artifact))
            tempf.seek(0)

            artifact = tempf
            extension = 'zip'

        self._log_artifact(key, artifact, _CommonCommonService.ArtifactTypeEnum.BLOB, extension, overwrite=overwrite)

    def log_artifact_path(self, key, artifact_path):
        """
        Logs the filesystem path of an artifact to this Experiment Run.

        This function makes no attempt to open a file at `artifact_path`. Only the path string itself
        is logged.

        Parameters
        ----------
        key : str
            Name of the artifact.
        artifact_path : str
            Filesystem path of the artifact.

        """
        _artifact_utils.validate_key(key)
        _utils.validate_flat_key(key)

        self._log_artifact_path(key, artifact_path, _CommonCommonService.ArtifactTypeEnum.BLOB)

    def get_artifact(self, key):
        """
        Gets the artifact with name `key` from this Experiment Run.

        If the artifact was originally logged as just a filesystem path, that path will be returned.
        Otherwise, the artifact object will be returned. If the object is unable to be deserialized,
        the raw bytes are returned instead.

        Parameters
        ----------
        key : str
            Name of the artifact.

        Returns
        -------
        str or bytes
            Filesystem path of the artifact, the artifact object, or a bytestream representing the
            artifact.

        """
        artifact, path_only = self._get_artifact(key)
        if path_only:
            # might be clientside storage
            # NOTE: can cause problem if accidentally picks up unrelated file w/ same name
            if os.path.exists(artifact):
                try:
                    with open(artifact, 'rb') as f:
                        return pickle.load(f)
                except:
                    # return bytestream b/c that's what this fn does with MDB artifacts
                    return open(artifact, 'rb')

            return artifact
        else:
            try:
                return pickle.loads(artifact)
            except:
                return six.BytesIO(artifact)

    def download_artifact(self, key, download_to_path, chunk_size=32*(10**6)):
        """
        Downloads the artifact with name `key` to path `download_to_path`.

        Parameters
        ----------
        key : str
            Name of the artifact.
        download_to_path : str
            Path to download to.
        chunk_size : int, default 32 MB
            Number of bytes to download at a time.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where artifact was downloaded to. Matches `download_to_path`.

        """
        download_to_path = os.path.abspath(download_to_path)

        # get key-path from ModelDB
        # TODO: consolidate the following ~12 lines with ExperimentRun._get_artifact()
        Message = _CommonService.GetArtifacts
        msg = Message(id=self.id, key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        artifact = {artifact.key: artifact for artifact in response_msg.artifacts}.get(key)
        if artifact is None:
            raise KeyError("no artifact found with key {}".format(key))

        # TODO: unpack dirs logged as artifacts
        #     But we can't distinguish if a ZIP artifact is a directory we've compressed, or if it
        #     was a ZIP file the user already had.

        # create parent dirs
        pathlib2.Path(download_to_path).parent.mkdir(parents=True, exist_ok=True)
        # TODO: clean up empty parent dirs if something later fails

        # get a stream of the file bytes, without loading into memory, and write to file
        # TODO: consolidate this with _get_artifact() and get_artifact()
        print("downloading {} from ModelDB".format(key))
        if artifact.path_only:
            if os.path.exists(artifact.path):
                # copy from clientside storage
                shutil.copyfile(artifact.path, download_to_path)
            else:
                raise ValueError(
                    "artifact {} appears to have been logged as path-only,"
                    " and cannot be downloaded".format(key)
                )
        else:
            # download artifact from artifact store
            url = self._get_url_for_artifact(key, "GET").url
            response = _utils.make_request("GET", url, self._conn, stream=True)
            try:
                _utils.raise_for_http_error(response)

                # TODO: use a tempfile first, and also delete if failed
                with open(download_to_path, 'wb') as dest_f:
                    for chunk in response.iter_content(chunk_size=chunk_size):
                        dest_f.write(chunk)
            finally:
                response.close()
        print("download complete; file written to {}".format(download_to_path))

        return download_to_path

    def log_observation(self, key, value, timestamp=None, epoch_num=None):
        """
        Logs an observation to this Experiment Run.

        Parameters
        ----------
        key : str
            Name of the observation.
        value : one of {None, bool, float, int, str}
            Value of the observation.
        timestamp : str or float or int, optional
            String representation of a datetime or numerical Unix timestamp. If not provided, the
            current time will be used.
        epoch_num : non-negative int, optional
            Epoch number associated with this observation. If not provided, it will automatically
            be incremented from prior observations for the same `key`.

        Warnings
        --------
        If `timestamp` is provided by the user, it must contain timezone information. Otherwise,
        it will be interpreted as UTC.

        """
        _utils.validate_flat_key(key)

        if timestamp is None:
            timestamp = _utils.now()
        else:
            timestamp = _utils.ensure_timestamp(timestamp)

        if epoch_num is not None:
            if (not isinstance(epoch_num, six.integer_types)
                    and not (isinstance(epoch_num, float) and epoch_num.is_integer())):
                raise TypeError("`epoch_num` must be int, not {}".format(type(epoch_num)))
            if epoch_num < 0:
                raise ValueError("`epoch_num` must be non-negative")

        attribute = _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value))
        observation = _ExperimentRunService.Observation(attribute=attribute, timestamp=timestamp)  # TODO: support Artifacts
        if epoch_num is not None:
            observation.epoch_number.number_value = epoch_num  # pylint: disable=no-member

        msg = _ExperimentRunService.LogObservation(id=self.id, observation=observation)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment-run/logObservation".format(self._conn.scheme, self._conn.socket),
                                       self._conn, json=data)
        _utils.raise_for_http_error(response)

    def get_observation(self, key):
        """
        Gets the observation series with name `key` from this Experiment Run.

        Parameters
        ----------
        key : str
            Name of observation series.

        Returns
        -------
        list of {None, bool, float, int, str}
            Values of observation series.

        """
        _utils.validate_flat_key(key)

        Message = _ExperimentRunService.GetObservations
        msg = Message(id=self.id, observation_key=key)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getObservations".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        if len(response_msg.observations) == 0:
            raise KeyError("no observation found with key {}".format(key))
        else:
            return [_utils.unravel_observation(observation)[1:]  # drop key from tuple
                    for observation in response_msg.observations]  # TODO: support Artifacts

    def get_observations(self):
        """
        Gets all observations from this Experiment Run.

        Returns
        -------
        dict of str to list of {None, bool, float, int, str}
            Names and values of all observation series.

        """
        run_msg = self._get_self_as_msg()
        return _utils.unravel_observations(run_msg.observations)

    def log_requirements(self, requirements, overwrite=False):
        """
        Logs a pip requirements file for Verta model deployment.

        .. versionadded:: 0.13.13

        Parameters
        ----------
        requirements : str or list of str
            PyPI-installable packages necessary to deploy the model.
                - If str, then it will be interpreted as a filesystem path to a requirements file
                  for upload.
                - If list of str, then it will be interpreted as a list of PyPI package names.
        overwrite : bool, default False
            Whether to allow overwriting existing requirements.

        Raises
        ------
        ValueError
            If a package's name is invalid for PyPI, or its exact version cannot be determined.

        Examples
        --------
        From a file:

        .. code-block:: python

            run.log_requirements("../requirements.txt")
            # upload complete (requirements.txt)
            print(run.get_artifact("requirements.txt").read().decode())
            # cloudpickle==1.2.1
            # jupyter==1.0.0
            # matplotlib==3.1.1
            # pandas==0.25.0
            # scikit-learn==0.21.3
            # verta==0.13.13

        From a list of package names:

        .. code-block:: python

            run.log_requirements(['verta', 'cloudpickle', 'scikit-learn'])
            # upload complete (requirements.txt)
            print(run.get_artifact("requirements.txt").read().decode())
            # verta==0.13.13
            # cloudpickle==1.2.1
            # scikit-learn==0.21.3

        """
        if isinstance(requirements, six.string_types):
            with open(requirements, 'r') as f:
                requirements = _pip_requirements_utils.clean_reqs_file_lines(f.readlines())
        elif (isinstance(requirements, list)
              and all(isinstance(req, six.string_types) for req in requirements)):
            requirements = copy.copy(requirements)
        else:
            raise TypeError("`requirements` must be either str or list of str, not {}".format(type(requirements)))

        _pip_requirements_utils.process_requirements(requirements)

        if self._conf.debug:
            print("[DEBUG] requirements are:")
            print(requirements)

        requirements = six.BytesIO(six.ensure_binary('\n'.join(requirements)))  # as file-like
        self._log_artifact("requirements.txt", requirements, _CommonCommonService.ArtifactTypeEnum.BLOB, 'txt', overwrite=overwrite)

    def log_modules(self, paths, search_path=None):
        """
        Logs local files that are dependencies for a deployed model to this Experiment Run.

        .. deprecated:: 0.13.13
           The behavior of this function has been merged into :meth:`log_model` as its
           ``custom_modules`` parameter; consider using that instead.
        .. deprecated:: 0.12.4
           The `search_path` parameter is no longer necessary and will removed in v0.15.0; consider
           removing it from the function call.

        Parameters
        ----------
        paths : str or list of str
            Paths to local Python modules and other files that the deployed model depends on. If a
            directory is provided, all files within will be included.

        """
        warnings.warn("The behavior of this function has been merged into log_model() as its"
                      " `custom_modules` parameter; consider using that instead",
                      category=FutureWarning)
        if search_path is not None:
            warnings.warn("`search_path` is no longer used and will removed in a later version;"
                          " consider removing it from the function call",
                          category=FutureWarning)

        self._log_modules(paths)

    def _log_modules(self, paths=None, overwrite=False):
        if isinstance(paths, six.string_types):
            paths = [paths]
        if paths is not None:
            paths = list(map(os.path.expanduser, paths))
            paths = list(map(os.path.abspath, paths))

        # collect local sys paths
        local_sys_paths = copy.copy(sys.path)
        ## replace empty first element with cwd
        ##     https://docs.python.org/3/library/sys.html#sys.path
        if local_sys_paths[0] == "":
            local_sys_paths[0] = os.getcwd()
        ## convert to absolute paths
        local_sys_paths = list(map(os.path.abspath, local_sys_paths))
        ## remove paths that don't exist
        local_sys_paths = list(filter(os.path.exists, local_sys_paths))
        ## remove .ipython
        local_sys_paths = list(filter(lambda path: not path.endswith(".ipython"), local_sys_paths))
        ## remove virtual (and real) environments
        def is_in_venv(path):
            """
            Roughly checks for:
                /
                |_ lib/
                |   |_ python*/ <- directory with Python packages, containing `path`
                |
                |_ bin/
                    |_ python*  <- Python executable

            """
            lib_python_str = os.path.join(os.sep, "lib", "python")
            i = path.find(lib_python_str)
            return i != -1 and glob.glob(os.path.join(path[:i], "bin", "python*"))
        local_sys_paths = list(filter(lambda path: not is_in_venv(path), local_sys_paths))

        # get paths to files within
        if paths is None:
            # Python files within filtered sys.path dirs
            paths = local_sys_paths
            extensions = ['py', 'pyc', 'pyo']
        else:
            # all user-specified files
            paths = paths
            extensions = None
        local_filepaths = _utils.find_filepaths(
            paths, extensions=extensions,
            include_hidden=True,
            include_venv=False,  # ignore virtual environments nested within
        )

        # obtain deepest common directory
        #     This directory on the local system will be mirrored in `_CUSTOM_MODULES_DIR` in
        #     deployment.
        curr_dir = os.path.join(os.getcwd(), "")
        paths_plus = list(local_filepaths) + [curr_dir]
        common_prefix = os.path.commonprefix(paths_plus)
        common_dir = os.path.dirname(common_prefix)

        # replace `common_dir` with `_CUSTOM_MODULES_DIR` for deployment sys.path
        depl_sys_paths = list(map(lambda path: os.path.relpath(path, common_dir), local_sys_paths))
        depl_sys_paths = list(map(lambda path: os.path.join(_CUSTOM_MODULES_DIR, path), depl_sys_paths))

        bytestream = six.BytesIO()
        with zipfile.ZipFile(bytestream, 'w') as zipf:
            for filepath in local_filepaths:
                arcname = os.path.relpath(filepath, common_dir)  # filepath relative to archive root
                try:
                    zipf.write(filepath, arcname)
                except:
                    # maybe file has corrupt metadata; try reading then writing contents
                    with open(filepath, 'rb') as f:
                        zipf.writestr(arcname, f.read())

            # add verta config file for sys.path and chdir
            working_dir = os.path.join(_CUSTOM_MODULES_DIR, os.path.relpath(curr_dir, common_dir))
            zipf.writestr(
                "_verta_config.py",
                six.ensure_binary('\n'.join([
                    "import os, sys",
                    "",
                    "",
                    "sys.path = sys.path[:1] + {} + sys.path[1:]".format(depl_sys_paths),
                    "",
                    "try:",
                    "    os.makedirs(\"{}\")".format(working_dir),
                    "except OSError:  # already exists",
                    "    pass",
                    "os.chdir(\"{}\")".format(working_dir),
                ]))
            )

            # add __init__.py
            init_filename = "__init__.py"
            if init_filename not in zipf.namelist():
                zipf.writestr(init_filename, b"")

            if self._conf.debug:
                print("[DEBUG] archive contains:")
                zipf.printdir()
        bytestream.seek(0)

        self._log_artifact("custom_modules", bytestream, _CommonCommonService.ArtifactTypeEnum.BLOB, 'zip', overwrite=overwrite)

    def log_setup_script(self, script, overwrite=False):
        """
        Associate a model deployment setup script with this Experiment Run.

        .. versionadded:: 0.13.8

        Parameters
        ----------
        script : str
            String composed of valid Python code for executing setup steps at the beginning of model
            deployment. An on-disk file can be passed in using ``open("path/to/file.py", 'r').read()``.
        overwrite : bool, default False
            Whether to allow overwriting an existing setup script.

        Raises
        ------
        SyntaxError
            If `script` contains invalid Python.

        """
        # validate `script`'s syntax
        try:
            ast.parse(script)
        except SyntaxError as e:
            # clarify that the syntax error comes from `script`, and propagate details
            reason = e.args[0]
            line_no = e.args[1][1]
            line = script.splitlines()[line_no-1]
            six.raise_from(SyntaxError("{} in provided script on line {}:\n{}"
                                       .format(reason, line_no, line)),
                           e)

        # convert into bytes for upload
        script = six.ensure_binary(script)

        # convert to file-like for `_log_artifact()`
        script = six.BytesIO(script)

        self._log_artifact("setup_script", script, _CommonCommonService.ArtifactTypeEnum.BLOB, 'py', overwrite=overwrite)

    def log_training_data(self, train_features, train_targets, overwrite=False):
        """
        Associate training data with this Experiment Run.

        .. versionchanged:: 0.14.4
           Instead of uploading the data itself as a CSV artifact ``'train_data'``, this method now
           generates a histogram for internal use by our deployment data monitoring system.

        Parameters
        ----------
        train_features : pd.DataFrame
            pandas DataFrame representing features of the training data.
        train_targets : pd.DataFrame or pd.Series
            pandas DataFrame representing targets of the training data.
        overwrite : bool, default False
            Whether to allow overwriting existing training data.

        """
        if train_features.__class__.__name__ != "DataFrame":
            raise TypeError("`train_features` must be a pandas DataFrame, not {}".format(type(train_features)))
        if train_targets.__class__.__name__ == "Series":
            train_targets = train_targets.to_frame()
        elif train_targets.__class__.__name__ != "DataFrame":
            raise TypeError("`train_targets` must be a pandas DataFrame or Series, not {}".format(type(train_targets)))

        # check for overlapping column names
        common_column_names = set(train_features.columns) & set(train_targets.columns)
        if common_column_names:
            raise ValueError("`train_features` and `train_targets` combined have overlapping column names;"
                             " please ensure column names are unique")

        train_df = train_features.join(train_targets)

        histograms = _histogram_utils.calculate_histograms(train_df)

        endpoint = "{}://{}/api/v1/monitoring/data/references/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
        )
        response = _utils.make_request("PUT", endpoint, self._conn, json=histograms)
        _utils.raise_for_http_error(response)

    def fetch_artifacts(self, keys):
        """
        Downloads artifacts that are associated with a class model.

        Parameters
        ----------
        keys : list of str
            Keys of artifacts to download.

        Returns
        -------
        dict of str to str
            Map of artifacts' keys to their cache filepaths—for use as the ``artifacts`` parameter
            to a Verta class model.

        Examples
        --------
        .. code-block:: python

            run.log_artifact("weights", open("weights.npz", 'rb'))
            # upload complete (weights)
            run.log_artifact("text_embeddings", open("embedding.csv", 'rb'))
            # upload complete (text_embeddings)
            artifact_keys = ["weights", "text_embeddings"]
            artifacts = run.fetch_artifacts(artifact_keys)
            artifacts
            # {'weights': '/Users/convoliution/.verta/cache/artifacts/50a9726b3666d99aea8af006cf224a7637d0c0b5febb3b0051192ce1e8615f47/weights.npz',
            #  'text_embeddings': '/Users/convoliution/.verta/cache/artifacts/2d2d1d809e9bce229f0a766126ae75df14cadd1e8f182561ceae5ad5457a3c38/embedding.csv'}
            ModelClass(artifacts=artifacts).predict(["Good book.", "Bad book!"])
            # [0.955998517288053, 0.09809996313422353]
            run.log_model(ModelClass, artifacts=artifact_keys)
            # upload complete (custom_modules.zip)
            # upload complete (model.pkl)
            # upload complete (model_api.json)

        """
        if not (isinstance(keys, list)
                and all(isinstance(key, six.string_types) for key in keys)):
            raise TypeError("`keys` must be list of str, not {}".format(type(keys)))

        # validate that `keys` are actually logged
        run_msg = self._get_self_as_msg()
        existing_artifact_keys = {artifact.key for artifact in run_msg.artifacts}
        unlogged_artifact_keys = set(keys) - existing_artifact_keys
        if unlogged_artifact_keys:
            raise ValueError("`keys` contains keys that have not been logged: {}".format(sorted(unlogged_artifact_keys)))

        # get artifact checksums
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment-run/getArtifacts".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params={'id': self.id})
        _utils.raise_for_http_error(response)
        paths = {artifact['key']: artifact['path']
                 for artifact in _utils.body_to_json(response)['artifacts']}

        artifacts = dict()
        for key in keys:
            filename = os.path.join("artifacts", paths[key])

            # check cache, otherwise write to cache
            #     "try-get-then-create" can lead multiple threads trying to write to the cache
            #     simultaneously, but artifacts being cached at a particular location should be
            #     identical, so multiple writes would be idempotent.
            path = self._get_cached(filename)
            if path is None:
                contents, _ = self._get_artifact(key)  # TODO: raise error if path_only
                path = self._cache(filename, contents)

            artifacts.update({key: path})

        return artifacts

    def get_deployment_status(self):
        """
        Returns the current status of the model deployment associated with this Experiment Run.

        .. versionadded:: 0.13.17

        Returns
        -------
        status : dict
            - ``'status'`` (`str`) – Current status of the model deployment.
            - (if deployed) ``'url'`` (`str`) – Prediction endpoint URL.
            - (if deployed) ``'token'`` (`str or None`) – Token for authorizing prediction requests.
            - (if error during deployment) ``'message'`` (`str`) – Error message from the model.

        """
        response = _utils.make_request(
            "GET",
            "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
            self._conn,
        )
        _utils.raise_for_http_error(response)

        status = _utils.body_to_json(response)
        if 'api' in status:
            status.update({'url': "{}://{}{}".format(self._conn.scheme, self._conn.socket, status.pop('api'))})
            status.update({'token': status.pop('token', None)})
        return status

    def deploy(self, path=None, token=None, no_token=False, wait=False):
        """
        Deploys the model logged to this Experiment Run.

        .. versionadded:: 0.13.17

        Parameters
        ----------
        path : str, optional
            Suffix for the prediction endpoint URL. If not provided, one will be generated
            automatically.
        token : str, optional
            Token to use to authorize predictions requests. If not provided and `no_token` is
            ``False``, one will be generated automatically.
        no_token : bool, default False
            Whether to not require a token for predictions.
        wait : bool, default False
            Whether to wait for the deployed model to be ready for this function to finish.

        Returns
        -------
        status : dict
            See :meth:`~ExperimentRun.get_deployment_status`.

        Raises
        ------
        RuntimeError
            If the model is already deployed or is being deployed, or if a required deployment
            artifact is missing.

        Examples
        --------
        .. code-block:: python

            status = run.deploy(path="banana", no_token=True, wait=True)
            # waiting for deployment.........
            status
            # {'status': 'deployed',
            #  'url': 'https://app.verta.ai/api/v1/predict/abcdefgh-1234-abcd-1234-abcdefghijkl/banana',
            #  'token': None}
            DeployedModel.from_url(status['url']).predict([x])
            # [0.973340685896]

        """
        data = {}
        if path is not None:
            # get project ID for URL path
            run_msg = self._get_self_as_msg()
            data.update({'url_path': "{}/{}".format(run_msg.project_id, path)})
        if no_token:
            data.update({'token': ""})
        elif token is not None:
            data.update({'token': token})

        response = _utils.make_request(
            "PUT",
            "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
            self._conn, json=data,
        )
        try:
            _utils.raise_for_http_error(response)
        except requests.HTTPError as e:
            # propagate error caused by missing artifact
            # TODO: recommend user call log_model() / log_requirements()
            error_text = e.response.text.strip()
            if error_text.startswith("missing artifact"):
                six.raise_from(RuntimeError("unable to deploy due to {}".format(error_text)), None)
            else:
                raise e

        if wait:
            print("waiting for deployment...", end='')
            sys.stdout.flush()
            while self.get_deployment_status()['status'] not in ("deployed", "error"):
                print(".", end='')
                sys.stdout.flush()
                time.sleep(5)
            print()
            if self.get_deployment_status()['status'] == "error":
                status = self.get_deployment_status()
                raise RuntimeError("model deployment is failing;\n{}".format(status.get('message', "no error message available")))

        return self.get_deployment_status()

    def undeploy(self, wait=False):
        """
        Undeploys the model logged to this Experiment Run.

        .. versionadded:: 0.13.17

        Parameters
        ----------
        wait : bool, default False
            Whether to wait for the undeployment to complete for this function to finish.

        Returns
        -------
        status : dict
            See :meth:`~ExperimentRun.get_deployment_status`.

        Raises
        ------
        RuntimeError
            If the model is already not deployed.

        """
        # skip calling undeploy on already-undeployed model
        #     This needs to be checked first, otherwise the undeployment endpoint will return an
        #     unhelpful HTTP 404 Not Found.
        if self.get_deployment_status()['status'] != "not deployed":
            response = _utils.make_request(
                "DELETE",
                "{}://{}/api/v1/deployment/models/{}".format(self._conn.scheme, self._conn.socket, self.id),
                self._conn,
            )
            _utils.raise_for_http_error(response)

            if wait:
                print("waiting for undeployment...", end='')
                sys.stdout.flush()
                while self.get_deployment_status()['status'] != "not deployed":
                    print(".", end='')
                    sys.stdout.flush()
                    time.sleep(5)
                print()

        return self.get_deployment_status()

    def get_deployed_model(self):
        """
        Returns an object for making predictions against the deployed model.

        .. versionadded:: 0.13.17

        Returns
        -------
        :class:`~verta.deployment.DeployedModel`

        Raises
        ------
        RuntimeError
            If the model is not currently deployed.

        """
        if self.get_deployment_status()['status'] != "deployed":
            raise RuntimeError("model is not currently deployed")

        status = self.get_deployment_status()
        return deployment.DeployedModel.from_url(status['url'], status['token'])

    def log_commit(self, commit, key_paths=None):
        """
        Associate a Commit with this Experiment Run.

        .. versionadded:: 0.14.1

        Parameters
        ----------
        commit : :class:`verta._repository.commit.Commit`
            Verta Commit.
        key_paths : dict of `key` to `path`, optional
            A mapping between descriptive keys and paths of particular interest within `commit`.
            This can be useful for, say, highlighting a particular file as *the* training dataset
            used for this Experiment Run.

        """
        msg = _ExperimentRunService.LogVersionedInput()
        msg.id = self.id
        msg.versioned_inputs.repository_id = commit._repo.id
        msg.versioned_inputs.commit = commit.id
        for key, path in six.viewitems(key_paths or {}):
            location = commit_module.path_to_location(path)
            location_msg = msg.versioned_inputs.key_location_map.get_or_create(key)
            location_msg.location.extend(location)

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/experiment-run/logVersionedInput".format(
            self._conn.scheme,
            self._conn.socket,
        )
        response = _utils.make_request("POST", endpoint, self._conn, json=data)
        _utils.raise_for_http_error(response)

    def get_commit(self):
        """
        Gets the Commit associated with this Experiment Run.

        .. versionadded:: 0.14.1

        Returns
        -------
        commit : :class:`verta._repository.commit.Commit`
            Verta Commit.
        key_paths : dict of `key` to `path`
            A mapping between descriptive keys and paths of particular interest within `commit`.

        """
        msg = _ExperimentRunService.GetVersionedInput()
        msg.id = self.id

        data = _utils.proto_to_json(msg)
        endpoint = "{}://{}/api/v1/modeldb/experiment-run/getVersionedInput".format(
            self._conn.scheme,
            self._conn.socket,
        )
        response = _utils.make_request("GET", endpoint, self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), msg.Response)
        repo = _repository.Repository(self._conn, response_msg.versioned_inputs.repository_id)
        commit_id = response_msg.versioned_inputs.commit
        commit = commit_module.Commit._from_id(self._conn, repo, commit_id)

        key_paths = {
            key: '/'.join(location_msg.location)
            for key, location_msg
            in response_msg.versioned_inputs.key_location_map.items()
        }

        return commit, key_paths
