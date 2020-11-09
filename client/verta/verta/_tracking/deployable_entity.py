# -*- coding: utf-8 -*-

from __future__ import print_function

import abc
import copy
import glob
import importlib
import os
import re
import shutil
import sys
import tarfile
import tempfile
import zipfile

import requests

from .entity import _ModelDBEntity
from .._internal_utils import (
    _histogram_utils,
    _utils,
)

from .._protos.public.common import CommonService_pb2 as _CommonCommonService


from ..external import six


# location in DeploymentService model container
_CUSTOM_MODULES_DIR = os.environ.get('VERTA_CUSTOM_MODULES_DIR', "/app/custom_modules/")

# for caching files
_CACHE_DIR = os.path.join(
    os.path.expanduser("~"),
    ".verta",
    "cache",
)


@six.add_metaclass(abc.ABCMeta)
class _DeployableEntity(_ModelDBEntity):
    @property
    def _histogram_endpoint(self):
        return "{}://{}/api/v1/monitoring/data/references/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.id,
        )

    def _cache_file(self, filename, contents):
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
        shutil.move(temp_path, path)

        return path

    def _get_cached_file(self, filename):
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

    @abc.abstractmethod
    def _get_artifact(self, key):
        raise NotImplementedError

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
        self._refresh_cache()
        existing_artifact_keys = {artifact.key for artifact in self._msg.artifacts}
        unlogged_artifact_keys = set(keys) - existing_artifact_keys
        if unlogged_artifact_keys:
            raise ValueError("`keys` contains keys that have not been logged: {}".format(sorted(unlogged_artifact_keys)))

        # get artifact checksums
        paths = {artifact.key: artifact.path
                 for artifact in self._msg.artifacts}

        artifacts = dict()
        for key in keys:
            filename = os.path.join("artifacts", paths[key])

            # check cache, otherwise write to cache
            #     "try-get-then-create" can lead multiple threads trying to write to the cache
            #     simultaneously, but artifacts being cached at a particular location should be
            #     identical, so multiple writes would be idempotent.
            path = self._get_cached_file(filename)
            if path is None:
                contents = self._get_artifact(key)
                if isinstance(contents, tuple):
                    # ExperimentRun._get_artifact() returns two values (contents, path_only)
                    # whereas ModelVersion._get_artifact() returns one (contents), so until
                    # their implementations are unified, this check is to handle the difference.
                    contents, _ = contents  # TODO: raise error if path_only

                path = self._cache_file(filename, contents)

            artifacts.update({key: path})

        return artifacts

    def _custom_modules_as_artifact(self, paths=None):
        if isinstance(paths, six.string_types):
            paths = [paths]

        # If we include a path that is actually a module, then we _must_ add its parent to the
        # adjusted sys.path in the end so that we can re-import with the same name.
        forced_local_sys_paths = []
        if paths is not None:
            new_paths = []
            for p in paths:
                abspath = os.path.abspath(os.path.expanduser(p))
                if os.path.exists(abspath):
                    new_paths.append(abspath)
                else:
                    try:
                        mod = importlib.import_module(p)
                        new_paths.extend(mod.__path__)
                        forced_local_sys_paths.extend(map(os.path.dirname, mod.__path__))
                    except ImportError:
                        raise ValueError("custom module {} does not correspond to an existing folder or module".format(p))

            paths = new_paths

        forced_local_sys_paths = sorted(list(set(forced_local_sys_paths)))

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
        local_sys_paths = list(filter(lambda path: not _utils.is_in_venv(path), local_sys_paths))

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
        ## remove .git
        local_filepaths = set(filter(lambda path: not path.endswith(".git") and ".git/" not in path,
                                      local_filepaths))

        # obtain deepest common directory
        #     This directory on the local system will be mirrored in `_CUSTOM_MODULES_DIR` in
        #     deployment.
        curr_dir = os.path.join(os.getcwd(), "")
        paths_plus = list(local_filepaths) + [curr_dir]
        common_prefix = os.path.commonprefix(paths_plus)
        common_dir = os.path.dirname(common_prefix)

        # replace `common_dir` with `_CUSTOM_MODULES_DIR` for deployment sys.path
        depl_sys_paths = list(map(lambda path: os.path.relpath(path, common_dir), local_sys_paths + forced_local_sys_paths))
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

        bytestream.seek(0)

        return bytestream

    def log_training_data(self, train_features, train_targets, overwrite=False):
        """
        Associate training data with this model reference.

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

        response = _utils.make_request("PUT", self._histogram_endpoint, self._conn, json=histograms)
        _utils.raise_for_http_error(response)

    def _get_histogram(self):
        """
        Returns histogram JSON.

        Note that in Python 2, the JSON library returns strings as ``unicode``.

        Returns
        -------
        dict

        """
        response = _utils.make_request("GET", self._histogram_endpoint, self._conn)
        try:
            _utils.raise_for_http_error(response)
        except requests.HTTPError as e:
            if e.response.status_code == 404:
                raise RuntimeError("log_training_data() may not yet have been called")

            # if not 404 error (e.g 429), throw them as-is:
            raise e

        return response.json()
