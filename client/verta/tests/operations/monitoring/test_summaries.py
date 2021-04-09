# -*- coding: utf-8 -*-

from verta._internal_utils._utils import generate_default_name
from verta.operations.monitoring.summaries import (
    Summary,
    SummaryQuery,
    SummarySample,
    SummarySampleQuery,
)
from verta._internal_utils import time_utils
from datetime import timedelta
import requests
from verta import data_types
import pytest

class TestSummaries(object):

    def test_summary_labels(self, client):
        summaries = client.operations.summaries

        monitored_entity = client.operations.get_or_create_monitored_entity()
        summary_name = "summary_v2_{}".format(generate_default_name())
        summary = summaries.create(summary_name, data_types.DiscreteHistogram, monitored_entity)

        assert isinstance(summary, Summary)

        summaries_for_monitored_entity = SummaryQuery(monitored_entities=[monitored_entity])
        retrieved_summaries = summaries.find(summaries_for_monitored_entity)
        assert len(retrieved_summaries) > 0
        for s in retrieved_summaries:
            assert isinstance(s, Summary)

        now = time_utils.now()
        yesterday = now - timedelta(days=1)

        discrete_histogram = data_types.DiscreteHistogram(
            buckets=["hotdog", "not hotdog"], data=[100, 20]
        )
        labels = {"env": "test", "color": "blue"}
        summary_sample = summary.log_sample(
            discrete_histogram, labels=labels, time_window_start=yesterday, time_window_end=now
        )
        assert isinstance(summary_sample, SummarySample)

        with pytest.raises(TypeError):
            float_histogram = data_types.FloatHistogram(
                bucket_limits=[1, 13, 25, 37, 49, 61],
                data=[15, 53, 91, 34, 7],
            )
            labels2 = {"env": "test", "color": "red"}
            summary_sample_2 = summary.log_sample(
                float_histogram, labels=labels2, time_window_start=yesterday, time_window_end=now
            )
            print("this should not be printed, and an error should be raised")


        labels = client.operations.labels

        retrieved_label_keys = labels.find_keys(summary_query=summaries_for_monitored_entity)
        assert len(retrieved_label_keys) > 0

        if retrieved_label_keys:
            retrieved_labels = labels.find_values(
                summary_query=summaries_for_monitored_entity, keys=retrieved_label_keys
            )
            for key in retrieved_label_keys:
                assert key in retrieved_labels

        all_samples_for_summary = summary.find_samples()
        assert len(all_samples_for_summary) == 1

        blue_samples = summary.find_samples(
            SummarySampleQuery(labels={"color": ["blue"]}),
        )
        assert len(blue_samples) == 1
