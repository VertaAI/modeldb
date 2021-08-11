# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Table(_VertaDataType):
    """
    Representation of a table.

    Parameters
    ----------
    data : list of list
        Tabular data.
    columns : list of str
        Column names.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import Table
        data = Table(
            data=[[1, 24, "blue"], [2, 36, "red"]],
            columns=["id", "height", "color"],
        )
        run.log_attribute("measurements", data)

    """

    _TYPE_NAME = "table"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, data, columns):
        if len(set(len(row) for row in data)) != 1:
            raise ValueError("each row in `data` must have same length")
        if len(columns) != len(data[0]):
            raise ValueError("length of `columns` must equal length of rows in `data`")

        self._data = data
        self._columns = columns

    @classmethod
    def from_pandas(cls, df):
        """
        Alternate constructor that takes a pandas DataFrame.

        Parameters
        ----------
        df : :class:`pandas.DataFrame`
            DataFrame.

        Returns
        -------
        :class:`verta.data_types.Table`

        """
        return cls(df.values.tolist(), df.columns.tolist())

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "rows": self._data,
                "header": self._columns,
            }
        )

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(
            data=data["rows"],
            columns=data["header"],
        )

    def diff(self, other):
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        raise NotImplementedError  # TODO
