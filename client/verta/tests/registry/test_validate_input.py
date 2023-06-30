from datetime import timedelta

import hypothesis
import pytest
from hypothesis import given
from pydantic import BaseModel
import hypothesis.strategies as st

from tests import strategies
from tests.strategies import json_strategy
from verta.registry import validate_input


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
