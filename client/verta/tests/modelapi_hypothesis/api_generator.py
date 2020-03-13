import hypothesis
import hypothesis.strategies as st
from string import printable

# We have 4 base types that can be leaves
# Assume that the null type doesn't make much sense, since it's a constant
modelapi_base = hypothesis.strategies.sampled_from(["VertaBool", "VertaFloat", "VertaString"])
# Each base type is in a dictionary with name and the given type
modelapi_base = modelapi_base.map(lambda b: {'type': b, 'name': ''})

# Updates the dictionary with the given key and value and returns the result dictionary
def dict_replace(d, key, value):
    d.update({key: value})
    return d

# Takes a generator for a subtree of the API and returns either a list of mapping composition of it
def recursive(children):
    # Create a list of subtrees
    lst = st.lists(children, 1, 10)
    # Then iterate over them replacing the given name (which is empty) with the index
    lst = lst.map(lambda v: [dict_replace(el, 'name', str(i)) for i, el in enumerate(v)])
    # Then convert the newly created to the dictionary format
    lst = lst.map(lambda v: {'type': 'VertaList', 'name': '', 'value': v})

    # Create printable text for the keys
    name = st.text(printable)
    # Create a list of (name, subtree) where the names are unique
    dikt = st.lists(st.tuples(name, children), 1, 10, unique_by=lambda v: v[0])
    # Then iterate over them replacing the given name (which is empty) with the given name
    dikt = dikt.map(lambda lst: sorted([dict_replace(el, 'name', n) for n, el in lst], key=lambda value: value['name']))
    # Then convert the newly created to the dictionary format
    dikt = dikt.map(lambda d: {'type': 'VertaJson', 'name': '', 'value': d})

    # Return either the list or the dictionary
    return lst | dikt

# Create a model api by doing this recursively
model_api = st.recursive(modelapi_base, recursive)

# pandas-specific types
name = st.text(printable)
model_api_series = st.tuples(name, modelapi_base).map(lambda arg: dict_replace(arg[1], 'name', arg[0]))
model_api_dataframe = st.lists(model_api_series, 1, unique_by=lambda x: x['name']).map(lambda v: {'type': 'VertaList', 'name': '', 'value': v})
