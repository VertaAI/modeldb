# -*- coding: utf-8 -*-

import pytest


@pytest.fixture
def dataset(client, created_entities):
    dataset = client.create_dataset()
    created_entities.append(dataset)

    return dataset
