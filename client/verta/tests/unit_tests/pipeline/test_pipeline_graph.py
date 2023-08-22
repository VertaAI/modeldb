# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineGraph class
"""

from hypothesis import given, HealthCheck, settings
from hypothesis import strategies as st

from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import PipelineGraph


def test_set_steps(make_mock_pipeline_step) -> None:
    """
    Test that the steps of a PipelineGraph can be set
    """
    step_1 = make_mock_pipeline_step()
    step_2 = make_mock_pipeline_step()
    graph = PipelineGraph(steps=set())
    graph.set_steps({step_1, step_2})
    assert set(graph.steps) == {step_1, step_2}
    graph.set_steps(set())
    assert not graph.steps


@given(
    pipeline_definition=pipeline_definition(),
    registered_model_id=st.integers(min_value=1, max_value=2**63),
    # max value limit avoids protobuf "Value out of range" error
    model_version_name=st.text(min_size=1),
    model_name=st.text(min_size=1),
)
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_from_definition(
    mocked_responses,
    pipeline_definition,
    mock_conn,
    mock_config,
    registered_model_id,
    model_version_name,
    model_name,
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
            json={
                "model_version": {
                    "id": step['model_version_id'],
                    "registered_model_id": registered_model_id,
                    "version": model_version_name,
                }
            },
            status=200,
        )
        mocked_responses.get(
            f"https://test_socket/api/v1/registry/registered_models/{registered_model_id}",
            json={
                "registered_model": {
                    "id": registered_model_id,
                    "name": model_name,
                }
            },
            status=200,
        )
    graph = PipelineGraph._from_definition(
        pipeline_definition=pipeline_definition, conn=mock_conn, conf=mock_config
    )
    # the object produced is a PipelineGraph
    assert isinstance(graph, PipelineGraph)
    # we have the same number of steps as in the pipeline definition
    assert len(graph.steps) == len(pipeline_definition["steps"])

    # sort each group of steps for comparison
    pipeline_steps_sorted = sorted(
        pipeline_definition["steps"], key=lambda x: x["name"]
    )
    graph_steps_sorted = sorted(list(graph.steps), key=lambda x: x.name)

    for graph_step, pipeline_step in zip(graph_steps_sorted, pipeline_steps_sorted):
        assert graph_step.name == pipeline_step["name"]
        assert graph_step.model_version.id == pipeline_step["model_version_id"]
        assert graph_step._registered_model.name == model_name
        assert graph_step._registered_model.id == registered_model_id


def test_to_graph_definition(make_mock_pipeline_step) -> None:
    """Test that a pipeline graph specification can be constructed from a
    PipelineGraph object
    """
    step_1 = make_mock_pipeline_step("step_1")
    step_2 = make_mock_pipeline_step("step_2")
    step_3 = make_mock_pipeline_step("step_3")
    step_2.set_predecessors({step_1})
    step_3.set_predecessors({step_2})
    graph = PipelineGraph(steps={step_1, step_2, step_3})
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

    Definitions are type list to remain json serializable.
    """
    step_1 = make_mock_pipeline_step(name="step_1")
    step_2 = make_mock_pipeline_step(name="step_2")
    graph = PipelineGraph(steps={step_1, step_2})
    step_specs = graph._to_steps_definition()
    expected_definition = [
        {
            "name": step_1.name,
            "model_version_id": step_1.model_version.id,
        },
        {
            "name": step_2.name,
            "model_version_id": step_2.model_version.id,
        },
    ]
    assert sorted(step_specs, key=lambda x: x["name"]) == sorted(
        expected_definition, key=lambda x: x["name"]
    )
