# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib
import os

from .._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

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
        super(Path, self).__init__(enable_mdb_versioning=enable_mdb_versioning)

        if isinstance(paths, six.string_types):
            paths = [paths]
        paths = map(os.path.expanduser, paths)

        paths = map(self._remove_file_scheme, paths)

        filepaths = _file_utils.flatten_file_trees(paths)
        components = list(map(self._file_to_component, filepaths))

        # remove `base_path` from the beginning of component paths
        # TODO: move this into _add_components()
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

        self._add_components(components)

    @classmethod
    def _from_proto(cls, blob_msg):
        obj = cls._create_empty()

        obj._add_components([
            _dataset.Component._from_proto(component_msg)
            for component_msg
            in blob_msg.dataset.path.components
        ])

        return obj

    def _as_proto(self):
        blob_msg = _VersioningService.Blob()

        for component in self._components_map.values():
            component_msg = component._as_proto()
            blob_msg.dataset.path.components.append(component_msg)

        return blob_msg

    def _file_to_component(self, filepath):
        return _dataset.Component(
            path=filepath,
            size=os.stat(filepath).st_size,
            last_modified=_utils.timestamp_to_ms(os.stat(filepath).st_mtime),
            md5=self._hash_file(filepath),
        )

    def _add_components(self, components):
        for component in components:
            component.path = self._remove_file_scheme(component.path)

        super(Path, self)._add_components(components)

    @staticmethod
    def _remove_file_scheme(path):
        """
        Removes the "file" scheme from `path`, if present.

        Parameters
        ----------
        path : str
            Filepath.

        Returns
        -------
        str
            `path` without "file" scheme.

        References
        ----------
        .. [1] https://en.wikipedia.org/wiki/File_URI_scheme

        """
        path = _file_utils.remove_prefix(path, "file://")
        path = _file_utils.remove_prefix(path, "file:")

        return path

    # TODO: move to _file_utils.calc_md5()
    def _hash_file(self, filepath):
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

    @classmethod
    def with_spark(cls, sc, paths):
        if all(map(os.path.exists, paths)):
            # This `if` is a slight hack to check for local files,
            # because we don't want this behavior in the HDFS subclass.
            # TODO: maybe have an abstract base class for filesystem datasets

            # PySpark won't traverse directories, so we have to
            paths = _file_utils.flatten_file_trees(paths)

            # PySpark won't see hidden files, so we have filter them out
            removed_paths = list(filter(cls._is_hidden_to_spark, paths))
            for removed_path in removed_paths:
                print("ignored by Spark: {}".format(removed_path))
                paths.remove(removed_path)

        return super(Path, cls).with_spark(sc, paths)

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
