from datetime import timedelta

import hypothesis
import pytest
from jsonschema import ValidationError

from tests.strategies import generate_object, generate_another_object
from verta.registry import validate_schema, verify_io


class TestValidateSchema:
    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(
        matching_input_value=generate_object(),
        matching_output_value=generate_another_object(),
    )
    def test_validate_schema_allow(
        self,
        recwarn,
        make_model_schema_file,
        matching_input_value,
        matching_output_value,
    ):
        @validate_schema
        def predict(self, input):
            return matching_output_value.dict()

        predict(None, matching_input_value.dict())
        # verify there were no warnings
        assert len(recwarn) == 0

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

    def test_validate_schema_deny_not_json(self, make_model_schema_file):
        array = pytest.importorskip("numpy").array([1, 2, 3])

        @validate_schema
        @verify_io  # this is actually pointless because validate_schema will raise first
        def predict(self, input):
            return input

        with pytest.raises(TypeError, match="input must be a dict.*"):
            predict(None, array)

    def test_validate_schema_deny_verify_io_first(self, make_model_schema_file):
        array = pytest.importorskip("numpy").array([1, 2, 3])

        @verify_io
        @validate_schema
        def predict(self, input):
            return input

        # when verify_io is first, it will raise a TypeError before validate_schema is called
        with pytest.raises(TypeError, match="Object of type ndarray is not JSON serializable.*"):
            predict(None, array)

    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(matching_input_value=generate_object())
    def test_validate_schema_deny_output(
        self, recwarn, make_model_schema_file, matching_input_value
    ):
        @validate_schema
        def predict(self, input):
            return input  # note that this does not match the output schema

        predict(None, matching_input_value.dict())
        assert len(recwarn) == 1
        w = recwarn.pop(UserWarning)
        assert str(w.message).startswith("output failed schema validation")

    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(
        matching_input_value=generate_object(),
    )
    def test_validate_schema_no_output(
        self, make_model_schema_file_no_output, matching_input_value
    ):
        @validate_schema
        def predict(self, input):
            return input  # irrelevant

        predict(None, matching_input_value.dict())

    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(
        matching_input_value=generate_object(),
    )
    def test_validate_schema_deny_output_not_json(
        self, make_model_schema_file, matching_input_value
    ):
        @validate_schema
        def predict(self, input):
            array = pytest.importorskip("numpy").array([1, 2, 3])
            return array

        with pytest.raises(TypeError, match="output must be a dict.*"):
            predict(None, matching_input_value.dict())
