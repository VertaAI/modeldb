from datetime import timedelta

import hypothesis
import pytest
from jsonschema import ValidationError

from tests.strategies import generate_object, generate_another_object
from verta.registry import validate_schema, verify_io


class TestValidateInput:
    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(matching_input_value=generate_object())
    def test_validate_schema_allow(self, make_model_schema_file, matching_input_value):
        @validate_schema
        def predict(self, input):
            return input

        predict(None, matching_input_value.dict())

    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(non_matching_input_value=generate_another_object())
    def test_validate_schema_deny(
        self, make_model_schema_file, non_matching_input_value
    ):
        @validate_schema
        def predict(self, input):
            return input

        with pytest.raises(ValidationError):
            predict(None, non_matching_input_value.dict())

    def test_validate_schema_deny_not_json(
        self, make_model_schema_file
    ):
        array = pytest.importorskip("numpy").array([1, 2, 3])

        @validate_schema
        @verify_io  # this is actually pointless because validate_schema will raise first
        def predict(self, input):
            return input

        with pytest.raises(ValidationError):
            predict(None, array)

    def test_validate_schema_deny_verify_io_first(
        self, make_model_schema_file
    ):
        array = pytest.importorskip("numpy").array([1, 2, 3])

        @verify_io
        @validate_schema
        def predict(self, input):
            return input

        # When verify_io is first, it will raise a TypeError before validate_schema is called
        with pytest.raises(TypeError):
            predict(None, array)
