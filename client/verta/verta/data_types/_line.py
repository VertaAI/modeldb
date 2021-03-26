# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Line(_VertaDataType):
    _TYPE_NAME = "line"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, x, y):
        if len(x) != len(y):
            raise ValueError("`x` and `y` must have the same length")
        if not arg_handler.contains_only_numbers(x):
            raise TypeError("`x` must contain only numbers")
        if not arg_handler.contains_only_numbers(y):
            raise TypeError("`y` must contain only numbers")

        self._x = x
        self._y = y

    @classmethod
    def from_tuples(cls, tuples):
        x, y = zip(*tuples)
        return cls(x, y)

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "x": self._x,
                "y": self._y,
            }
        )

    @classmethod
    def _from_dict(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(
            x=data["x"],
            y=data["y"],
        )
