import numbers

from .._internal_utils import _utils

from . import _VertaDataType


class Series(_VertaDataType):
    _TYPE_NAME = "series"
    _VERSION = "v1"

    def __init__(self, value):
        if not all(isinstance(el, numbers.Real) for el in value):
            raise TypeError("`value` must contain all numbers")

        self._value = _utils.to_builtin(value)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })
