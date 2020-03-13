# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Code_pb2 as _CodeService

from .._repository import blob


class _Code(blob.Blob):
    def __init__(self):
        """
        Base class for code versioning. Not for human consumption.

        """
        super(_Code, self).__init__()

        self._msg = _CodeService.CodeBlob()
