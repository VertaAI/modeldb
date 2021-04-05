import numbers

from ..external import six

from .._internal_utils import arg_handler

from . import _VertaDataType


class NumericValue(_VertaDataType):
    _TYPE_NAME = "numericValue"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value, unit=None):
        if not isinstance(value, numbers.Real):
            raise TypeError("`value` must be a number, not {}".format(type(value)))
        if unit and not isinstance(unit, six.string_types):
            raise TypeError("`unit` must be a string, not {}".format(type(unit)))

        self._value = value
        self._unit = unit

    def _as_dict(self):
        data = {"value": self._value}
        if self._unit:
            data["unit"] = self._unit
        return self._as_dict_inner(data)
