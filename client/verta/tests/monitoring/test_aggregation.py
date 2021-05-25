# -*- coding: utf-8 -*-

import sys

import pytest
from datetime import timedelta
from hypothesis import given
import hypothesis.strategies as st  # import builds, sampled_from,
from verta._internal_utils.time_utils import timedelta_millis


from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary

from verta.monitoring.summaries.aggregation import Aggregation

ProtoOperations = AggregationQuerySummary.AggregationOperation

millis_timedelta_strategy = st.timedeltas(min_value=timedelta(milliseconds=1))

millis_uint64_strategy = millis_timedelta_strategy.map(timedelta_millis)

aggregation_ops_strategy = st.sampled_from(ProtoOperations.values())
aggregation_query_strategy = st.builds(
    AggregationQuerySummary,
    time_granularity_millis=millis_uint64_strategy,
    operation=aggregation_ops_strategy,
)

granularity_strategy = st.one_of(
    st.sampled_from(["30m", "1d"]), millis_timedelta_strategy, millis_uint64_strategy
)

op_str_strategy = st.one_of(
    st.sampled_from(ProtoOperations.values()),
    st.sampled_from(ProtoOperations.keys()),
    st.sampled_from(Aggregation.operations()),
)

ops_strategy = st.one_of(
    aggregation_ops_strategy,
    st.sampled_from(AggregationQuerySummary.AggregationOperation.keys()),
)


class TestAggregation(object):
    @given(aggregation_msg=aggregation_query_strategy)
    def test_from_proto(self, aggregation_msg):
        aggregation = Aggregation._from_proto(aggregation_msg)
        assert isinstance(aggregation, Aggregation)

    @given(granularity=granularity_strategy, operation=ops_strategy)
    def test_to_proto(self, granularity, operation):
        aggregation = Aggregation(granularity, operation)
        msg = aggregation._to_proto()
        decoded = Aggregation._from_proto(msg)
        assert aggregation == decoded
