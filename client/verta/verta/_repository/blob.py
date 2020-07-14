# -*- coding: utf-8 -*-

from __future__ import print_function

import abc

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class Blob(object):
    @classmethod
    @abc.abstractmethod
    def _from_proto(cls, blob_msg):
        """
        Returns a blob from `blob_msg`.

        Parameters
        ----------
        blob_msg : _VersioningService.Blob

        Returns
        -------
        blob : class instance

        """
        pass

    @abc.abstractmethod
    def _as_proto(self):
        """
        Returns this blob as a protobuf message.

        Returns
        -------
        blob_msg : _VersioningService.Blob

        """
        pass
