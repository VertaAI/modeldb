# -*- coding: utf-8 -*-

from ..external import six

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
