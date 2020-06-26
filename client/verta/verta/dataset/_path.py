# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib
import os

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from ..external import six

from .._internal_utils import _artifact_utils
from .._internal_utils import _file_utils
from .._internal_utils import _utils

from . import _dataset


class Path(_dataset._Dataset):
    """
    Captures metadata about files.

    .. note::

        If relative paths are passed in, they will *not* be converted to absolute paths.

    Parameters
    ----------
    paths : list of str
        List of filepaths or directory paths.
    enable_mdb_versioning : bool, default False
        Whether to upload the data itself to ModelDB to enable managed data versioning.

    Examples
    --------
    .. code-block:: python

        from verta.dataset import Path
        dataset1 = Path([
            "../datasets/census-train.csv",
            "../datasets/census-test.csv",
        ])
        dataset2 = Path([
            "../datasets",
        ])

    """
    def __init__(self, paths, enable_mdb_versioning=False):
        if isinstance(paths, six.string_types):
            paths = [paths]
        paths = map(os.path.expanduser, paths)

        super(Path, self).__init__(enable_mdb_versioning=enable_mdb_versioning)

        filepaths = _file_utils.flatten_file_trees(paths)
        components = map(self._get_file_metadata, filepaths)

        self._msg.path.components.extend(components)

    def __repr__(self):
        # TODO: consolidate with S3 since they're almost identical now
        lines = ["Path Version"]
        components = sorted(
            self._path_component_blobs,
            key=lambda component_msg: component_msg.path,
        )
        for component in components:
            lines.extend(self._path_component_to_repr_lines(component))

        return "\n    ".join(lines)

    @property
    def _path_component_blobs(self):
        return [
            component
            for component
            in self._msg.path.components
        ]

    @classmethod
    def _get_file_metadata(cls, filepath):
        msg = _DatasetService.PathDatasetComponentBlob()
        msg.path = filepath
        msg.size = os.stat(filepath).st_size
        msg.last_modified_at_source = _utils.timestamp_to_ms(os.stat(filepath).st_mtime)
        msg.md5 = cls._hash_file(filepath)

        return msg

    @staticmethod
    def _hash_file(filepath):
        """
        Returns the MD5 hash of `filename`.

        Notes
        -----
        Loop recommended by https://stackoverflow.com/questions/3431825 and
        https://stackoverflow.com/questions/1131220.

        """
        file_hash = hashlib.md5()
        with open(filepath, 'rb') as f:
            for chunk in iter(lambda: f.read(8192), b''):
                file_hash.update(chunk)
        return file_hash.hexdigest()

    def _prepare_components_to_upload(self):
        """
        Tracks files for upload to ModelDB.

        This method does nothing if ModelDB-managed versioning was not enabled.

        """
        if not self._mdb_versioned:
            return

        for component_blob in self._path_component_blobs:
            component_path = component_blob.path

            # TODO: when stripping base path is implemented, reconstruct original path here
            filepath = os.path.abspath(component_path)

            # track which file this component corresponds to
            self._components_to_upload[component_path] = filepath

            # add MDB path to component blob
            with open(filepath, 'rb') as f:
                artifact_hash = _artifact_utils.calc_sha256(f)
            component_blob.internal_versioned_path = artifact_hash + '/' + os.path.basename(filepath)

    def _clean_up_uploaded_components(self):
        """
        This method does nothing because this dataset's components shouldn't be automatically deleted.

        """
        return
