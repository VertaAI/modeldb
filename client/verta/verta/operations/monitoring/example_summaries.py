# -*- coding: utf-8 -*-

from client import Client
from verta._internal_utils._utils import generate_default_name
from clients.summaries import SummaryQuery
import time_utils
from datetime import datetime, timedelta, timezone
import requests
from verta import data_types

# 0. Setup
client = Client("https://dev.verta.ai")
monitored_entity = client.get_or_create_monitored_entity()

# 1. Create a summary

summary_name = "summary_v2_{}".format(generate_default_name())

summary = client.summaries.create(summary_name, "test", monitored_entity)
print("summary {}".format(summary))

# 2. Find a summary by monitored entity

summaries_for_monitored_entity = SummaryQuery(monitored_entities=[monitored_entity])
retrieved_summaries = client.summaries.find(summaries_for_monitored_entity)
print("retrieved_summaries {}".format(retrieved_summaries))

# 3. Log summary samples
now = datetime.now(timezone.utc)
yesterday = now - timedelta(days=1)

discrete_histogram = data_types.DiscreteHistogram(
    buckets=["hotdog", "not hotdog"], data=[100, 20]
)
labels = {"env": "test", "color": "blue"}
summary_sample = summary.log_sample(
    discrete_histogram, labels=labels, time_window_start=yesterday, time_window_end=now
)
print(summary_sample)

float_histogram = data_types.FloatHistogram(
    bucket_limits=[1, 13, 25, 37, 49, 61],
    data=[15, 53, 91, 34, 7],
)
labels2 = {"env": "test", "color": "red"}
summary_sample_2 = summary.log_sample(
    float_histogram, labels=labels2, time_window_start=yesterday, time_window_end=now
)
print(summary_sample_2)

retrieved_label_keys = client.labels.find(summary_query=summaries_for_monitored_entity)
print("retrieved_label_keys {}".format(retrieved_label_keys))

if retrieved_label_keys:
    retrieved_labels = client.labels.find(
        summary_query=summaries_for_monitored_entity, keys=retrieved_label_keys
    )
    print("retrieved_labels {}".format(retrieved_labels))

# 4. Find summary samples by labels
all_samples_for_summary = summary.find_samples()
print("samples {}".format(all_samples_for_summary))

blue_samples = summary.find_samples(labels={"color": ["blue"]})
print("samples {}".format(blue_samples))

all_labels = client.labels.find()
print(all_labels)

all_labels_with_values = client.labels.find(keys=all_labels)
print(all_labels_with_values)

# End. Clean up

# Can't currently delete non-empty domain objects before deleting all dependent objects

# client.summariesV2.delete([summary])
# monitored_entity.delete()
