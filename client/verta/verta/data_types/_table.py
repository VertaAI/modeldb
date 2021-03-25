from .._internal_utils import _utils

from . import _VertaDataType


class Table(_VertaDataType):
    _TYPE_NAME = "table"
    _VERSION = "v1"

    def __init__(self, data, columns):
        if len(set(len(row) for row in data)) != 1:
            raise ValueError("each row in `data` must have same length")
        if len(columns) != len(data[0]):
            raise ValueError("length of `columns` must equal length of rows in `data`")

        self._data = _utils.to_builtin(data)
        self._columns = _utils.to_builtin(columns)

    @classmethod
    def from_pandas(cls, df):
        return cls(df.values.tolist(), df.columns.tolist())

    def _as_dict(self):
        return self._as_dict_inner({
            "rows": self._data,
            "header": self._columns,
        })
