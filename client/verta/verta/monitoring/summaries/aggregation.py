# -*- coding: utf-8 -*-

from __future__ import print_function

from verta._internal_utils import time_utils
from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary
from verta.external import six


class Aggregation(object):

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
        return self._granularity

    @granularity.setter
    def granularity(self, value):
        time_delta = time_utils.parse_duration(value)
        self._granularity = time_delta

    @property
    def operation(self):
        return self._operation

    @operation.setter
    def operation(self, value):
        parsed_operation = self._parse_operation(value)
        self._operation = parsed_operation

    def _to_proto(self):
        granularity_proto = time_utils.timedelta_millis(self.granularity)
        operation_proto = self._OPERATIONS[self.operation]
        return AggregationQuerySummary(
            time_granularity_millis=granularity_proto, operation=operation_proto
        )

    @classmethod
    def _from_proto(cls, msg):
        return cls(msg.time_granularity_millis, msg.operation)

    def __eq__(self, other):
        if not isinstance(other, type(self)):
            return NotImplemented

        return self._to_proto() == other._to_proto()

    @classmethod
    def operations(cls):
        return cls._OPERATIONS.keys()

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
