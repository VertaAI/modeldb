# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Line(_VertaDataType):
    """
    Representation of line plot points.

    Parameters
    ----------
    x : list of float
        Points' x-coordinates.
    y : list of float
        Points' y-coordinates.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import Line
        data = Line(
            x=[1, 2, 3],
            y=[1, 4, 9],
        )
        run.log_attribute("price", data)

    """

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
        """
        Alternate constructor that takes coordinates as tuples.

        Parameters
        ----------
        tuples : list of (float, float)
            Points' coordinates.

        Returns
        -------
        :class:`verta.data_types.Line`

        Examples
        --------
        .. code-block:: python

            from verta.data_types import Line
            data = Line.from_tuples(
                [(1, 1), (2, 4), (3, 9)],
            )
            run.log_attribute("price", data)

        """
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
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(
            x=data["x"],
            y=data["y"],
        )

    def diff(self, other):
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        raise NotImplementedError  # TODO
