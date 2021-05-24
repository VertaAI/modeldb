# -*- coding: utf-8 -*-

from __future__ import print_function
from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary
from verta._internal_utils import time_utils


class Aggregation(object):

    _OPERATIONS = {
        k.lower(): v for k, v in AggregationQuerySummary.AggregationOperation.items()
    }

    def __init__(self, granularity, operation):
        self.granularity = granularity
        self.operation = operation

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
        operation_proto = self._OPERATIONS[self.operation]
        granularity_proto = time_utils.timedelta_millis(self.granularity)
        return AggregationQuerySummary(
            time_granularity_millis=granularity_proto, operation=operation_proto
        )

    @classmethod
    def _from_proto(cls, msg):
        return cls(msg.time_granularity_millis, msg.operation)

    @classmethod
    def operations(cls):
        return cls._OPERATIONS.keys()

    @classmethod
    def _parse_operation(cls, value):
        if value in cls._OPERATIONS:
            return value
        if type(value) is str:
            if value in AggregationQuerySummary.AggregationOperation.keys():
                return value.lower()
        if type(value) is int:
            try:
                op_name = AggregationQuerySummary.AggregationOperation.Name(value)
                return op_name.lower()
            except:
                raise ValueError(
                    "Could not parse int value as operation"
                )  # TODO: better error message
        raise ValueError(
            "Could not parse value as operation"
        )  # TODO: better error message
