# -*- coding: utf-8 -*-

import math

import hypothesis
import hypothesis.strategies as st

from verta._pipeline_orchestrator._step_handler import ModelObjectStepHandler

from tests.models.standard_models import VertaModelDecorated
from tests.strategies import json_strategy


class TestModelObjectStepHandler:
    @hypothesis.given(
        name=st.text(),
        predecessors=st.lists(st.text(), unique=True),
        input=json_strategy,
    )
    def test_create_and_run(self, name, predecessors, input):
        """Verify attributes and run() return expected values."""
        hypothesis.assume(not (isinstance(input, float) and math.isnan(input)))

        model = VertaModelDecorated(artifacts=None)

        step_handler = ModelObjectStepHandler(name, predecessors, model)
        assert step_handler.name == name
        assert step_handler.predecessors == set(predecessors)

        assert step_handler.run(input) == model.predict(input)
