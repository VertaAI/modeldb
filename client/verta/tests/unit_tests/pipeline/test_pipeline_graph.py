# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineGraph class
"""

from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import PipelineGraph


def test_set_steps(make_mock_pipeline_step) -> None:
    """
    Test that the steps of a PipelineGraph can be set
    """
    step_1 = make_mock_pipeline_step()
    step_2 = make_mock_pipeline_step()
    graph = PipelineGraph(steps=[])
    graph.set_steps([step_1, step_2])
    assert set(graph.steps) == set([step_1, step_2])
    graph.set_steps([])
    assert not graph.steps


@given(pipeline_definition=pipeline_definition())
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_from_definition(
    mocked_responses, pipeline_definition, mock_conn, mock_config
) -> None:
    """Test that a PipelineGraph object can be constructed from a pipeline
    specification.

    The model version is fetched for each step, so a response
    is mocked for each.  In depth testing of each step is handled in
    test_pipeline_step.test_steps_from_pipeline_spec.
    """
    for step in pipeline_definition["steps"]:
        mocked_responses.get(
            f"https://test_socket/api/v1/registry/model_versions/{step['model_version_id']}",
            json={"name": "test"},
            status=200,
        )
        mocked_responses.get(
            f"https://test_socket/api/v1/registry/registered_models/0",
            json={},
            status=200,
        )
    graph = PipelineGraph._from_definition(
        pipeline_definition=pipeline_definition, conn=mock_conn, conf=mock_config
    )
    assert isinstance(graph, PipelineGraph)
    assert len(graph.steps) == len(pipeline_definition["steps"])


def test_to_graph_definition(make_mock_pipeline_step) -> None:
    """Test that a pipeline graph specification can be constructed from a
    PipelineGraph object
    """
    step_1 = make_mock_pipeline_step("step_1")
    step_2 = make_mock_pipeline_step("step_2")
    step_3 = make_mock_pipeline_step("step_3")
    step_2.set_predecessors([step_1])
    step_3.set_predecessors([step_2])
    graph = PipelineGraph(steps=[step_1, step_2, step_3])
    graph_spec = graph._to_graph_definition()
    assert sorted(graph_spec, key=lambda x: x["name"]) == [
        {
            "name": step_1.name,
            "predecessors": [],
        },
        {
            "name": step_2.name,
            "predecessors": [step_1.name],
        },
        {
            "name": step_3.name,
            "predecessors": [step_2.name],
        },
    ]


def test_to_steps_definition(make_mock_pipeline_step) -> None:
    """Test that a pipeline steps specification can be constructed from a
    PipelineGraph object.
    """
    step_1 = make_mock_pipeline_step()
    step_2 = make_mock_pipeline_step()
    graph = PipelineGraph(steps=[step_1, step_2])
    step_specs = graph._to_steps_definition()
    assert step_specs == [
        {
            "name": step_1.name,
            "model_version_id": step_1.model_version.id,
        },
        {
            "name": step_2.name,
            "model_version_id": step_2.model_version.id,
        },
    ]
