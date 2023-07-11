# -*- coding: utf-8 -*-
import re

import pytest

from tests.registry.pydantic_models import InputClass, OutputClass

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestSetSchema:
    def test_set_schema(self, model_version):
        input_schema = InputClass.schema()
        output_schema = OutputClass.schema()
        model_version.set_schema(input=input_schema, output=output_schema)

        assert model_version.get_schema() == {
            "input": input_schema,
            "output": output_schema,
        }

    def test_set_schema_no_output(self, model_version):
        input_schema = InputClass.schema()
        model_version.set_schema(input=input_schema)

        assert model_version.get_schema() == {
            "input": input_schema,
        }

    def test_set_schema_no_input_which_is_bad(self, model_version):
        output_schema = OutputClass.schema()
        with pytest.raises(
            TypeError,
            match=re.escape(
                "RegisteredModelVersion.set_schema() missing 1 required positional argument: 'input'"
            ),
        ):
            model_version.set_schema(output=output_schema)
