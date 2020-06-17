# -*- coding: utf-8 -*-

from __future__ import print_function

import os
import pathlib2
import tempfile

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._internal_utils import _utils
from .._internal_utils import _file_utils

from .._repository import blob


class _Dataset(blob.Blob):
    """
    Base class for dataset versioning. Not for human consumption.

    """
    def __init__(self, enable_mdb_versioning=False):
        super(_Dataset, self).__init__()

        self._msg = _DatasetService.DatasetBlob()

        self._mdb_versioned = enable_mdb_versioning
        self._components_to_upload = dict()  # component paths to local filepaths

        # to be set during commit.get() to enable download() with ModelDB-managed versioning
        self._commit = None
        self._blob_path = None

    @property
    def _path_component_blobs(self):
        """
        Returns path components of this dataset.

        Returns
        -------
        list of PathDatasetComponentBlob
            Path components of this dataset.

        """
        # This shall be implemented by subclasses, but shouldn't halt execution if called.
        return []

    @staticmethod
    def _path_component_to_repr_lines(path_component_msg):
        """
        Parameters
        ----------
        path_component_msg : PathDatasetComponentBlob

        Returns
        -------
        lines : list of str
            Lines to be used in the ``__repr__`` of a dataset blob object.

        """
        lines = [path_component_msg.path]
        if path_component_msg.size:
            lines.append("    {} bytes".format(path_component_msg.size))
        if path_component_msg.last_modified_at_source:
            lines.append("    last modified {}".format(_utils.timestamp_to_str(path_component_msg.last_modified_at_source)))
        if path_component_msg.md5:
            lines.append("    MD5 checksum: {}".format(path_component_msg.md5))
        if path_component_msg.sha256:
            lines.append("    SHA-256 checksum: {}".format(path_component_msg.sha256))

        return lines

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

    def _get_components_to_download(self, component_path, download_to_path=None):
        """
        Identify components to be downloaded, along with their local destination paths.

        Parameters
        ----------
        component_path : str
            Path to directory or file within blob.
        download_to_path : str, optional
            Local path to download to.

        Returns
        -------
        components_to_download : dict
            Map of component paths to local destination paths.

        """
        component_path_as_dir = component_path if component_path.endswith('/') else component_path+'/'
        components_to_download = dict()

        for component_blob in self._path_component_blobs:
            if component_blob.path == component_path:  # exact match with file
                if download_to_path is None:
                    # default to filename from `component_path` in cwd
                    local_path = os.path.basename(component_path)
                else:
                    local_path = download_to_path

                return {component_blob.path: local_path}
            elif component_blob.path.startswith(component_path_as_dir):
                if download_to_path is None:
                    # default to path relative to parent of `component_path`
                    #     For example:
                    #     component_blob.path = "coworker/downloads/data/info.csv"
                    #     component_path      = "coworker/downloads"
                    #     local_path          =          "downloads/data/info.csv"
                    local_path = os.path.relpath(component_blob.path, pathlib2.Path(component_path).parent)
                else:
                    # rebase from `component_path` onto `download_to_path`
                    #     For example:
                    #     component_blob.path = "coworker/downloads/data/info.csv"
                    #     component_path      = "coworker/downloads"
                    #     download_to_path    =            "my-data"
                    #     local_path          =            "my-data/data/info.csv"
                    local_path = os.path.join(
                        download_to_path,
                        os.path.relpath(component_blob.path, component_path),
                    )

                components_to_download[component_blob.path] = local_path

        if not components_to_download:
            raise KeyError("no components found for path {}".format(component_path))

        return components_to_download

    def download(self, component_path, download_to_path=None, chunk_size=32*(10**3)):
        """
        Downloads `component_path` from this dataset if ModelDB-managed versioning was enabled.

        Parameters
        ----------
        component_path : str
            Original path of the file or directory in this dataset to download.
        download_to_path : str, optional
            Path to download to. If not provided, the file(s) will be downloaded into a new path in
            the current directory. If provided and the path already exists, it will be overwritten.
        chunk_size : int, default 32 kB
            Number of bytes to download at a time.

        Returns
        -------
        downloaded_to_path : str
            Path where file(s) were downloaded to. Identical to `download_to_path` if it was
            provided as an argument.

        """
        if self._commit is None or self._blob_path is None:
            raise RuntimeError(
                "this dataset cannot be used for downloads;"
                " consider using `commit.get()` to obtain a download-capable dataset"
                " if ModelDB-managed versioning was enabled"
            )

        components_to_download = self._get_components_to_download(component_path, download_to_path)
        for path in components_to_download:  # component paths
            url = self._commit._get_url_for_artifact(self._blob_path, path, "GET").url

            # stream download to avoid overwhelming memory
            response = _utils.make_request("GET", url, self._commit._conn, stream=True)
            try:
                _utils.raise_for_http_error(response)

                print("downloading {} from ModelDB".format(path))
                tempf = None  # declare first in case NamedTemporaryFile init fails
                try:
                    # read response stream into temp file
                    with tempfile.NamedTemporaryFile('wb', delete=False) as tempf:
                        for chunk in response.iter_content(chunk_size=chunk_size):
                            tempf.write(chunk)

                    local_path = components_to_download[path]

                    if download_to_path is None:  # destination paths were automatically generated
                        # avoid collisions with existing files
                        while os.path.exists(local_path):
                            local_path = _file_utils.increment_filepath(local_path)

                        # update local path in dict
                        components_to_download[path] = local_path

                    # create parent dirs
                    pathlib2.Path(local_path).parent.mkdir(parents=True, exist_ok=True)

                    # move written contents to `filepath`
                    os.rename(tempf.name, local_path)
                except Exception as e:
                    # delete partially-downloaded file
                    if tempf is not None:
                        os.remove(tempf.name)
                    raise e
                else:
                    print("download complete; file written to {}".format(components_to_download[path]))
            finally:
                response.close()

        # TODO: return dir, not path, when user passed dir containing one file
        # TODO: os.path.commonprefix is not the right tool here
        return os.path.abspath(os.path.commonprefix(list(components_to_download.values())))
