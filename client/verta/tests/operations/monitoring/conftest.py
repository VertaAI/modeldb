# -*- coding: utf-8 -*-

import datetime

import pytest

from verta._internal_utils import (
    _utils,
    time_utils,
)
from verta import data_types


@pytest.fixture
def monitored_entity(client, created_entities):
    name = _utils.generate_default_name()
    monitored_entity = client.operations.get_or_create_monitored_entity(name)
    created_entities.append(monitored_entity)

    return monitored_entity

@pytest.fixture
def summary_sample(client, monitored_entity, created_entities):
    summary = client.operations.summaries.create(
        _utils.generate_default_name(),
        data_types.NumericValue._type_string(),
        monitored_entity,
    )
    end_time = time_utils.now()
    start_time = end_time - datetime.timedelta(hours=1)
    sample = summary.log_sample(
        data_types.NumericValue(3), {"foo": "bar"},
        start_time, end_time,
    )

    yield sample

    # TODO: use `created_entities` if/when Summary reimplements delete()
    client.operations.summaries.delete([summary])
