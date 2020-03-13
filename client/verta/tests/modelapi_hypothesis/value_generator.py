from string import printable

import numpy as np
import pandas as pd

import hypothesis.strategies as st
from api_generator import model_api, model_api_series, model_api_dataframe

# Gets a full api description and returns a generator for that api
def value_from_api(api):
    # Base types are simple, just use common strategies
    if api['type'] == 'VertaNull':
        return st.just(None)
    if api['type'] == 'VertaBool':
        return st.booleans()
    if api['type'] == 'VertaFloat':
        return st.floats() | st.integers() # We store integers and floats in the same type
    if api['type'] == 'VertaString':
        return st.text(printable)

    if api['type'] == 'VertaList':
        # If it's a list, get a generator for each element in the list and return as a tuple
        # We use a tuple to ensure it will have exactly the same length as the list in the api
        ret = st.tuples(*tuple([value_from_api(v) for v in api['value']]))
        # Then convert to a list with known size
        ret = ret.map(lambda t: list(t))
        return ret

    if api['type'] == 'VertaJson':
        # If it's a json/mapping, we'll have the same keys always but the values will keep changing
        # So collect all the keys based on the names
        keys = [v['name'] for v in api['value']]
        # Creates a generator for the subitems, like in the list case
        values = st.tuples(*tuple([value_from_api(v) for v in api['value']]))
        # Then create a dictionary out of the keys and the generated values
        ret = values.map(lambda vals: {k: v for k, v in zip(keys, vals)})
        return ret

    raise ValueError("unknown api format %s" % api)

def verta_type_to_dtype(name):
    map = {
        "VertaBool": np.bool,
        "VertaFloat": np.float,
        "VertaString": str,
    }
    return map[name]

def _sized_series_from_api(api, size):
    values = st.lists(value_from_api(api), size, size)
    return values.map(lambda vals: pd.Series(data=vals, name=api['name'], dtype=verta_type_to_dtype(api['type'])))

def series_from_api(api):
    size = st.integers(1, 100)
    return size.flatmap(lambda size: _sized_series_from_api(api, size))

def dataframe_from_api(api):
    size = st.integers(1, 100)
    multi_series = size.flatmap(lambda size: st.tuples(*[_sized_series_from_api(subapi, size) for subapi in api['value']]))
    dataframe = multi_series.map(lambda s: pd.concat(s, axis=1, keys=[v.name for v in s]))
    return dataframe

# Converts a generator to a model api to a generator of (model api, list of samples)
api_and_values = model_api.flatmap(lambda api: st.tuples(st.just(api), st.lists(value_from_api(api), 1, 100)))

series_api_and_values = model_api_series.flatmap(lambda api: st.tuples(st.just(api), series_from_api(api)))
dataframe_api_and_values = model_api_dataframe.flatmap(lambda api: st.tuples(st.just(api), dataframe_from_api(api)))
