# -*- coding: utf-8 -*-

from __future__ import division

import collections

from ..external import six

from .._internal_utils import arg_handler
from .._internal_utils.importer import maybe_dependency

from . import _VertaDataType


class FloatHistogram(_VertaDataType):
    """
    Representation of a float histogram.

    Parameters
    ----------
    bucket_limits : list of float
        Boundary values between buckets.
    data : list of int
        Counts for each bucket.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import FloatHistogram
        data = FloatHistogram(
            bucket_limits=[1, 13, 25, 37, 49, 61],
            data=[15, 53, 91, 34, 7],
        )
        run.log_attribute("age_histogram", data)

    """

    _TYPE_NAME = "floatHistogram"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, bucket_limits, data):
        self._scipy_spatial = maybe_dependency("scipy.spatial")
        if self._scipy_spatial is None:
            raise ImportError("scipy is not installed; try `pip install scipy`")

        if len(bucket_limits) != len(data) + 1:
            raise ValueError(
                "length of `bucket_limits` must be 1 greater than length of `data`"
            )
        if not arg_handler.contains_only_numbers(bucket_limits):
            raise TypeError("`bucket_limits` must contain only numbers")
        if not list(bucket_limits) == sorted(bucket_limits):
            raise ValueError("`bucket_limits` must be in ascending order")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._bucket_limits = bucket_limits
        self._data = data

    def __repr__(self):
        attrs = {
            "bucket_limits": self._bucket_limits,
            "data": self._data,
        }
        lines = ["{}: {}".format(key, value) for key, value in sorted(attrs.items())]
        return "\n\t".join([type(self).__name__] + lines)

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "bucketLimits": self._bucket_limits,
                "data": self._data,
            }
        )

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(bucket_limits=data["bucketLimits"], data=data["data"])

    def diff(self, other):
        """Calculate the difference between `other` and this value.

        Parameters
        ----------
        other : :class:`FloatHistogram`
            Value to calculate difference from.

        Returns
        -------
        float
            Cosine distance between the normalized bucket values.

        """
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        if self._bucket_limits != other._bucket_limits:
            raise ValueError(
                "bucket limits must match (self: {}, other: {})".format(
                    self._bucket_limits, other._bucket_limits,
                )
            )

        return self._scipy_spatial.distance.cosine(
            self.normalized_data(),
            other.normalized_data(),
        )

    def normalized_data(self):
        total = sum(self._data)
        return [x / total for x in self._data]
