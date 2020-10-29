# -*- coding: utf-8 -*-

from __future__ import print_function

import importlib
import os
import time
import zipfile
import requests

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import CommonService_pb2 as _CommonService

from ..external import six

from .._internal_utils import (
    _artifact_utils,
    _git_utils,
    _utils,
)


_OSS_DEFAULT_WORKSPACE = "personal"
_MODEL_ARTIFACTS_ATTR_KEY = "verta_model_artifacts"


class _ModelDBEntity(object):
    def __init__(self, conn, conf, service_module, service_url_component, msg):
        self._conn = conn
        self._conf = conf

        # TODO: remove these b/c they're barely used, and don't even work with registry
        self._service = service_module
        self._request_url = "{}://{}/api/v1/modeldb/{}/{}".format(self._conn.scheme,
                                                      self._conn.socket,
                                                      service_url_component,
                                                      '{}')  # endpoint placeholder

        self.id = msg.id
        self._msg = msg
        self._cache_time = time.time()
        self._update_cache()

    def _clear_cache(self):
        self._msg = None
        self._cache_time = None

    def _refresh_cache(self):
        now = time.time()
        if self._cache_time is None:
            self._msg = None

        # Cache for 5 seconds
        if self._cache_time is not None and now - self._cache_time > 5:
            self._msg = None

        if self._msg is None:
            self._msg = self._get_proto_by_id(self._conn, self.id)
            self._cache_time = now
            self._update_cache()

    def _fetch_with_no_cache(self):
        self._msg = self._get_proto_by_id(self._conn, self.id)
        self._cache_time = None
        self._update_cache()

    def _update_cache(self):
        pass

    def __getstate__(self):
        state = self.__dict__.copy()

        state['_service_module_name'] = state['_service'].__name__
        del state['_service']

        # This is done because we can't pickle protobuf objects
        # TODO: use json conversion instead to avoid the call
        del state['_msg']

        return state

    def __setstate__(self, state):
        state['_service'] = importlib.import_module(state['_service_module_name'])
        del state['_service_module_name']

        self.__dict__.update(state)

    @classmethod
    def _generate_default_name(cls):
        raise NotImplementedError

    @classmethod
    def _get_by_id(cls, conn, conf, id):
        msg = cls._get_proto_by_id(conn, id)
        if msg:
            print("got existing {}: {}".format(cls.__name__, msg.id))
            # pylint: disable=no-value-for-parameter
            # this is only called on subclasses, so 3 params to cls() is correct
            return cls(conn, conf, msg)
        else:
            raise ValueError("{} with ID {} not found".format(cls.__name__, id))

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        raise NotImplementedError

    # TODO: add prints about status
    @classmethod
    def _get_or_create_by_name(cls, conn, name, getter, creator, checker):
        if name is None:
            name = cls._generate_default_name()

        obj = getter(name)
        if obj is None:
            obj = creator(name)
        else:
            print("got existing {}: {}".format(cls.__name__, name))
            checker()
        return obj

    @classmethod
    def _get_by_name(cls, conn, conf, name, parent):
        msg = cls._get_proto_by_name(conn, name, parent)
        if msg:
            # pylint: disable=no-value-for-parameter
            # this is only called on subclasses, so 3 params to cls() is correct
            return cls(conn, conf, msg)
        else:
            return None

    @classmethod
    def _get_proto_by_name(cls, conn, name, parent):
        raise NotImplementedError

    @classmethod
    def _create(cls, conn, conf, *args, **kwargs):
        if 'name' in kwargs and kwargs['name'] is None:
            kwargs['name'] = cls._generate_default_name()

        msg = cls._create_proto(conn, *args, **kwargs)
        if msg:
            # pylint: disable=no-value-for-parameter
            # this is only called on subclasses, so 3 params to cls() is correct
            return cls(conn, conf, msg)
        else:
            return None

    @classmethod
    def _create_proto(cls, conn, *args, **kwargs):
        tags = kwargs.pop('tags', None)
        if tags is not None:
            kwargs['tags'] = _utils.as_list_of_str(tags)

        attrs = kwargs.pop('attrs', None)
        if attrs is not None:
            kwargs['attrs'] = [
                _CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                for key, value in six.viewitems(attrs)
            ]

        return cls._create_proto_internal(conn, *args, **kwargs)

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, desc=None, tags=None, attrs=None, date_created=None, **kwargs):  # recommended params
        raise NotImplementedError

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

                run.log_code()
                run.get_code()
                # {'exec_path': 'comparison/outcomes/classification.ipynb',
                #  'repo_url': 'git@github.com:VertaAI/experiments.git',
                #  'commit_hash': 'f99abcfae6c3ce6d22597f95ad6ef260d31527a6',
                #  'is_dirty': False}

            Log Git snapshot information, plus the location of a specific source code file relative
            to the repository root:

            .. code-block:: python

                run.log_code("../trainer/training_pipeline.py")
                run.get_code()
                # {'exec_path': 'comparison/trainer/training_pipeline.py',
                #  'repo_url': 'git@github.com:VertaAI/experiments.git',
                #  'commit_hash': 'f99abcfae6c3ce6d22597f95ad6ef260d31527a6',
                #  'is_dirty': False}

        With ``Client(use_git=False)``:

            Find and upload the currently executing notebook/script:

            .. code-block:: python

                run.log_code()
                zip_file = run.get_code()
                zip_file.printdir()
                # File Name                          Modified             Size
                # classification.ipynb        2019-07-10 17:18:24        10287

            Upload a specific source code file:

            .. code-block:: python

                run.log_code("../trainer/training_pipeline.py")
                zip_file = run.get_code()
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
            except (ImportError, OSError):  # notebook not found
                try:
                    exec_path = _utils.get_script_filepath()
                except OSError:  # script not found
                    print("unable to find code file; skipping")
        else:
            exec_path = os.path.expanduser(exec_path)
            if not os.path.isfile(exec_path):
                raise ValueError("`exec_path` \"{}\" must be a valid filepath".format(exec_path))

        # TODO: remove this circular dependency
        from .project import Project
        from .experiment import Experiment
        from .experimentrun import ExperimentRun
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
            # pylint: disable=no-member
            # this method should only be called on ExperimentRun, which does have _get_url_for_artifact()
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
        # TODO: remove this circular dependency
        from .project import Project
        from .experiment import Experiment
        from .experimentrun import ExperimentRun
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
            # pylint: disable=no-member
            # this method should only be called on ExperimentRun, which does have _get_url_for_artifact()
            url = self._get_url_for_artifact("verta_code_archive", "GET", code_ver_msg.code_archive.artifact_type).url

            response = _utils.make_request("GET", url, self._conn)
            _utils.raise_for_http_error(response)

            code_archive = six.BytesIO(response.content)
            return zipfile.ZipFile(code_archive, 'r')  # TODO: return a util class instead, maybe
        else:
            raise RuntimeError("unable find code in response")

    def _get_workspace_name_by_id(self, workspace_id):
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
