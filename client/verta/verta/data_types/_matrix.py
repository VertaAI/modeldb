# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Matrix(_VertaDataType):
    """
    Representation of a matrix.

    Parameters
    ----------
    value : list of list of float
        Matrix values.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import Matrix
        data = Matrix([
            [1, 2, 3],
            [4, 5, 6],
            [7, 8, 9],
        ])
        run.log_attribute("matrix", data)

    """

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

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(value=data["value"])

    def diff(self, other):
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        raise NotImplementedError  # TODO
