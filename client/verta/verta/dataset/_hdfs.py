# -*- coding: utf-8 -*-

from __future__ import print_function

import functools
import hashlib

from ._path import Path

from ..external import six

from . import _dataset

_HDFS_PREFIX = "hdfs://"

class HDFSPath(Path):
    """
    Captures metadata about files from HDFS.

    Parameters
    ----------
    hdfs_client : hdfs.client.Client
        Instance of HDFS client.
    paths : list of strs
        List of paths to files from HDFS.
    base_path : str, optional
        Directory path to be removed from the beginning of `paths` before saving to ModelDB.

    """
    # TODO: support mdb versioning
    def __init__(self, hdfs_client, paths, base_path=None):
        self.client = hdfs_client

        if isinstance(paths, six.string_types):
            paths = [paths]

        filepaths = set()
        for path in paths:
            previous_length = len(filepaths)
            for base, dirs, files in self.client.walk(path):
                for filename in files:
                    filepaths.add(base + "/" + filename)

            # If the path was a file, then we didn't add any new entry and we should add the file
            if len(filepaths) == previous_length:
                filepaths.add(path)

        paths = sorted(list(filepaths))

        paths = list(map(lambda path: _HDFS_PREFIX+path if not path.startswith(_HDFS_PREFIX) else path, paths))
        if base_path and not base_path.startswith(_HDFS_PREFIX):
            base_path = _HDFS_PREFIX+base_path

        super(HDFSPath, self).__init__(paths, base_path)

    def _file_to_component(self, filepath):
        original_filepath = filepath
        filepath = filepath[len(_HDFS_PREFIX):]  # prefix prepended in init
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

    @staticmethod
    def with_spark(sc, paths):
        """
        Creates an HDFSPath blob with a SparkContext instance.

        Parameters
        ----------
        sc : pyspark.SparkContext
            SparkContext instance.
        paths : list of strs
            List of paths to binary input data file(s) from HDFS.

        Returns
        -------
        :class:`~verta.dataset.HDFSPath`
            HDFSPath blob capturing the metadata of the binary files.

        """
        if isinstance(paths, six.string_types):
            paths = [paths]

        rdds = list(map(sc.binaryFiles, paths))
        rdd = functools.reduce(lambda a,b: a.union(b), rdds)

        def get_component(entry):
            filepath, content = entry
            h = hashlib.md5(content).hexdigest()
            return _dataset.Component(
                path=filepath,
                size=len(content),
                # last_modified=metadata['modificationTime'], # handle timezone?
                md5=hashlib.md5(content).hexdigest(),
            )

        result = rdd.map(get_component)
        result = result.collect()
        obj = HDFSPath(None, [])
        obj._components_map.update({
            component.path: component
            for component
            in result
        })
        return obj
