# -*- coding: utf-8 -*-

import datetime

import pytest
from verta import data_types
from verta._internal_utils import _utils, time_utils


@pytest.fixture
def monitored_entity(client, created_entities):
    name = _utils.generate_default_name()
    monitored_entity = client.monitoring.get_or_create_monitored_entity(name)
    created_entities.append(monitored_entity)

    return monitored_entity


@pytest.fixture
def summary(client, monitored_entity, created_entities):
    summary = client.monitoring.summaries.create(
        _utils.generate_default_name(),
        data_types.NumericValue,
        monitored_entity,
    )

    yield summary

    # TODO: use `created_entities` if/when Summary reimplements delete()
    client.monitoring.summaries.delete([summary])


@pytest.fixture
def summary_sample(client, summary):
    end_time = time_utils.now()
    start_time = end_time - datetime.timedelta(hours=1)
    sample = summary.log_sample(
        data_types.NumericValue(3), {"foo": "bar"},
        start_time, end_time,
    )

    return sample
