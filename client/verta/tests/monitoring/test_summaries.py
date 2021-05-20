# -*- coding: utf-8 -*-

import datetime
from datetime import timedelta

import pytest
from verta import data_types
from verta._internal_utils import time_utils
from verta._internal_utils._utils import generate_default_name
from verta.monitoring.summaries.queries import SummaryQuery, SummarySampleQuery
from verta.monitoring.summaries.summary import Summary
from verta.monitoring.summaries.summary_sample import SummarySample


class TestSummaries(object):
    def test_summary_labels(self, client):
        pytest.importorskip("scipy")

        summaries = client.monitoring.summaries

        monitored_entity = client.monitoring.get_or_create_monitored_entity()
        summary_name = "summary_v2_{}".format(generate_default_name())
        summary = summaries.create(
            summary_name, data_types.DiscreteHistogram, monitored_entity
        )

        assert isinstance(summary, Summary)

        summaries_for_monitored_entity = SummaryQuery(
            monitored_entities=[monitored_entity]
        )
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
            discrete_histogram,
            labels=labels,
            time_window_start=yesterday,
            time_window_end=now,
        )
        assert isinstance(summary_sample, SummarySample)

        float_histogram = data_types.FloatHistogram(
            bucket_limits=[1, 13, 25, 37, 49, 61],
            data=[15, 53, 91, 34, 7],
        )
        labels2 = {"env": "test", "color": "red"}
        with pytest.raises(TypeError):
            summary_sample_2 = summary.log_sample(
                float_histogram,
                labels=labels2,
                time_window_start=yesterday,
                time_window_end=now,
            )

        labels = client.monitoring.labels

        retrieved_label_keys = labels.find_keys(
            summary_query=summaries_for_monitored_entity
        )
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

    def test_summary_get_or_create(self, client):
        summaries = client.monitoring.summaries

        monitored_entity = client.monitoring.get_or_create_monitored_entity()
        summary_name = "summary:{}".format(generate_default_name())
        created_summary = summaries.get_or_create(
            summary_name, data_types.DiscreteHistogram, monitored_entity
        )
        retrieved_summary = summaries.get_or_create(
            summary_name, data_types.DiscreteHistogram, monitored_entity
        )
        assert created_summary.id == retrieved_summary.id


class TestSummarySampleQuery:
    def test_creation_datetime(self):
        time_window_start = time_utils.now() - datetime.timedelta(weeks=1)
        time_window_end = time_utils.now() - datetime.timedelta(days=1)
        created_after = time_utils.now() - datetime.timedelta(hours=1)
        time_window_start_millis = time_utils.epoch_millis(time_window_start)
        time_window_end_millis = time_utils.epoch_millis(time_window_end)
        created_after_millis = time_utils.epoch_millis(created_after)

        # as datetime
        sample_query = SummarySampleQuery(
            time_window_start=time_window_start,
            time_window_end=time_window_end,
            created_after=created_after,
        )
        proto_request = sample_query._to_proto_request()
        assert (
            proto_request.filter.time_window_start_at_millis == time_window_start_millis
        )
        assert proto_request.filter.time_window_end_at_millis == time_window_end_millis
        assert proto_request.filter.created_at_after_millis == created_after_millis

        # as millis
        sample_query = SummarySampleQuery(
            time_window_start=time_window_start_millis,
            time_window_end=time_window_end_millis,
            created_after=created_after_millis,
        )
        proto_request = sample_query._to_proto_request()
        assert (
            proto_request.filter.time_window_start_at_millis == time_window_start_millis
        )
        assert proto_request.filter.time_window_end_at_millis == time_window_end_millis
        assert proto_request.filter.created_at_after_millis == created_after_millis
