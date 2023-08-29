# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineStep class
"""

import random
from unittest.mock import patch

from hypothesis import given, HealthCheck, settings, strategies as st

import verta
from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import PipelineStep


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
def test_steps_from_pipeline_definition(
    pipeline_definition,
    registered_model_id,
    model_version_name,
    model_name,
    mock_conn,
    mock_config,
    mocked_responses,
) -> None:
    """Test that a list of PipelineStep objects can be constructed and
    returned from a pipeline definition.

    The registered model, and registered model version is fetched for
    each step, so a call is mocked for each.
    """
    graph = pipeline_definition["graph"]
    for step in pipeline_definition["steps"]:
        mocked_responses.get(
            f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/registry/model_versions/{step['model_version_id']}",
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
            f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/registry/registered_models/{registered_model_id}",
            json={
                "registered_model": {
                    "id": registered_model_id,
                    "name": model_name,
                }
            },
            status=200,
        )
    generated_steps = PipelineStep._steps_from_pipeline_definition(
        pipeline_definition=pipeline_definition,
        conn=mock_conn,
        conf=mock_config,
    )
    # we have the same number of steps as in the pipeline definition
    assert len(generated_steps) == len(pipeline_definition["steps"])
    # sort both group of steps for side-by-side comparison
    generated_steps_sorted = sorted(generated_steps, key=lambda x: x.name)
    definition_steps_sorted = sorted(
        pipeline_definition["steps"], key=lambda x: x["name"]
    )

    for def_step, gen_step in zip(definition_steps_sorted, generated_steps_sorted):
        # the names are the same for the steps and their definitions
        assert gen_step.name == def_step["name"]
        # model version ids are the same for the steps and their definitions
        assert gen_step.registered_model_version.id == def_step["model_version_id"]
        # the registered model id for each step was fetched and added from the mocked response.
        assert gen_step._registered_model.id == registered_model_id
        # registered model names are fetched and added from the mocked response.
        assert gen_step._registered_model.name == model_name
        # each step is converted to a PipelineStep object
        assert isinstance(gen_step, PipelineStep)
        # predecessors for each step are also converted to PipelineStep objects
        for predecessor in gen_step.predecessors:
            assert isinstance(predecessor, PipelineStep)
        # the predecessors for each step are all included and have the same name as in the definition
        assert [s.name for s in gen_step.predecessors] == [
            s["predecessors"] for s in graph if gen_step.name == s["name"]
        ][0]


def test_to_step_spec(
    make_mock_registered_model_version, make_mock_registered_model
) -> None:
    """Test that a PipelineStep object can be converted to a step specification."""
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
            predecessors=set(),  # predecessors not included in step spec
        )
    assert step._to_step_spec() == {
        "name": "test_name",
        "model_version_id": mocked_rmv.id,
    }


def test_to_graph_spec(
    make_mock_registered_model_version,
    make_mock_pipeline_step,
    make_mock_registered_model,
) -> None:
    """Test that a PipelineStep object can be converted to a graph specification."""
    mocked_rmv = make_mock_registered_model_version()
    mocked_rm = make_mock_registered_model(
        id=mocked_rmv.registered_model_id, name="test_rmv"
    )
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        predecessors = {make_mock_pipeline_step() for _ in range(random.randint(1, 5))}
        step = PipelineStep(
            registered_model_version=mocked_rmv,
            name="test_name",
            predecessors=predecessors,
        )
    assert step._to_graph_spec() == {
        "name": "test_name",
        "predecessors": [s.name for s in predecessors],
    }


def test_set_predecessors_add(
    make_mock_registered_model_version,
    make_mock_pipeline_step,
    make_mock_registered_model,
) -> None:
    """Test that predecessors can be added to a PipelineStep object."""
    mocked_rmv = make_mock_registered_model_version()
    mocked_rm = make_mock_registered_model(
        id=mocked_rmv.registered_model_id, name="test_rm"
    )
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        predecessor_1 = make_mock_pipeline_step()
        predecessor_2 = make_mock_pipeline_step()
        step = PipelineStep(
            registered_model_version=mocked_rmv,
            name="test_name",
            predecessors={predecessor_1},
        )
    new_steps = step.predecessors.copy()
    new_steps.add(predecessor_2)
    step.set_predecessors(new_steps)
    assert set(step.predecessors) == {predecessor_1, predecessor_2}


def test_set_predecessors_remove(
    make_mock_registered_model_version,
    make_mock_pipeline_step,
    make_mock_registered_model,
) -> None:
    """Test that predecessors can be removed from a PipelineStep object."""
    mocked_rmv = make_mock_registered_model_version()
    mocked_rm = make_mock_registered_model(
        id=mocked_rmv.registered_model_id, name="test_rmv"
    )
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        predecessors = {make_mock_pipeline_step() for _ in range(random.randint(2, 10))}
        predecessors_as_list = list(predecessors)  # convert to list for slicing
        steps_to_remain = predecessors_as_list[: len(predecessors_as_list) // 2]
        step = PipelineStep(
            registered_model_version=mocked_rmv,
            name="test_name",
            predecessors=predecessors,
        )
    step.set_predecessors(set(steps_to_remain))
    assert step.predecessors == set(steps_to_remain)


@given(
    rm_1_name=st.text(min_size=1),
    rm_2_name=st.text(min_size=1),
)
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_change_model_version(
    rm_1_name,
    rm_2_name,
    make_mock_registered_model_version,
    mocked_responses,
) -> None:
    """Test that a PipelineStep object can have its model version changed.

    Each time a RMV is set for a PipelineStep, the RM for it is fetched,
    so a call is mocked for the initial step creation and the change.
    """
    rmv_1 = make_mock_registered_model_version()
    rmv_2 = make_mock_registered_model_version()
    mocked_responses.get(
        f"{rmv_1._conn.scheme}://{rmv_1._conn.socket}/api/v1/registry/registered_models/{rmv_1.registered_model_id}",
        json={
            "registered_model": {
                "id": rmv_1.registered_model_id,
                "name": rm_1_name,
            }
        },
        status=200,
    )
    mocked_responses.get(
        f"{rmv_2._conn.scheme}://{rmv_2._conn.socket}/api/v1/registry/registered_models/{rmv_2.registered_model_id}",
        json={
            "registered_model": {
                "id": rmv_2.registered_model_id,
                "name": rm_2_name,
            }
        },
        status=200,
    )
    step = PipelineStep(
        registered_model_version=rmv_1,
        name="test_name",
        predecessors=set(),
    )
    assert step.registered_model_version == rmv_1
    assert step._registered_model.id == rmv_1.registered_model_id
    step.set_registered_model_version(rmv_2)
    assert step.registered_model_version == rmv_2
    assert step._registered_model.id == rmv_2.registered_model_id
