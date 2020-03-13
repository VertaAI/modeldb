# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib
import os

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from ..external import six

from .._internal_utils import _utils

from . import _dataset


class Path(_dataset._Dataset):
    """
    Captures metadata about files.

    Parameters
    ----------
    paths : list of str
        List of filepaths or directory paths.

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
    def __init__(self, paths):
        if isinstance(paths, six.string_types):
            paths = [paths]

        super(Path, self).__init__()

        paths_to_metadata = dict()  # prevent duplicate objects
        for path in paths:
            paths_to_metadata.update({
                file_metadata.path: file_metadata
                for file_metadata
                in self._get_path_metadata(path)
            })

        metadata = six.viewvalues(paths_to_metadata)
        self._msg.path.components.extend(metadata)

    @classmethod
    def _get_path_metadata(cls, path):
        if os.path.isdir(path):
            for root, _, filenames in os.walk(path):
                for filename in filenames:
                    filepath = os.path.join(root, filename)
                    yield cls._get_file_metadata(filepath)
        else:
            yield cls._get_file_metadata(path)

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
