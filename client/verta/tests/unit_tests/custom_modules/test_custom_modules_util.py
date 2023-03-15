# -*- coding: utf-8 -*-

import importlib

from hypothesis import (
    HealthCheck,
    assume,
    given,
    settings,
    strategies as st,
)
import pytest

from verta._internal_utils.custom_modules import CustomModules
from verta.registry import VertaModelBase, verify_io


SLOW_FILTER_HEALTH_CHECKS = [
    HealthCheck.filter_too_much,
    HealthCheck.too_slow,
    HealthCheck.function_scoped_fixture,
]


@st.composite
def custom_module_name(draw, max_depth: int = 8) -> str:
    """Generate dot-delimited Python module names."""
    depth = draw(st.integers(min_value=0, max_value=max_depth))

    # Python 2 rules, for simplicity
    module_name = st.from_regex("[a-zA-Z_][a-zA-Z0-9_]{0,31}", fullmatch=True)

    return ".".join(draw(module_name) for _ in range(depth + 1))


class TestIsImportable:
    """Test ``CustomModules.is_importable()``."""

    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(data=st.data())
    def test_is_importable(self, make_custom_module, data):
        """Test that we can verify a valid module can be imported."""
        module = make_custom_module(data.draw(custom_module_name()))
        assert CustomModules.is_importable(module.__name__)

    @settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
    @given(data=st.data())
    def test_is_not_importable(self, make_custom_module, data):
        """Test that we can verify a nonexistent module cannot be imported."""
        nonexistent_module_name = data.draw(custom_module_name())
        assert not CustomModules.is_importable(nonexistent_module_name)


class TestGetModulePath:
    """Test ``CustomModules.get_module_path()``."""

    @settings(suppress_health_check=SLOW_FILTER_HEALTH_CHECKS)
    @given(data=st.data())
    def test_get_single_file_module_path(self, make_custom_module, data):
        """Test that we can find the path to a ``.py`` file module."""
        _module_name = data.draw(custom_module_name())
        assume("." not in _module_name)
        module = make_custom_module(_module_name)

        module_path = CustomModules().get_module_path(module.__name__)
        assert module_path == module.__file__

    @settings(suppress_health_check=SLOW_FILTER_HEALTH_CHECKS)
    @given(data=st.data())
    def test_get_directory_module_path(self, make_custom_module, data):
        """Test that we can find the path to a directory module."""
        _module_name = data.draw(custom_module_name())
        assume("." in _module_name)
        module = importlib.import_module(
            name="..",
            package=make_custom_module(_module_name).__name__,
        )

        module_path = CustomModules().get_module_path(module.__name__)
        assert module_path == list(module.__path__)[0]
