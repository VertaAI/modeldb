# -*- coding: utf-8 -*-

from .._internal_utils import arg_handler

from . import _VertaDataType


class Table(_VertaDataType):
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
        return cls(df.values.tolist(), df.columns.tolist())

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "rows": self._data,
                "header": self._columns,
            }
        )
