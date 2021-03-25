import numbers

from .._internal_utils import _utils

from . import _VertaDataType


class Line(_VertaDataType):
    _TYPE_NAME = "line"
    _VERSION = "v1"

    def __init__(self, x, y):
        if len(x) != len(y):
            raise ValueError("`x` and `y` must have the same length")
        if not all(isinstance(el, numbers.Real) for el in x):
            raise TypeError("`x` must contain all numbers")
        if not all(isinstance(el, numbers.Real) for el in y):
            raise TypeError("`y` must contain all numbers")

        self._x = _utils.to_builtin(x)
        self._y = _utils.to_builtin(y)

    @classmethod
    def from_tuples(cls, tuples):
        x, y = zip(*tuples)
        return cls(x, y)

    def _as_dict(self):
        return self._as_dict_inner({
            "x": self._x,
            "y": self._y,
        })
