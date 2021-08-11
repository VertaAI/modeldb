# -*- coding: utf-8 -*-

import sys
from datetime import timedelta

import hypothesis.strategies as st  # import builds, sampled_from,
import pytest
from hypothesis import given
from verta.monitoring.summaries.aggregation import Aggregation

from .strategies import aggregation_query_strategy, granularity_strategy, ops_strategy


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
