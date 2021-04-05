from ..external import six

from .._internal_utils import arg_handler

from . import _VertaDataType


class StringValue(_VertaDataType):
    _TYPE_NAME = "stringValue"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })
