# -*- coding: utf-8 -*-

from __future__ import print_function

from verta import _blob

from .._protos.public.modeldb.versioning import Code_pb2 as _CodeService


class _Code(_blob.Blob):
    def __init__(self):
        """
        Base class for code versioning. Not for human consumption.

        """
        super(_Code, self).__init__()

        # TODO: don't use proto to store data
        self._msg = _CodeService.CodeBlob()
