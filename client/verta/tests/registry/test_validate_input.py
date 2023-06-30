from datetime import timedelta

import hypothesis
from tests.strategies import json_strategy, input_class
from verta.registry import validate_input


class TestValidateInput:
    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(value=json_strategy, input_schema=input_class())
    def test_validate_input_allow(
        self, make_model_schema_file, value, input_schema
    ):

        @validate_input
        def predict(self, input):
            return input

        predict(None, value)
