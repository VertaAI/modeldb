import numbers

from ..external import six

from .._internal_utils import _utils

from . import _VertaDataType


class FloatHistogram(_VertaDataType):
    _TYPE_NAME = "floatHistogram"
    _VERSION = "v1"

    def __init__(self, bucket_limits, data):
        # TODO: convert to builtin prior to checks

        if len(bucket_limits) != len(data) + 1:
            raise ValueError("length of `bucket_limits` must be 1 greater than length of `data`")
        if not all(isinstance(limit, numbers.Real) for limit in bucket_limits):
            raise TypeError("`bucket_limits` must contain all numbers")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._bucket_limits = _utils.to_builtin(bucket_limits)
        self._data = _utils.to_builtin(data)

    def _as_dict(self):
        return self._as_dict_inner({
            "bucketLimits": self._bucket_limits,
            "data": self._data,
        })
