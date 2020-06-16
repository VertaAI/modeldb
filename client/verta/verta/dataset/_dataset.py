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
    def _component_blobs(self):
        """This shall be implemented by subclasses, but shouldn't halt execution if called."""
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

    def download(self, component_path, filepath=None, chunk_size=32*(10**3)):
        """
        Downloads `component_path` from this dataset if ModelDB-managed versioning was enabled.

        Parameters
        ----------
        component_path : str
            Original path of the file in this dataset to download.
        filepath : str, optional
            Filepath to download `component_path` to. If not provided, the file will be downloaded
            to the current directory.
        chunk_size : int, default 32 kB
            Number of bytes to download at a time.

        """
        if self._commit is None and self._blob_path is None:
            raise RuntimeError(
                "this dataset cannot be used for downloads;"
                " consider using `commit.get()` to obtain a download-capable dataset"
                " if ModelDB-managed versioning was enabled"
            )
        if filepath is not None and os.path.exists(filepath):
            raise OSError("{} already exists".format(filepath))

        # backend will return error if `component_path` not found/versioned
        url = self._commit._get_url_for_artifact(self._blob_path, component_path, "GET").url

        # stream download to avoid overwhelming memory
        response = _utils.make_request("GET", url, self._commit._conn, stream=True)
        try:
            _utils.raise_for_http_error(response)

            print("downloading {} from ModelDB".format(component_path))
            try:
                # read response stream into temp file
                with tempfile.NamedTemporaryFile('wb', delete=False) as tempf:
                    for chunk in response.iter_content(chunk_size=chunk_size):
                        tempf.write(chunk)

                # check for a unique `filepath`
                if filepath is None:
                    # default to filename from `component_path` in cwd
                    filepath = os.path.basename(component_path)

                    # avoid collisions with existing files
                    while os.path.exists(filepath):
                        filepath = _file_utils.increment_filepath(filepath)
                else:
                    if os.path.exists(filepath):  # file was created while we were downloading
                        raise OSError("{} already exists".format(filepath))
                    else:
                        # create parent dirs
                        pathlib2.Path(filepath).parent.mkdir(parents=True, exist_ok=True)

                # move written contents to `filepath`
                os.rename(tempf.name, filepath)
                print("download complete; file written to {}".format(filepath))
            except Exception as e:
                # delete partially-downloaded file
                os.remove(tempf.name)
                raise e
        finally:
            response.close()

        print("download complete ({})".format(filepath))
