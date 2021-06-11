# -*- coding: utf-8 -*-

import abc

from ..external import six

from .._internal_utils import _file_utils


@six.add_metaclass(abc.ABCMeta)
class _VertaDataType(object):
    """
    Base class for complex structured data types. Not for external use.

    """

    _TYPE_NAME = None
    _VERSION = None

    def __eq__(self, other):
        if type(self) is not type(other):
            return NotImplemented
        return self.__dict__ == other.__dict__

    def __repr__(self):
        attrs = {
            _file_utils.remove_prefix(key, "_"): value
            for key, value in self.__dict__.items()
        }
        lines = ["{}: {}".format(key, value) for key, value in sorted(attrs.items())]
        return "\n\t".join([type(self).__name__] + lines)

    @classmethod
    def _type_string(cls):
        return "verta.{}.{}".format(cls._TYPE_NAME, cls._VERSION)

    def _as_dict_inner(self, data):
        return {
            "type": self._type_string(),
            self._TYPE_NAME: data,
        }

    @abc.abstractmethod
    def _as_dict(self):
        raise NotImplementedError

    # TODO: _from_dict_inner() should be an abstract class method, but need to
    #       figure out how to do that in Python 2
    @classmethod
    def _from_dict(cls, d):
        # TODO: when we have v2 onwards, make sure old v are still supported
        d_type = d["type"]
        for subcls in cls.__subclasses__():
            if d_type == subcls._type_string():
                return subcls._from_dict_inner(d)

        raise ValueError("data type {} not recognized".format(d_type))

    @classmethod
    def _from_type_string(cls, type_string):
        for subcls in cls.__subclasses__():
            if type_string == subcls._type_string():
                return subcls  # NOTE: intentionally returns the type rather than an instance
        return None

    @classmethod
    def _from_type_strings(cls, type_strings):
        return map(cls._from_type_string, type_strings)

    @abc.abstractmethod
    def diff(self, other):
        raise NotImplementedError
