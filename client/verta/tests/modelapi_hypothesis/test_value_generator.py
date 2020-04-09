import pytest

import six

import numbers

pytest.importorskip("numpy")
pytest.importorskip("pandas")

import hypothesis
from value_generator import api_and_values, series_api_and_values, dataframe_api_and_values


# Check if the given value fits the defined api
def fit_api(api, value):
    if api['type'] == 'VertaNull':
        return value is None
    if api['type'] == 'VertaBool':
        return isinstance(value, bool)
    if api['type'] == 'VertaFloat':
        return isinstance(value, numbers.Real)
    if api['type'] == 'VertaString':
        return isinstance(value, six.string_types)

    if api['type'] == 'VertaList':
        if not isinstance(value, list):
            return False
        for subapi, subvalue in zip(api['value'], value):
            if not fit_api(subapi, subvalue):
                return False

    if api['type'] == 'VertaJson':
        keys = sorted([v['name'] for v in api['value']])
        actual_keys = sorted(list(value.keys()))
        if keys != actual_keys:
            return False

        subapi_dict = {v['name']: v for v in api['value']}

        for k in keys:
            if not fit_api(subapi_dict[k], value[k]):
                return False

    return True


# Verify that the value generation system actually creates something that fits the api
@hypothesis.given(api_and_values)
def test_value_from_api(api_and_values):
    api, values = api_and_values
    for v in values:
        assert fit_api(api, v)


@hypothesis.given(series_api_and_values)
def test_series_from_api(api_and_values):
    api, values = api_and_values
    assert api['name'] == values.name
    for v in values.to_list():
        assert fit_api(api, v)


@hypothesis.given(dataframe_api_and_values)
def test_dataframe_from_api(api_and_values):
    api, values = api_and_values
    assert api['name'] == ''
    assert api['type'] == 'VertaList'
    for subapi, c in zip(api['value'], values.columns):
        subvalues = values[c]

        assert subapi['name'] == subvalues.name
        for v in subvalues.to_list():
            assert fit_api(subapi, v)
