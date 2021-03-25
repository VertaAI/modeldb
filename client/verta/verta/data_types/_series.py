import numbers

from .._internal_utils import arg_handler

from . import _VertaDataType


class Series(_VertaDataType):
    _TYPE_NAME = "series"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value):
        if not all(isinstance(el, numbers.Real) for el in value):
            raise TypeError("`value` must contain all numbers")

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "value": self._value,
            }
        )
