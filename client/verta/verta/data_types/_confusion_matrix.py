import numbers

from .._internal_utils import _utils

from . import _VertaDataType


class ConfusionMatrix(_VertaDataType):
    _TYPE_NAME = "confusionMatrix"
    _VERSION = "v1"

    def __init__(self, value, labels):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if len(value) != len(value[0]):
            raise ValueError("rows and columns in `value` must have the same length")
        if len(labels) != len(value[0]):
            raise ValueError("length of `columns` must equal length of rows in `value`")
        if not all(isinstance(el, numbers.Real) for row in value for el in row):
            raise TypeError("`value` must contain all numbers")

        self._value = _utils.to_builtin(value)
        self._labels = _utils.to_builtin(labels)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
            "labels": self._labels,
        })
