# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib
import os

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService
from .._protos.public.modeldb.versioning import Dataset_pb2 as _Dataset

from ..external import six

from .._internal_utils import _artifact_utils
from .._internal_utils import _file_utils
from .._internal_utils import _utils

from . import _dataset_blob


class Path(_dataset_blob._DatasetBlob):
    """
    Captures metadata about files.

    .. note::

        If relative paths are passed in, they will *not* be converted to absolute paths.

    Parameters
    ----------
    paths : list of str
        List of filepaths or directory paths.
    base_path : str, optional
        Directory path to be removed from the beginning of `paths` before saving to ModelDB.
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

    .. describe:: dataset += other

        Updates the dataset, adding paths from ``other``.

    .. describe:: dataset + other + ...

        Returns a new dataset with paths from the dataset and all others.

    """
    def __init__(self, paths, base_path=None, enable_mdb_versioning=False):
        if isinstance(paths, six.string_types):
            paths = [paths]
        paths = map(os.path.expanduser, paths)

        super(Path, self).__init__(enable_mdb_versioning=enable_mdb_versioning)

        filepaths = _file_utils.flatten_file_trees(paths)
        components = list(map(self._file_to_component, filepaths))

        # remove `base_path` from the beginning of component paths
        if base_path is not None:
            for component in components:
                path = _file_utils.remove_prefix_dir(component.path, prefix_dir=base_path)
                if path == component.path:  # no change
                    raise ValueError("path {} does not begin with `base_path` {}".format(
                        component.path,
                        base_path,
                    ))

                # update component with modified path
                component.path = path

                # track base path
                component.base_path = base_path

        self._components_map.update({
            component.path: component
            for component
            in components
        })

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls(paths=[])

        for component_msg in blob_msg.dataset.path.components:
            component = Component._from_proto(component_msg)
            obj._components_map[component.path] = component

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()

        for component in self._components_map.values():
            component_msg = component._as_proto()
            blob_msg.dataset.path.components.append(component_msg)

        return blob_msg

    @classmethod
    def _file_to_component(cls, filepath):
        return Component(
            path=filepath,
            size=os.stat(filepath).st_size,
            last_modified=_utils.timestamp_to_ms(os.stat(filepath).st_mtime),
            md5=cls._hash_file(filepath),
        )

    # TODO: move to _file_utils.calc_md5()
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

        for component in self._components_map.values():
            # reconstruct original filepaths with removed `base_path`s
            if component.base_path:
                filepath = os.path.join(component.base_path, component.path)
            else:
                filepath = component.path
            filepath = os.path.abspath(filepath)

            # track which file this component corresponds to
            component._local_path = filepath

            # track MDB path to component
            with open(filepath, 'rb') as f:
                artifact_hash = _artifact_utils.calc_sha256(f)
            component._internal_versioned_path = artifact_hash + '/' + os.path.basename(filepath)

    def _clean_up_uploaded_components(self):
        """
        This method does nothing because this dataset's components shouldn't be automatically deleted.

        """
        return

    def add(self, paths, base_path=None):
        """
        Adds `paths` to this dataset.

        Parameters
        ----------
        paths : list of str
            List of filepaths or directory paths.
        base_path : str, optional
            Directory path to be removed from the beginning of `paths` before saving to ModelDB.

        """
        # re-use logic in __init__
        other = self.__class__(
            paths=paths, base_path=base_path,
            enable_mdb_versioning=self._mdb_versioned,
        )

        self += other


class Component(object):
    """
    A dataset component returned by ``dataset.list_components()``.

    Attributes
    ----------
    path : str
        File path.
    base_path : str
        Prefix of `path`.
    size : int
        File size.
    last_modified : int
        Unix time when this file was last modified.
    sha256 : str
        SHA-256 checksum.
    md5 : str
        MD5 checksum.

    """
    def __init__(
            self,
            path, size=None, last_modified=None,
            sha256=None, md5=None,
            base_path=None,
            internal_versioned_path=None, local_path=None):
        # metadata
        self.path = path
        self.size = size
        self.last_modified = last_modified

        # checksums
        self.sha256 = sha256
        self.md5 = md5

        # base path
        self.base_path = base_path

        # ModelDB versioning
        self._internal_versioned_path = internal_versioned_path
        self._local_path = local_path

    def __repr__(self):
        lines = [self.path]

        if self.base_path:
            lines.append("base path: {}".format(self.base_path))
        if self.size:
            lines.append("{} bytes".format(self.size))
        if self.last_modified:
            lines.append("last modified: {}".format(_utils.timestamp_to_str(self.last_modified)))
        if self.sha256:
            lines.append("SHA-256 checksum: {}".format(self.sha256))
        if self.md5:
            lines.append("MD5 checksum: {}".format(self.md5))

        return "\n    ".join(lines)

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self.__dict__ == other.__dict__

    @classmethod
    def _from_proto(cls, component_msg):
        return cls(
            path=component_msg.path,
            size=component_msg.size or None,
            last_modified=component_msg.last_modified_at_source or None,
            sha256=component_msg.sha256 or None,
            md5=component_msg.md5 or None,
            internal_versioned_path=component_msg.internal_versioned_path or None,
            base_path=component_msg.base_path,
        )

    def _as_proto(self):
        return _Dataset.PathDatasetComponentBlob(
            path=self.path or "",
            size=self.size or 0,
            last_modified_at_source=self.last_modified or 0,
            sha256=self.sha256 or "",
            md5=self.md5 or "",
            internal_versioned_path=self._internal_versioned_path or "",
            base_path=self.base_path or "",
        )
