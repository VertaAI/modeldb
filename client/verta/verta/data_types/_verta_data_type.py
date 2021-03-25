# -*- coding: utf-8 -*-

import abc

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _VertaDataType(object):
    """
    Base class for complex structured data types. Not for external use.

    """

    _TYPE_NAME = None
    _VERSION = None

    def _as_dict_inner(self, data):
        return {
            "type": "verta.{}.{}".format(
                self._TYPE_NAME,
                self._VERSION,
            ),
            self._TYPE_NAME: data,
        }

    @abc.abstractmethod
    def _as_dict(self):
        raise NotImplementedError

    def _from_dict(self, d):
        pass
