import numbers

from .._internal_utils import _utils

from . import _VertaDataType


class Matrix(_VertaDataType):
    _TYPE_NAME = "matrix"
    _VERSION = "v1"

    def __init__(self, value):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if not all(isinstance(el, numbers.Real) for row in value for el in row):
            raise TypeError("`value` must contain all numbers")

        self._value = _utils.to_builtin(value)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })
