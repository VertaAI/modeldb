# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineStep class
"""

import random

from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import PipelineStep


@given(pipeline_definition=pipeline_definition())
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_steps_from_pipeline_definition(
    pipeline_definition,
    mock_conn,
    mock_config,
    mocked_responses,
) -> None:
    """Test that a list of PipelineStep objects can be constructed and returned from
    a pipeline definition.

    The registered model, model version, and environment
    is fetched for each step, so a response is mocked for each call.
    """
    graph = pipeline_definition["graph"]
    for step in pipeline_definition["steps"]:
        mocked_responses.get(
            f"https://test_socket/api/v1/registry/model_versions/{step['model_version_id']}",
            json={"model_version": step["model_version_id"]},
            status=200,
        )
        mocked_responses.get(
            f"https://test_socket/api/v1/registry/registered_models/0",
            json={},
            status=200,
        )
    generated_steps = PipelineStep._steps_from_pipeline_definition(
        pipeline_definition=pipeline_definition,
        conn=mock_conn,
        conf=mock_config,
    )
    # we have the same number of steps as in the pipeline definition
    assert len(generated_steps) == len(pipeline_definition["steps"])
    for spec_step, gen_step in zip(pipeline_definition["steps"], generated_steps):
        # each step is converted to a PipelineStep object
        assert isinstance(gen_step, PipelineStep)
        # the names are the same for the steps and their definitions
        assert gen_step.name == spec_step["name"]
        # predecessors for each step are also converted to PipelineStep objects
        for i in gen_step.predecessors:
            assert isinstance(i, PipelineStep)
        # the predecessors for each step are the same as in the definition
        assert set([i.name for i in gen_step.predecessors]) == set(
            [s["predecessors"] for s in graph if gen_step.name == s["name"]][0]
        )


def test_to_step_spec(make_mock_registered_model_version) -> None:
    """Test that a PipelineStep object can be converted to a step specification"""
    model_version = make_mock_registered_model_version()
    step = PipelineStep(
        model_version=model_version,
        name="test_name",
        predecessors=[],  # predecessors not included in step spec
    )
    assert step._to_step_spec() == {
        "name": "test_name",
        "model_version_id": model_version.id,
    }


def test_to_graph_spec(
    make_mock_registered_model_version, make_mock_pipeline_step
) -> None:
    """Test that a PipelineStep object can be converted to a step specification"""
    predecessors = [make_mock_pipeline_step() for _ in range(random.randint(1, 5))]
    step = PipelineStep(
        model_version=make_mock_registered_model_version(),
        name="test_name",
        predecessors=predecessors,
    )
    assert step._to_graph_spec() == {
        "name": "test_name",
        "predecessors": [s.name for s in predecessors],
    }


def test_set_predecessors_add(
    make_mock_registered_model_version, make_mock_pipeline_step
) -> None:
    """Test that predecessors can be added to a PipelineStep object"""
    predecessor_1 = make_mock_pipeline_step()
    predecessor_2 = make_mock_pipeline_step()
    step = PipelineStep(
        model_version=make_mock_registered_model_version(),
        name="test_name",
        predecessors=[predecessor_1],
    )
    step.set_predecessors(step.predecessors + [predecessor_2])
    assert set(step.predecessors) == {predecessor_1, predecessor_2}


def test_set_predecessors_remove(
    make_mock_registered_model_version, make_mock_pipeline_step
) -> None:
    """Test that predecessors can be removed from a PipelineStep object"""
    predecessors = [make_mock_pipeline_step() for _ in range(random.randint(2, 10))]
    steps_to_remain = predecessors[: len(predecessors) // 2]
    step = PipelineStep(
        model_version=make_mock_registered_model_version(),
        name="test_name",
        predecessors=predecessors,
    )
    step.set_predecessors(steps_to_remain)
    assert set(step.predecessors) == set(steps_to_remain)


def test_change_model_version(make_mock_registered_model_version) -> None:
    """Test that a PipelineStep object can have its model version changed"""
    model_ver_1 = make_mock_registered_model_version()
    model_ver_2 = make_mock_registered_model_version()
    step = PipelineStep(
        model_version=model_ver_1,
        name="test_name",
        predecessors=[],
    )
    assert step.model_version == model_ver_1
    step.set_model_version(model_ver_2)
    assert step.model_version == model_ver_2
