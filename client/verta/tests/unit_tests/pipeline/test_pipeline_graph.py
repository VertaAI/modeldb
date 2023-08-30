# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineGraph class
"""

from unittest.mock import patch

import pytest
from hypothesis import given, HealthCheck, settings, strategies as st

import verta
from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import PipelineGraph, PipelineStep


def test_set_steps(make_mock_pipeline_step, make_mock_registered_model) -> None:
    """Test that the steps of a PipelineGraph can be set."""
    mocked_rm = make_mock_registered_model(id=123, name="test_rmv")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        step_1 = make_mock_pipeline_step()
        step_2 = make_mock_pipeline_step()
        graph = PipelineGraph(steps=set())
    graph.set_steps({step_1, step_2})
    assert set(graph.steps) == {step_1, step_2}
    graph.set_steps(set())
    assert not graph.steps


@given(
    pipeline_definition=pipeline_definition(),
    # max value limit avoids protobuf "Value out of range" error
    registered_model_id=st.integers(min_value=1, max_value=2**63),
    model_version_name=st.text(min_size=1),
    model_name=st.text(min_size=1),
)
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_from_definition(
    pipeline_definition,
    registered_model_id,
    model_version_name,
    model_name,
    mock_conn,
    mock_config,
    mocked_responses,
) -> None:
    """Test that a PipelineGraph object can be constructed from a pipeline
    definition.

    The model version is fetched for each step, so a response
    is mocked for each.  In depth testing of each step is handled in
    test_pipeline_step.test_steps_from_pipeline_definition.
    """
    for step in pipeline_definition["steps"]:
        mocked_responses.get(
            f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/registry/model_versions/"
            f"{step['model_version_id']}",
            json={
                "model_version": {
                    "id": step["model_version_id"],
                    "registered_model_id": registered_model_id,
                    "version": model_version_name,
                }
            },
            status=200,
        )
        mocked_responses.get(
            f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/registry/registered_models/"
            f"{registered_model_id}",
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
    graph_steps_sorted = sorted(graph.steps, key=lambda x: x.name)

    for graph_step, pipeline_step in zip(graph_steps_sorted, pipeline_steps_sorted):
        assert graph_step.name == pipeline_step["name"]
        assert (
            graph_step.registered_model_version.id == pipeline_step["model_version_id"]
        )
        assert graph_step._registered_model.name == model_name
        assert graph_step._registered_model.id == registered_model_id


def test_to_graph_definition(
    make_mock_pipeline_step, make_mock_registered_model
) -> None:
    """Test that a pipeline `graph` specification can be constructed from a
    PipelineGraph object.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rmv")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
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


def test_to_steps_definition(
    make_mock_pipeline_step, make_mock_registered_model
) -> None:
    """Test that a pipeline `steps` specification can be constructed from a
    PipelineGraph object.

    Definitions are type list to remain json serializable.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        step_1 = make_mock_pipeline_step(name="step_1")
        step_2 = make_mock_pipeline_step(name="step_2")
        graph = PipelineGraph(steps={step_1, step_2})
    step_specs = graph._to_steps_definition()
    expected_definition = [
        {
            "name": step_1.name,
            "model_version_id": step_1.registered_model_version.id,
        },
        {
            "name": step_2.name,
            "model_version_id": step_2.registered_model_version.id,
        },
    ]
    assert sorted(step_specs, key=lambda x: x["name"]) == sorted(
        expected_definition, key=lambda x: x["name"]
    )


def test_bad_mutation_of_step_predecessors_exception(
    make_mock_registered_model_version,
    make_mock_registered_model,
    make_mock_pipeline_step,
):
    """Test that we throw the correct exception when a user tries to mutate
    the predecessors of a step in an inappropriate way.
    """
    mocked_rmv = make_mock_registered_model_version()
    mocked_rm = make_mock_registered_model(
        id=mocked_rmv.registered_model_id, name="test_rm"
    )
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        step = PipelineStep(
            registered_model_version=mocked_rmv,
            name="test_name",
            predecessors=set(),
        )
    step.predecessors.add("not_a_step")
    with pytest.raises(TypeError) as err:
        PipelineGraph(steps={step})
    assert (
        str(err.value) == f"individual predecessors of a PipelineStep must be type"
        f" PipelineStep, not <class 'str'>."
    )


def test_step_name_uniqueness_exception(
    make_mock_registered_model, make_mock_pipeline_step
):
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        step_1 = make_mock_pipeline_step(name="step_1")
        step_2 = make_mock_pipeline_step(name="step_2")
        step_3 = make_mock_pipeline_step(name="step_1")

    with pytest.raises(ValueError) as err:
        PipelineGraph(steps={step_1, step_2, step_3})
    assert str(err.value) == "step names must be unique within a PipelineGraph"
