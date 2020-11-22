# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib

from ._path import Path

from ..external import six

from . import _dataset

_HDFS_PREFIX = "hdfs:/"

class HDFSPath(Path):
    # TODO: support mdb versioning
    def __init__(self, hdfs_client, paths, base_path=None):
        self.client = hdfs_client

        if isinstance(paths, six.string_types):
            paths = [paths]

        filepaths = set()
        for path in paths:
            for base, dirs, files in self.client.walk(path):
                for filename in files:
                    filepaths.add(base + "/" + filename)
            else:
                filepaths.add(path)

        paths = sorted(list(filepaths))

        paths = list(map(lambda path: _HDFS_PREFIX+path if not path.startswith(_HDFS_PREFIX) else path, paths))
        if base_path and not base_path.startswith(_HDFS_PREFIX):
            base_path = _HDFS_PREFIX+base_path

        super(HDFSPath, self).__init__(paths, base_path)

    def _file_to_component(self, filepath):
        original_filepath = filepath
        filepath = filepath[len(_HDFS_PREFIX):]
        metadata = self.client.status(filepath)
        return _dataset.Component(
            path=original_filepath,
            size=metadata['length'],
            last_modified=metadata['modificationTime'], # handle timezone?
            md5=self._hash_file(filepath),
        )

    def _hash_file(self, filepath):
        """
        Returns the MD5 hash of `filename`.

        Notes
        -----
        Loop recommended by https://stackoverflow.com/questions/3431825 and
        https://stackoverflow.com/questions/1131220.

        """
        file_hash = hashlib.md5()
        with self.client.read(filepath) as f:
            for chunk in iter(lambda: f.read(2**20), b''):
                file_hash.update(chunk)
        return file_hash.hexdigest()
