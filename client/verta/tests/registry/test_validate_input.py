from datetime import timedelta

import hypothesis
import pytest
from hypothesis import given
from pydantic import BaseModel
import hypothesis.strategies as st

from tests import strategies
from tests.strategies import json_strategy
from verta.registry import validate_input


class InnerInputClass(BaseModel):
    h_dict: dict[str, str]
    i_list: list[str]

    def __init__(self, h, i):
        super().__init__(
            h_dict=h,
            i_list=i,
        )


class InputClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list: list[int]
    f_dict: dict[str, int]
    g_inner: InnerInputClass

    def __init__(self, a, b, c, d, e, f, g):
        super().__init__(
            a_int=a,
            b_str=b,
            c_float=c,
            d_bool=d,
            e_list=e,
            f_dict=f,
            g_inner=g,
        )


class TestValidateInput:
    @hypothesis.settings(deadline=timedelta(milliseconds=50))
    @hypothesis.given(value=json_strategy, input_class=strategies.input_class())
    def test_validate_input_allow(self, tmp_path, value, input_class):
        print(f"a_int: {input_class.a_int}")

        # Create tmp model_schema file
        model_schema = tmp_path / "model_schema.json"
        @validate_input
        def predict(self, input):
            return input

        predict(None, value)

    def test_verify_io_reject(self):
        array = pytest.importorskip("numpy").array([1, 2, 3])
        df = pytest.importorskip("pandas").DataFrame([1, 2, 3])
        tensor = pytest.importorskip("torch").tensor([1, 2, 3])

        msg_match = "not JSON serializable.*{} must only contain types"
        for value in [array, df, tensor]:

            @validate_input
            def predict(self, _):
                return value

            with pytest.raises(TypeError, match=msg_match.format("input")):
                predict(None, value)
            with pytest.raises(TypeError, match=msg_match.format("output")):
                predict(None, None)

    def test_verify_io_reject_bytes(self):
        # TODO: create Hypothesis strategy for non-decodable binary
        value = b"\x80abc"

        msg_match = "not JSON serializable.*{} must only contain types"

        @validate_input
        def predict(self, _):
            return value

        with pytest.raises(TypeError, match=msg_match.format("input")):
            predict(None, value)
        with pytest.raises(TypeError, match=msg_match.format("output")):
            predict(None, None)