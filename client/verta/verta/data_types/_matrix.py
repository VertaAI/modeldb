# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Matrix(_VertaDataType):
    _TYPE_NAME = "matrix"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if not arg_handler.contains_only_numbers(value):
            raise TypeError("`value` must contain only numbers")

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "value": self._value,
            }
        )
