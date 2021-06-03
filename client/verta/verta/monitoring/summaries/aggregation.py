# -*- coding: utf-8 -*-

from __future__ import print_function

from verta._internal_utils import time_utils
from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary
from verta.external import six


class Aggregation(object):
    """A query object specifying how summary samples should be aggregated.

    Attributes
    ----------
    granularity : :class:`datetime.timedelta`
        The granularity of aggregation.
    operation : str
        The aggregation operation to perform.

    Examples
    --------
    .. code-block:: python

        from datetime import timedelta
        from verta.monitoring.summaries.aggregation import Aggregation

        weekly_agg = Aggregation(timedelta(days=7), "sum")
        hourly_agg = Aggregation("1h", "sum")
        minute_agg = Aggregation(1000 * 60, "sum")

    """

    _OPERATIONS = {
        k.lower(): v for k, v in AggregationQuerySummary.AggregationOperation.items()
    }

    def __init__(self, granularity, operation):
        self.granularity = granularity
        self.operation = operation

    def __repr__(self):
        return "Aggregation('{}',{})".format(self.granularity, self.operation)

    @property
    def granularity(self):
        """The granularity of aggregation.

        :getter: Returns the specified time granularity of aggregation
        :setter: Sets the time granularity from a timedelta, string, or int of epoch milliseconds.
        """
        return self._granularity

    @granularity.setter
    def granularity(self, value):
        time_delta = time_utils.parse_duration(value)
        self._granularity = time_delta

    @property
    def operation(self):
        """The operation of aggregation used to combine summary samples.

        :getter: Returns the operation
        :setter: Sets the time operation from a string
        """
        return self._operation

    @operation.setter
    def operation(self, value):
        parsed_operation = self._parse_operation(value)
        self._operation = parsed_operation

    def _to_proto(self):
        granularity_proto = time_utils.duration_millis(self.granularity)
        operation_proto = self._OPERATIONS[self.operation]
        return AggregationQuerySummary(
            time_granularity_millis=granularity_proto, operation=operation_proto
        )

    @classmethod
    def _from_proto(cls, msg):
        if msg.time_granularity_millis == 0:
            return None
        else:
            return cls(msg.time_granularity_millis, msg.operation)

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._to_proto() == other._to_proto()

    @classmethod
    def operations(cls):
        """Return the set of supported aggregation operations.

        Returns
        -------
        set of str
            A set of valid aggregation operations.
        """
        return set(cls._OPERATIONS.keys())

    @classmethod
    def _parse_operation(cls, value):
        if value in cls._OPERATIONS:
            return value
        if isinstance(value, six.string_types):
            value = six.ensure_str(value)
            if value in AggregationQuerySummary.AggregationOperation.keys():
                return value.lower()
        if type(value) is int:
            try:
                op_name = AggregationQuerySummary.AggregationOperation.Name(value)
                return op_name.lower()
            except:
                raise ValueError("could not parse int value as operation")
        raise ValueError("could not parse value as operation")
