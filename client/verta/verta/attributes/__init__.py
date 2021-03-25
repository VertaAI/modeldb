import abc
import numbers

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _VertaAttribute(object):
    @abc.abstractmethod
    def _as_dict(self):
        raise NotImplementedError

    def _from_dict(self, d):
        pass


class StringValue(_VertaAttribute):
    def __init__(self, value):
        if not isinstance(value, six.string_types):
            raise TypeError("`value` must be a string, not {}".format(type(value)))

        self._value = value

    def _as_dict(self):
        pass


class NumericValue(_VertaAttribute):
    def __init__(self, value, unit=None):
        if not isinstance(value, numbers.Real):
            raise TypeError("`value` must be a number, not {}".format(type(value)))
        if unit and not isinstance(unit, six.string_types):
            raise TypeError("`unit` must be a string, not {}".format(type(unit)))

        self._value = value
        self._unit = unit

    def _as_dict(self):
        pass


class DiscreteHistogram(_VertaAttribute):
    def __init__(self, buckets, data):
        if len(buckets) != len(data):
            raise ValueError("`buckets` and `data` must have the same length")
        if not all(isinstance(bucket, numbers.Real) for bucket in buckets):
            raise TypeError("`buckets` must contain all numbers")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._buckets = buckets
        self._data = data

    def _as_dict(self):
        pass


class FloatHistogram(_VertaAttribute):
    def __init__(self, bucket_limits, data):
        if len(bucket_limits) != len(data) + 1:
            raise ValueError("length of `bucket_limits` must be 1 greater than length of `data`")
        if not all(isinstance(limit, numbers.Real) for limit in bucket_limits):
            raise TypeError("`bucket_limits` must contain all numbers")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`count` must contain all integers")

        self._bucket_limits = bucket_limits
        self._data = data

    def _as_dict(self):
        pass


class Table(_VertaAttribute):
    def __init__(self, data, columns):
        # TODO: support NumPy data
        if len(set(len(row) for row in data)) != 1:
            raise ValueError("each row in `data` must have same length")
        if len(columns) != len(data[0]):
            raise ValueError("length of `columns` must equal length of rows in `data`")

        self._data = data
        self._columns = columns

    def from_pandas(self, df):
        pass

    def _as_dict(self):
        pass


class Matrix(_VertaAttribute):
    def __init__(self, value):
        # TODO: support NumPy value
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if not all(isinstance(el, numbers.Real) for row in value for el in row):
            raise TypeError("`value` must contain all numbers")

        self._value = value

    def _as_dict(self):
        pass


class Series(_VertaAttribute):
    def __init__(self, value):
        # TODO: support NumPy value
        if not all(isinstance(el, numbers.Real) for el in value):
            raise TypeError("`value` must contain all numbers")  # TODO: check if true

        self._value = value

    def _as_dict(self):
        pass


class Line(_VertaAttribute):
    def __init__(self, x, y):
        # TODO: support NumPy x and y
        if len(x) != len(y):
            raise ValueError("`x` and `y` must have the same length")
        if not all(isinstance(el, numbers.Real) for el in x):
            raise TypeError("`x` must contain all numbers")
        if not all(isinstance(el, numbers.Real) for el in y):
            raise TypeError("`y` must contain all numbers")

        self._x = x
        self._y = y

    def from_tuples(self, tuples):
        pass

    def _as_dict(self):
        pass


class ConfusionMatrix(_VertaAttribute):
    def __init__(self, value, labels):
        if len(set(len(row) for row in value)) != 1:
            raise ValueError("each row in `value` must have same length")
        if len(value) != len(value[0]):
            raise ValueError("rows and columns in `value` must have the same length")
        if len(labels) != len(value[0]):
            raise ValueError("length of `columns` must equal length of rows in `value`")
        if not all(isinstance(el, numbers.Real) for row in value for el in row):
            raise TypeError("`value` must contain all numbers")

        self._value = value
        self._labels = labels

    def _as_dict(self):
        pass
