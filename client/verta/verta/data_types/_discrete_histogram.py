# -*- coding: utf-8 -*-

import collections

from ..external import six

from scipy import spatial

from .._internal_utils import arg_handler

from . import _VertaDataType


class DiscreteHistogram(_VertaDataType):
    """
    Representation of a discrete histogram.

    Parameters
    ----------
    buckets : list of str
        Bucket labels.
    data : list of int
        Counts for each bucket.

    Examples
    --------
    .. code-block:: python

        from verta.data_types import DiscreteHistogram
        data = DiscreteHistogram(
            buckets=["yes", "no"],
            data=[10, 20],
        )
        run.log_attribute("response_histogram", data)

    """

    _TYPE_NAME = "discreteHistogram"
    _VERSION = "v1"

    @arg_handler.args_to_builtin(ignore_self=True)
    def __init__(self, buckets, data):
        if len(buckets) != len(data):
            raise ValueError("`buckets` and `data` must have the same length")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._buckets = buckets
        self._data = data

    def _as_dict(self):
        return self._as_dict_inner(
            {
                "buckets": self._buckets,
                "data": self._data,
            }
        )

    @classmethod
    def _from_dict_inner(cls, d):
        data = d[cls._TYPE_NAME]
        return cls(
            buckets=data["buckets"],
            data=data["data"],
        )

    def dist(self, other):
        if not isinstance(other, type(self)):
            raise TypeError(
                "`other` must be type {}, not {}".format(type(self), type(other))
            )

        # TODO: assuming labels are consistent
        if collections.Counter(self._buckets) != collections.Counter(other._buckets):
            raise ValueError(
                "buckets must match (self: {}, other: {})".format(
                    self._buckets, other._buckets,
                )
            )
        else:
            # TODO: assuming order of labels is consistent
            # normalize
            self_normalized = self.normalize()
            other_normalized = other.normalize()
            return spatial.distance.cosine(self_normalized, other_normalized)

    def normalize(self):
        total = sum(self._data)
        return [x * 1.0 / total for x in self._data]
