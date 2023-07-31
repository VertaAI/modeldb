from datetime import timedelta

import hypothesis
import hypothesis.strategies as st
import pytest
from jsonschema import ValidationError

from verta.registry import validate_schema, verify_io

from .pydantic_models import AnInnerClass, InputClass, OutputClass


@st.composite
def generate_inner_object(draw):
    h_dict = draw(st.dictionaries(st.text(), st.integers()))
    i_list_str = draw(st.lists(st.text()))
    return AnInnerClass(
        h_dict=h_dict,
        i_list_str=i_list_str,
    )


@st.composite
def generate_object(draw):
    a_int = draw(st.integers())
    b_str = draw(st.text())
    c_float = draw(st.floats())
    d_bool = draw(st.booleans())
    e_list_int = draw(st.lists(st.integers()))
    f_dict = draw(st.dictionaries(st.text(), st.text()))
    g_inner_input_class = draw(generate_inner_object())

    return InputClass(
        a_int=a_int,
        b_str=b_str,
        c_float=c_float,
        d_bool=d_bool,
        e_list_int=e_list_int,
        f_dict=f_dict,
        g_inner=g_inner_input_class,
    )


@st.composite
def generate_another_object(draw):
    j_bool = draw(st.booleans())
    k_list_list_int = draw(st.lists(st.lists(st.integers())))
    l_str = draw(st.text())

    return OutputClass(
        j_bool=j_bool,
        k_list_list_int=k_list_list_int,
        l_str=l_str,
    )


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
        make_model_schema_file,
        matching_input_value,
        matching_output_value,
    ):
        @validate_schema
        def predict(self, input):
            return matching_output_value.dict()

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
        with pytest.raises(
            TypeError, match="Object of type ndarray is not JSON serializable.*"
        ):
            predict(None, array)

    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(matching_input_value=generate_object())
    def test_validate_schema_deny_output(
        self, make_model_schema_file, matching_input_value
    ):
        @validate_schema
        def predict(self, input):
            return input  # note that this does not match the output schema

        with pytest.raises(ValidationError, match="output failed schema validation.*"):
            predict(None, matching_input_value.dict())

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
