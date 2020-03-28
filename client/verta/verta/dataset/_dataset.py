# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._internal_utils import _utils

from .._repository import blob


class _Dataset(blob.Blob):
    """
    Base class for dataset versioning. Not for human consumption.

    """
    def __init__(self):
        super(_Dataset, self).__init__()

        self._msg = _DatasetService.DatasetBlob()

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
        return [
            path_component_msg.path,
            "    {} bytes".format(path_component_msg.size),
            "    last modified {}".format(_utils.timestamp_to_str(path_component_msg.last_modified_at_source)),
            "    checksum: {}".format(path_component_msg.md5),
        ]
