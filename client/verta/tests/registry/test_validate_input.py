from datetime import timedelta

import hypothesis
from tests.strategies import json_strategy, input_class
from verta.registry import validate_input


class TestValidateInput:
    @hypothesis.settings(
        suppress_health_check=[hypothesis.HealthCheck.function_scoped_fixture],
        deadline=timedelta(milliseconds=50),
    )
    @hypothesis.given(input_value=input_class())
    def test_validate_input_allow(
        self, make_model_schema_file, input_value
    ):

        @validate_input
        def predict(self, input):
            return input

        print(input_value.dict())
        predict(None, input_value.dict())
