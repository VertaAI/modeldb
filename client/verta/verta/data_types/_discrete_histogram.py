from ..external import six

from .._internal_utils import _utils

from . import _VertaDataType


class DiscreteHistogram(_VertaDataType):
    _TYPE_NAME = "discreteHistogram"
    _VERSION = "v1"

    def __init__(self, buckets, data):
        if len(buckets) != len(data):
            raise ValueError("`buckets` and `data` must have the same length")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._buckets = _utils.to_builtin(buckets)
        self._data = _utils.to_builtin(data)

    def _as_dict(self):
        return self._as_dict_inner({
            "buckets": self._buckets,
            "data": self._data,
        })
