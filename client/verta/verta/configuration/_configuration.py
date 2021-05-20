# -*- coding: utf-8 -*-

from __future__ import print_function

from .._protos.public.modeldb.versioning import Config_pb2 as _ConfigService

from ..repository import _blob


class _Configuration(_blob.Blob):
    """
    Base class for configuration versioning. Not for human consumption.

    """
    def __init__(self):
        super(_Configuration, self).__init__()

        # TODO: don't use proto to store data
        self._msg = _ConfigService.ConfigBlob()
