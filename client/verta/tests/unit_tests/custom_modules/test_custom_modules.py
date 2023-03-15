# -*- coding: utf-8 -*-

import string

from hypothesis import (
    HealthCheck,
    given,
    settings,
    strategies as st,
)


@st.composite
def custom_module_import_path(draw, max_depth=8):
    depth = draw(st.integers(min_value=0, max_value=max_depth))
    module_name = st.text(
        alphabet=string.ascii_lowercase + "_",  # omit numbers for simplicity
        min_size=1,
        max_size=32,
    )
    return ".".join(draw(module_name) for _ in range(depth + 1))


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(data=st.data())
def test_custom_modules(custom_module_factory, data):
    module = custom_module_factory(data.draw(custom_module_import_path()))
    print(f"{module.__file__=}")
    print(f"{module.__name__=}")
