# -*- coding: utf-8 -*-
"""
Unit tests for the PipelineStep class
"""

import hypothesis.strategies as st
from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import mock_pipeline_spec
from verta.pipeline import PipelineStep


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(pipeline_spec=mock_pipeline_spec())
def test_from_pipeline_spec(pipeline_spec, mock_conn, mocked_responses) -> None:
    """Test that a PipelineStep object can be constructed from a pipeline specification"""
    step = pipeline_spec["steps"][0]
    inputs = [i["inputs"] for i in pipeline_spec["graph"] if i["name"] == step["name"]][
        0
    ]
    mocked_responses.get(
        f"https://test_socket/api/v1/registry/model_versions/{step['model_version_id']}",
        json={},
        status=200,
    )
    generated_step = PipelineStep._from_pipeline_spec(
        step_name=step["name"],
        pipeline_spec=pipeline_spec,
        conn=mock_conn,
    )
    assert generated_step.name == step["name"]
    assert generated_step.input_steps == inputs
    assert generated_step.attributes == {
        a["key"]: a["value"] for a in step["attributes"]
    }


@given(
    name=st.text(min_size=1),
    input_steps=st.lists(st.text(min_size=1), min_size=1),
    attributes=st.dictionaries(st.text(min_size=1), st.text(min_size=1), min_size=1),
)
def test_to_steps_spec(
    mock_registered_model_version, name, attributes, input_steps
) -> None:
    """Test that a PipelineStep object can be converted to a step specification"""
    step = PipelineStep(
        model_version=mock_registered_model_version,
        name=name,
        input_steps=input_steps,
        attributes=attributes,
    )
    assert step._to_steps_spec() == {
        "name": name,
        "model_version_id": mock_registered_model_version.id,
        "attributes": [{"key": k, "value": v} for k, v in attributes.items()],
    }


@given(
    name=st.text(min_size=1),
    input_steps=st.lists(st.text(min_size=1), min_size=1),
    attributes=st.dictionaries(st.text(min_size=1), st.text(min_size=1), min_size=1),
)
def test_to_graph_spec(
    mock_registered_model_version, name, attributes, input_steps
) -> None:
    """Test that a PipelineStep object can be converted to a step specification"""
    step = PipelineStep(
        model_version=mock_registered_model_version,
        name=name,
        input_steps=input_steps,
        attributes=attributes,
    )
    assert step._to_graph_spec() == {
        "name": name,
        "inputs": input_steps,
    }


@given(
    input_steps=st.lists(st.text(min_size=1), min_size=2, unique=True),
)
def test_add_input_steps(mock_registered_model_version, input_steps) -> None:
    """Test that input steps can be added to a PipelineStep object"""
    initial_steps = input_steps[: len(input_steps) // 2]
    new_steps = input_steps[len(input_steps) // 2 :]
    step = PipelineStep(
        model_version=mock_registered_model_version,
        name="test",
        input_steps=initial_steps,
        attributes={},
    )
    step.add_input_steps(new_steps)
    assert step.input_steps == input_steps


@given(
    input_steps=st.lists(st.text(min_size=1), min_size=2, unique=True),
)
def test_remove_input_steps(mock_registered_model_version, input_steps) -> None:
    """Test that input steps can be removed from a PipelineStep object"""
    steps_to_remain = input_steps[: len(input_steps) // 2]
    steps_to_remove = input_steps[len(input_steps) // 2 :]
    step = PipelineStep(
        model_version=mock_registered_model_version,
        name="test",
        input_steps=input_steps,
        attributes={},
    )
    step.remove_input_steps(steps_to_remove)
    assert step.input_steps == steps_to_remain


def test_change_model_version(make_mock_registered_model_version) -> None:
    """Test that a PipelineStep object can have its model version changed"""
    model_ver_1 = make_mock_registered_model_version()
    model_ver_2 = make_mock_registered_model_version()

    step = PipelineStep(
        model_version=model_ver_1,
        name="test",
        input_steps=[],
        attributes={},
    )
    assert step.model_version.id == model_ver_1.id
    step.change_model_version(model_ver_2)
    assert step.model_version.id == model_ver_2.id
