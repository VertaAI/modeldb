import pytest

import json

pytest.importorskip("numpy")
pytest.importorskip("pandas")

from verta.utils import ModelAPI

import hypothesis
from value_generator import api_and_values, series_api_and_values, dataframe_api_and_values


# Verify that, given a sample created from an api, the same api can be inferred
@hypothesis.given(api_and_values)
def test_modelapi_and_values(api_and_values):
    api, values = api_and_values
    assert len(values) > 0
    predicted_api = ModelAPI._data_to_api(values)

    assert json.dumps(api, sort_keys=True, indent=2) == json.dumps(predicted_api, sort_keys=True, indent=2)


@hypothesis.given(series_api_and_values)
def test_series_modelapi_and_values(series_api_and_values):
    api, values = series_api_and_values
    predicted_api = ModelAPI._data_to_api(values)

    assert json.dumps(api, sort_keys=True, indent=2) == json.dumps(predicted_api, sort_keys=True, indent=2)


@hypothesis.given(dataframe_api_and_values)
def test_dataframe_modelapi_and_values(dataframe_api_and_values):
    api, values = dataframe_api_and_values
    predicted_api = ModelAPI._data_to_api(values)

    assert json.dumps(api, sort_keys=True, indent=2) == json.dumps(predicted_api, sort_keys=True, indent=2)
