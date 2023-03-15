# -*- coding: utf-8 -*-

import string

from hypothesis import (
    HealthCheck,
    given,
    settings,
    strategies as st,
)

import verta._internal_utils.model_dependencies as md
from verta.registry import VertaModelBase, verify_io


@st.composite
def custom_module_name(draw, max_depth: int = 8) -> str:
    """Generate dot-delimited Python module names."""
    depth = draw(st.integers(min_value=0, max_value=max_depth))

    # Python 2 rules, for simplicity
    module_name = st.from_regex("[a-zA-Z_][a-zA-Z0-9_]{0,31}", fullmatch=True)

    return ".".join(draw(module_name) for _ in range(depth + 1))


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(data=st.data())
def test_custom_modules(make_custom_module, data):
    module1, module2, module3 = [
        make_custom_module(data.draw(custom_module_name())) for _ in range(3)
    ]

    class Model(VertaModelBase):
        def __init__(self, artifacts=None):
            self._value = module1.get_value()

        @staticmethod
        def get_value():
            return module2.get_value()

        @verify_io
        def predict(self, input):
            return [
                self._value,
                self.get_value(),
                module3.get_value(),
                input,
            ]

    top_level_module_names = set(
        module.__name__.split(".")[0] for module in (module1, module2, module3)
    )
    assert md.class_module_names(Model) == top_level_module_names
