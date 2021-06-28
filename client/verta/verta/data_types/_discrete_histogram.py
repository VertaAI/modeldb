# -*- coding: utf-8 -*-

from __future__ import division

from collections import defaultdict

from ..external import six

from .._internal_utils.importer import maybe_dependency
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
        self._scipy_spatial = maybe_dependency("scipy.spatial")
        if self._scipy_spatial is None:
            raise ImportError("scipy is not installed; try `pip install scipy`")

        if len(buckets) != len(set(buckets)):
            raise ValueError("`buckets` elements must all be unique")
        if len(buckets) != len(data):
            raise ValueError("`buckets` and `data` must have the same length")
        if not all(isinstance(count, six.integer_types) for count in data):
            raise TypeError("`data` must contain all integers")

        self._buckets = buckets
        self._data = data
        self._data_dict = defaultdict(int, zip(buckets, data))

    def __repr__(self):
        attrs = {
            "buckets": self._buckets,
            "data": self._data,
        }
        lines = ["{}: {}".format(key, value) for key, value in sorted(attrs.items())]
        return "\n\t".join([type(self).__name__] + lines)

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
        keys = list(set(self._buckets + other._buckets))
        return self._scipy_spatial.distance.cosine(
            self.normalized_data(keys),
            other.normalized_data(keys),
        )

    def normalized_data(self, keys=None):
        total = sum(self._data)
        keys = keys if keys else self._buckets
        return [self._data_dict[k] / total for k in keys]
