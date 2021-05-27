# -*- coding: utf-8 -*-

import sys
from datetime import timedelta

import hypothesis.strategies as st
import pytest
from hypothesis import given
from verta._internal_utils.time_utils import timedelta_millis
from verta._protos.public.monitoring.Summary_pb2 import AggregationQuerySummary
from verta.monitoring.summaries.aggregation import Aggregation
from ..time_strategies import millis_timedelta_strategy, millis_uint64_strategy

ProtoOperations = AggregationQuerySummary.AggregationOperation

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
