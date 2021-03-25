import abc
import numbers

from ..external import six

from .._internal_utils import (
    _utils,
    importer,
)


@six.add_metaclass(abc.ABCMeta)
class _VertaAttribute(object):
    _TYPE_NAME = None
    _VERSION = None

    def _as_dict_inner(self, data):
        return {
            "type": "verta.{}.{}".format(
                self._TYPE_NAME, self._VERSION,
            ),
            self._TYPE_NAME: data,
        }

    @abc.abstractmethod
    def _as_dict(self):
        raise NotImplementedError

    def _from_dict(self, d):
        pass


class StringValue(_VertaAttribute):
    _TYPE_NAME = "stringValue"
    _VERSION = "v1"

    def __init__(self, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))

        self._value = value

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })


class NumericValue(_VertaAttribute):
    _TYPE_NAME = "numericValue"
    _VERSION = "v1"

    def __init__(self, value, unit=None):
        if not isinstance(value, numbers.Real):
            raise TypeError("`value` must be a number, not {}".format(type(value)))
        if unit and not isinstance(unit, six.string_types):
            raise TypeError("`unit` must be a string, not {}".format(type(unit)))

        self._value = value
        self._unit = unit

    def _as_dict(self):
        data = {"value": self._value}
        if self._unit:
            data["unit"] = self._unit
        return self._as_dict_inner(data)


class DiscreteHistogram(_VertaAttribute):
    _TYPE_NAME = "discreteHistogram"
    _VERSION = "v1"

    def __init__(self, buckets, data):
        if len(buckets) != len(data):
            raise ValueError("`buckets` and `data` must have the same length")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._buckets = buckets
        self._data = _utils.to_builtin(data)

    def _as_dict(self):
        return self._as_dict_inner({
            "buckets": self._buckets,
            "data": self._data,
        })


class FloatHistogram(_VertaAttribute):
    _TYPE_NAME = "floatHistogram"
    _VERSION = "v1"

    def __init__(self, bucket_limits, data):
        if len(bucket_limits) != len(data) + 1:
            raise ValueError("length of `bucket_limits` must be 1 greater than length of `data`")
        if not all(isinstance(limit, numbers.Real) for limit in bucket_limits):
            raise TypeError("`bucket_limits` must contain all numbers")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`count` must contain all integers")

        self._bucket_limits = bucket_limits
        self._data = _utils.to_builtin(data)

    def _as_dict(self):
        return self._as_dict_inner({
            "bucketLimits": self._bucket_limits,
            "data": self._data,
        })


class Table(_VertaAttribute):
    _TYPE_NAME = "table"
    _VERSION = "v1"

    def __init__(self, data, columns):
        if len(set(len(row) for row in data)) != 1:
            raise ValueError("each row in `data` must have same length")
        if len(columns) != len(data[0]):
            raise ValueError("length of `columns` must equal length of rows in `data`")

        self._data = _utils.to_builtin(data)
        self._columns = columns

    @classmethod
    def from_pandas(cls, df):
        return cls(df.values.tolist(), df.columns.tolist())

    def _as_dict(self):
        return self._as_dict_inner({
            "rows": self._data,
            "header": self._columns,
        })


class Matrix(_VertaAttribute):
    _TYPE_NAME = "matrix"
    _VERSION = "v1"

    def __init__(self, value):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if not all(isinstance(el, numbers.Real) for row in value for el in row):
            raise TypeError("`value` must contain all numbers")

        self._value = _utils.to_builtin(value)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })


class Series(_VertaAttribute):
    _TYPE_NAME = "series"
    _VERSION = "v1"

    def __init__(self, value):
        if not all(isinstance(el, numbers.Real) for el in value):
            raise TypeError("`value` must contain all numbers")

        self._value = _utils.to_builtin(value)

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
        })


class Line(_VertaAttribute):
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


class ConfusionMatrix(_VertaAttribute):
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
        self._labels = labels

    def _as_dict(self):
        return self._as_dict_inner({
            "value": self._value,
            "labels": self._labels,
        })
