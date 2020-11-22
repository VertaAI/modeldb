# -*- coding: utf-8 -*-

from __future__ import print_function

import hashlib

from ._path import Path

from . import _dataset

class HDFSPath(Path):
    # TODO: support mdb versioning
    def __init__(self, hdfs_client, paths, base_path=None):
        self.client = hdfs_client

        paths = list(map(lambda path: "hdfs:/"+path if not path.startswith("hdfs:/") else path, paths))
        if base_path and not base_path.startswith("hdfs:/"):
            base_path = "hdfs:/"+base_path

        super(HDFSPath, self).__init__(paths, base_path)

    def _file_to_component(self, filepath):
        metadata = self.client.status(filepath)
        return _dataset.Component(
            path=filepath,
            size=metadata['length'],
            last_modified=metadata['modificationTime'],
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
        return
        file_hash = hashlib.md5()
        with self.client.read(filepath) as f:
            for chunk in iter(lambda: f.read(2**20), b''):
                file_hash.update(chunk)
        return file_hash.hexdigest()
