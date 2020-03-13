# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Dataset_pb2 as _DatasetService

from .._repository import blob


class _Dataset(blob.Blob):
    """
    Base class for dataset versioning. Not for human consumption.

    """
    def __init__(self):
        super(_Dataset, self).__init__()

        self._msg = _DatasetService.DatasetBlob()
