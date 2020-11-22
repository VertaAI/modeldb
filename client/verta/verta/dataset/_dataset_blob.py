# -*- coding: utf-8 -*-

from __future__ import print_function

import abc
import copy
import os
import pathlib2

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._internal_utils import (
    _file_utils,
    _request_utils,
    _utils,
)

from .._repository import blob


DEFAULT_DOWNLOAD_DIR = "mdb-data-download"  # to be in cwd


class _DatasetBlob(blob.Blob):
    """
    Base class for dataset versioning. Not for human consumption.

    """
    def __init__(self, paths=None, enable_mdb_versioning=False):
        super(_DatasetBlob, self).__init__()

        self._components_map = dict()  # paths to Component objects

        self._mdb_versioned = enable_mdb_versioning

        # to be set during commit.get() to enable download() with ModelDB-managed versioning
        self._commit = None
        self._blob_path = None

    def __repr__(self):
        lines = ["{} Version".format(self.__class__.__name__)]

        components = self._components_map.values()
        components = sorted(components, key=lambda component: component.path)
        for component in components:
            lines.extend(repr(component).splitlines())

        return "\n    ".join(lines)

    def __add__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        new = copy.deepcopy(self)
        return new.__iadd__(other)

    def __iadd__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        self_keys = set(self._components_map.keys())
        other_keys = set(other._components_map.keys())
        intersection = list(self_keys & other_keys)
        if intersection:
            raise ValueError("dataset already contains paths: {}".format(intersection))

        if self._mdb_versioned != other._mdb_versioned:
            raise ValueError("datasets must have same value for `enable_mdb_versioning`")

        self._components_map.update(other._components_map)
        return self

    @abc.abstractmethod
    def _prepare_components_to_upload(self):
        pass

    @abc.abstractmethod
    def _clean_up_uploaded_components(self):
        pass

    def _set_commit_and_blob_path(self, commit, blob_path):
        """
        Associate this blob with a commit and path to enable downloads.

        Parameters
        ----------
        commit : :class:`verta._repository.commit.Commit`
            Commit this blob was gotten from.
        blob_path : str
            Location of this blob within its Repository.

        """
        self._commit = commit
        self._blob_path = blob_path

    # TODO: there is too much happening in this method's body
    def _get_components_to_download(self, component_path=None, download_to_path=None):
        """
        Identify components to be downloaded, along with their local destination paths.

        Parameters
        ----------
        component_path : str, optional
            Path to directory or file within blob.
        download_to_path : str, optional
            Local path to download to.

        Returns
        -------
        components_to_download : dict
            Map of component paths to local destination paths.
        downloaded_to_path : str
            Absolute path where file(s) were downloaded to. Matches `download_to_path` if it was
            provided as an argument.

        """
        implicit_download_to_path = download_to_path is None

        if component_path is not None:
            # look for an exact match with `component_path` as a file
            for path in self.list_paths():
                if path == component_path:
                    if implicit_download_to_path:
                        # default to filename from `component_path`, in cwd
                        local_path = os.path.basename(component_path)

                        # avoid collision with existing file
                        local_path = _file_utils.without_collision(local_path)
                    else:
                        # exactly where the user requests
                        local_path = download_to_path

                    return ({path: local_path}, os.path.abspath(local_path))
        # no exact match, so it's a folder download (or nonexistent path)

        # figure out where files are going to be downloaded to
        if implicit_download_to_path:
            if component_path is None:
                downloaded_to_path = DEFAULT_DOWNLOAD_DIR

                # avoid collision with existing directory
                downloaded_to_path = _file_utils.without_collision(downloaded_to_path)
            else:  # need to automatically determine directory
                # NOTE: if `component_path` == "s3://" with any trailing slashes, it becomes "s3:"
                downloaded_to_path = pathlib2.Path(component_path).name  # final path component

                if downloaded_to_path in {".", "..", "/", "s3:"}:
                    # rather than dump everything into cwd, use new child dir
                    downloaded_to_path = DEFAULT_DOWNLOAD_DIR

                # avoid collision with existing directory
                downloaded_to_path = _file_utils.without_collision(downloaded_to_path)
        else:
            # exactly where the user requests
            downloaded_to_path = download_to_path

        # collect paths in blob and map them to download locations
        components_to_download = dict()
        if component_path is None:
            # download all
            for path in self.list_paths():
                local_path = os.path.join(
                    downloaded_to_path,
                    _file_utils.remove_prefix_dir(path, "s3:"),
                )

                components_to_download[path] = local_path
        else:
            # look for files contained in `component_path` as a directory
            component_path_as_dir = component_path if component_path.endswith('/') else component_path+'/'
            for path in self.list_paths():
                if path.startswith(component_path_as_dir):
                    # rebase from `component_path` onto `downloaded_to_path`
                    #     Implicit `download_to_path` example:
                    #         component_blob.path = "coworker/downloads/data/info.csv"
                    #         component_path      = "coworker/downloads"
                    #         downloaded_to_path  =          "downloads" or "downloads 1", etc.
                    #         local_path          =          "downloads/data/info.csv"
                    #     Explicit `download_to_path` example:
                    #         component_blob.path = "coworker/downloads/data/info.csv"
                    #         component_path      = "coworker/downloads"
                    #         downloaded_to_path  =            "my-data"
                    #         local_path          =            "my-data/data/info.csv"
                    local_path = os.path.join(
                        downloaded_to_path,
                        _file_utils.remove_prefix_dir(path, prefix_dir=component_path),
                    )

                    components_to_download[path] = local_path

        if not components_to_download:
            raise KeyError("no components found for path {}".format(component_path))

        return (components_to_download, os.path.abspath(downloaded_to_path))

    @abc.abstractmethod
    def add(self, paths):
        pass

    def download(self, component_path=None, download_to_path=None):
        """
        Downloads `component_path` from this dataset if ModelDB-managed versioning was enabled.

        Parameters
        ----------
        component_path : str, optional
            Original path of the file or directory in this dataset to download. If not provided,
            all files will be downloaded.
        download_to_path : str, optional
            Path to download to. If not provided, the file(s) will be downloaded into a new path in
            the current directory. If provided and the path already exists, it will be overwritten.

        Returns
        -------
        downloaded_to_path : str
            Absolute path where file(s) were downloaded to. Matches `download_to_path` if it was
            provided as an argument.

        """
        if self._commit is None or self._blob_path is None:
            raise RuntimeError(
                "this dataset cannot be used for downloads;"
                " consider using `commit.get()` to obtain a download-capable dataset"
                " if ModelDB-managed versioning was enabled"
            )
        implicit_download_to_path = download_to_path is None

        components_to_download, downloaded_to_path = self._get_components_to_download(
            component_path,
            download_to_path,
        )
        for path in components_to_download:  # component paths
            local_path = components_to_download[path]  # dict will be updated near end of iteration

            # create parent dirs
            pathlib2.Path(local_path).parent.mkdir(parents=True, exist_ok=True)
            # TODO: clean up empty parent dirs if something later fails

            url = self._commit._get_url_for_artifact(self._blob_path, path, "GET").url

            # stream download to avoid overwhelming memory
            with _utils.make_request("GET", url, self._commit._conn, stream=True) as response:
                _utils.raise_for_http_error(response)

                print("downloading {} from ModelDB".format(path))
                if (implicit_download_to_path
                        and len(components_to_download) == 1):  # single file download
                    # update `downloaded_to_path` in case changed to avoid overwrite
                    downloaded_to_path = _request_utils.download(response, local_path, overwrite_ok=False)
                else:
                    # don't update `downloaded_to_path` here because we are either downloading:
                    #     - single file with an explicit destination, so `local_path` won't change
                    #     - directory, so individual path's `local_path` isn't important
                    _request_utils.download(response, local_path, overwrite_ok=True)

        return os.path.abspath(downloaded_to_path)

    def list_paths(self):
        """
        Returns the paths of all components in this dataset.

        Returns
        -------
        component_paths : list of str
            Paths of all components.

        """
        return list(sorted(
            component.path
            for component
            in self._components_map.values()
        ))

    def list_components(self):
        """
        Returns the components in this dataset.

        Returns
        -------
        components : list of :class:`~verta.dataset._dataset.Component`
            Components.

        """
        components = self._components_map.values()
        return list(sorted(components, key=lambda component: component.path))
