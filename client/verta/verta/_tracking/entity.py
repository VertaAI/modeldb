# -*- coding: utf-8 -*-

from __future__ import print_function

import importlib
import os
import tarfile
import tempfile
import zipfile

# from .project import Project
# from .experiment import Experiment
# from .experimentrun import ExperimentRun

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import CommonService_pb2 as _CommonService

from ..external import six

from .._internal_utils import (
    _artifact_utils,
    _git_utils,
    _utils,
)

_CACHE_DIR = os.path.join(
    os.path.expanduser("~"),
    ".verta",
    "cache",
)


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
