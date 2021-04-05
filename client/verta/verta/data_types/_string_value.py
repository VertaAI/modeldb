from ..external import six

from .._internal_utils import _utils

from . import _VertaDataType


class StringValue(_VertaDataType):
    _TYPE_NAME = "stringValue"
    _VERSION = "v1"

    def __init__(self, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))

        self._value = _utils.to_builtin(value)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })
