# -*- coding: utf-8 -*-

# TODO: this functionality should be tucked into a method on Pipeline, so this test
# logic should be moved to use that instead of LocalOrchestrator directly.

import functools

from verta import runtime
from verta._pipeline_orchestrator import LocalOrchestrator
from verta.environment import Python
from verta.registry import VertaModelBase, verify_io


class ModelBase(VertaModelBase):
    def __init__(self, artifacts=None):
        pass


class Echo(ModelBase):
    @verify_io
    def predict(self, input):
        val = input["input"]
        runtime.log("echo", f"echoing {val}")
        return val


class Double(ModelBase):
    @verify_io
    def predict(self, input):
        val = input["echo"]
        runtime.log("double", f"doubling {val}")
        return val * 2


class Triple(ModelBase):
    @verify_io
    def predict(self, input):
        val = input["echo"]
        runtime.log("triple", f"tripling {val}")
        return val * 3


class Sum(ModelBase):
    @verify_io
    def predict(self, input):
        val1 = input["double"]
        val2 = input["triple"]
        runtime.log("sum", f"summing {val1} and {val2}")
        return val1 + val2


class TestLocalOrchestrator:
    def test_run_and_logs(self, registered_model):
        r"""Verify a simple DAG pipeline runs and captures logs as expected.

        The pipeline takes a numerical input, and processes it as follows:

                 echo
                /    \
            double  triple
                \    /
                 sum

        """
        create_standard_model = functools.partial(
            registered_model.create_standard_model,
            code_dependencies=[],
            environment=Python([]),
        )
        echo_model_ver = create_standard_model(Echo)
        double_model_ver = create_standard_model(Double)
        triple_model_ver = create_standard_model(Triple)
        sum_model_ver = create_standard_model(Sum)

        pipeline_defn = {
            "steps": [
                {"name": "echo", "model_version_id": echo_model_ver.id},
                {"name": "double", "model_version_id": double_model_ver.id},
                {"name": "triple", "model_version_id": triple_model_ver.id},
                {"name": "sum", "model_version_id": sum_model_ver.id},
            ],
            "graph": [
                {"name": "double", "predecessors": ["echo"]},
                {"name": "triple", "predecessors": ["echo"]},
                {"name": "sum", "predecessors": ["double", "triple"]},
            ],
        }
        orchestrator = LocalOrchestrator(registered_model._conn, pipeline_defn)

        input = 3
        with runtime.context() as ctx:
            output = orchestrator.run(3)
        assert ctx.logs() == {
            "echo": f"echoing {input}",
            "double": f"doubling {input}",
            "triple": f"tripling {input}",
            "sum": f"summing {input * 2} and {input * 3}",
        }
        assert output == input * 2 + input * 3
        assert orchestrator._outputs == {
            "echo": input,
            "double": input * 2,
            "triple": input * 3,
            "sum": output,
        }
