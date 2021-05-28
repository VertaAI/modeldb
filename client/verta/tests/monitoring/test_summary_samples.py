# -*- coding: utf-8 -*-

from datetime import timedelta

import pytest

from verta import data_types
from verta._internal_utils import time_utils
from verta._internal_utils._utils import generate_default_name
from verta.monitoring.summaries.queries import SummaryQuery, SummarySampleQuery
from verta.monitoring.summaries.aggregation import Aggregation


class TestNumericSummarySamples:

    values = list(range(1, 5))
    numeric_values = map(data_types.NumericValue, values)
    now = time_utils.now()
    yesterday = now - timedelta(days=1)
    labels = {}

    @pytest.fixture(scope="class")
    def summary_entity(self, class_client):
        return class_client.monitoring.get_or_create_monitored_entity()

    @pytest.fixture(scope="class")
    def numeric_summary(self, class_client, summary_entity):
        return class_client.monitoring.summaries.create(
            "test_numeric_summary", data_types.NumericValue, summary_entity
        )

    @pytest.fixture(scope="class")
    def numeric_samples(self, class_client, numeric_summary):
        created = []
        for numeric in self.numeric_values:
            logged = numeric_summary.log_sample(
                numeric, self.labels, self.yesterday, self.now
            )
            created.append(logged)
        return created

    def test_find_summary_samples(self, class_client, numeric_summary, numeric_samples):
        found_samples = numeric_summary.find_samples()
        assert len(found_samples) == len(self.values)

        created_ids = list(map(lambda sample: sample.id, numeric_samples))
        found_ids = list(map(lambda sample: sample.id, found_samples))
        for id in found_ids:
            assert id in created_ids

    def test_aggregate_summary_samples(
        self, class_client, numeric_summary, numeric_samples
    ):
        found_samples = numeric_summary.find_samples(
            SummarySampleQuery(aggregation=Aggregation("1d", "sum"))
        )
        assert len(found_samples) == 1
        aggregated_sample = found_samples[0]
        assert aggregated_sample.content == data_types.NumericValue(sum(self.values))
