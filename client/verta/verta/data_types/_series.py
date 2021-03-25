# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Series(_VertaDataType):
    """
    Representation of a numerical series.

    Parameters
    ----------
    value : list of float
        Series values.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import Series
        data = Series([1, 1, 2, 3, 5])
        run.log_attribute("series", data)

    """

    _TYPE_NAME = "series"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, value):
        if not arg_handler.contains_only_numbers(value):
            raise TypeError("`value` must contain only numbers")

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "value": self._value,
            }
        )
