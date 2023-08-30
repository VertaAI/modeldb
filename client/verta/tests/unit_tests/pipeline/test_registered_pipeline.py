# -*- coding: utf-8 -*-
"""
Unit tests for the RegisteredPipeline class
"""

from unittest.mock import patch

import pytest
from hypothesis import given, HealthCheck, settings, strategies as st

import verta
from tests.unit_tests.strategies import resources
from verta.pipeline import RegisteredPipeline


def test_copy_graph(
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that the graph of a RegisteredPipeline can be copied.

    Each step in the copied graph should be a new object, but have the same
    name, predecessors, and model version as the original.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )
    copied_graph = pipeline.copy_graph()
    # convert from sets to lists and sort for side-by-side comparison
    graph_steps_sorted = sorted(graph.steps, key=lambda x: x.name)
    copied_graph_steps_sorted = sorted(copied_graph.steps, key=lambda x: x.name)

    for orig_step, copied_step in zip(graph_steps_sorted, copied_graph_steps_sorted):
        assert orig_step is not copied_step
        assert orig_step.name == copied_step.name
        assert orig_step.predecessors == copied_step.predecessors
        assert (
            orig_step.registered_model_version.id
            == copied_step.registered_model_version.id
        )
    assert copied_graph is not graph


@given(model_version_name=st.text(min_size=1))
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_log_pipeline_definition_artifact(
    model_version_name,
    mocked_responses,
    make_mock_pipeline_graph,
    make_mock_registered_model,
    make_mock_registered_model_version,
) -> None:
    """Verify the expected sequence of calls when a pipeline definition
    is logged as an artifact to the pipeline's model version.

    Fetching the registered model version is patched instead of mocking a
    response to avoid having to pass the RM's id down through multiple
    pytest fixtures.
    """
    rm = make_mock_registered_model(id=123, name="test_rm")
    rmv = make_mock_registered_model_version()
    # Fetch the registered model version
    mocked_responses.get(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/{rmv.id}",
        json={
            "model_version": {
                "id": rmv.id,
                "registered_model_id": rmv.registered_model_id,
                "version": model_version_name,
            }
        },
        status=200,
    )
    mocked_responses.put(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/registered_models/{rmv.registered_model_id}/model_versions/{rmv.id}",
        json={},
        status=200,
    )
    # Fetch the artifact upload URL
    mocked_responses.post(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/{rmv.id}/getUrlForArtifact",
        json={
            "url": f"https://account.s3.amazonaws.com/development/ModelVersionEntity/"
            f"{rmv.id}/pipeline.json"
        },
        status=200,
    )
    # Upload the artifact
    mocked_responses.put(
        f"https://account.s3.amazonaws.com/development/ModelVersionEntity/{rmv.id}/pipeline.json",
        json={},
        status=200,
    )
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=rm
    ):
        pipeline = RegisteredPipeline(
            graph=make_mock_pipeline_graph(),
            registered_model_version=rmv,
        )
    pipeline._log_pipeline_definition_artifact()


def test_get_pipeline_definition_artifact(
    make_mock_registered_model_version,
    make_mock_simple_pipeline_definition,
) -> None:
    """Test that a pipeline definition artifact can be fetched from the
    registered model version associated with a RegisteredPipeline object.
    """
    rmv = make_mock_registered_model_version()
    pipeline_definition = RegisteredPipeline._get_pipeline_definition_artifact(
        registered_model_version=rmv,
    )
    assert pipeline_definition == make_mock_simple_pipeline_definition(id=rmv.id)


def test_to_pipeline_definition(
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that a pipeline definition can be constructed from a
    RegisteredPipeline object.

    In depth testing of the `_to_graph_definition`
    and `_to_steps_definition` functions are handled in unit tests for
    PipelineGraph.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )
    pipeline_definition = pipeline._to_pipeline_definition()
    assert pipeline_definition == {
        "pipeline_version_id": pipeline.id,
        "graph": graph._to_graph_definition(),
        "steps": graph._to_steps_definition(),
    }


@given(resources=resources())
def test_to_pipeline_configuration_valid_complete(
    resources,
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that a pipeline configuration can be constructed from a
    RegisteredPipeline object and a valid list of pipeline resources,
    where resources are provided for every step.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        step_resources = {step.name: resources for step in graph.steps}
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )

    pipeline_configuration = pipeline._to_pipeline_configuration(
        pipeline_resources=step_resources
    )
    assert pipeline_configuration["pipeline_version_id"] == pipeline.id
    assert len(graph.steps) == len(pipeline_configuration["steps"])
    for graph_step, config_step in zip(graph.steps, pipeline_configuration["steps"]):
        # All steps provided are included in the configuration.
        assert graph_step.name == config_step["name"]
        # All steps in the config have resources
        assert "resources" in config_step.keys()


@given(resources=resources())
def test_to_pipeline_configuration_valid_incomplete(
    resources,
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that a pipeline configuration can be constructed from a
    RegisteredPipeline object and a valid list of pipeline resources,
    where resources are not provided for every step.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        partial_steps = list(graph.steps)[:-1]
        excluded_step = list(graph.steps)[-1]
        step_resources = {step.name: resources for step in partial_steps}
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )

    pipeline_configuration = pipeline._to_pipeline_configuration(
        pipeline_resources=step_resources
    )
    assert pipeline_configuration["pipeline_version_id"] == pipeline.id
    # All steps have been included in the configuration
    assert len(graph.steps) == len(pipeline_configuration["steps"])
    # Compare the steps that have resources, allowing zip to drop the excluded step.
    for graph_step, config_step in zip(partial_steps, pipeline_configuration["steps"]):
        # All steps provided are included in the configuration.
        assert graph_step.name == config_step["name"]
        # All steps for which resource were provided have resources in the config.
        assert "resources" in config_step.keys()
    # The step for which no resources were provided is in the config without resources.
    assert excluded_step.name == pipeline_configuration["steps"][-1]["name"]
    assert "resources" not in pipeline_configuration["steps"][-1].keys()


@given(resources=resources())
def test_to_pipeline_configuration_invalid_resources(
    resources,
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that the expected errors are raised when an invalid pipeline resources
    are provided.

    Invalid resources include:
    - a step name not in the pipeline -> ValueError
    - a step name that is not a string -> TypeError
    - a step resource that is not a Resources object -> TypeError
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rmv")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        step_resources = {step.name: resources for step in graph.steps}
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )
    # step name not in pipeline
    step_resources["invalid_step_name"] = resources
    with pytest.raises(ValueError) as err:
        pipeline._to_pipeline_configuration(pipeline_resources=step_resources)
    assert (
        str(err.value) == "pipeline_resources contains resources for a step not in the "
        "pipeline: 'invalid_step_name'"
    )
    step_resources.pop("invalid_step_name")
    # step name not a string
    step_resources.update({123: resources})
    with pytest.raises(TypeError) as err2:
        pipeline._to_pipeline_configuration(pipeline_resources=step_resources)
    assert (
        str(err2.value) == "pipeline_resources keys must be type str, not <class 'int'>"
    )
    step_resources.pop(123)
    # step resource not a Resources object
    step_resources.update({"step_1": "not_resources"})
    with pytest.raises(TypeError) as err3:
        pipeline._to_pipeline_configuration(pipeline_resources=step_resources)
    assert (
        str(err3.value)
        == "pipeline_resources values must be type Resources, not <class 'str'>"
    )


def test_to_pipeline_configuration_no_resources(
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_registered_model,
) -> None:
    """Test that a pipeline configuration can be constructed from a
    RegisteredPipeline object without providing any pipeline resources.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()
        pipeline = RegisteredPipeline(
            graph=graph,
            registered_model_version=make_mock_registered_model_version(),
        )
    pipeline_configuration = pipeline._to_pipeline_configuration()
    assert pipeline_configuration["pipeline_version_id"] == pipeline.id
    for graph_step, config_step in zip(graph.steps, pipeline_configuration["steps"]):
        # All steps are included in the configuration
        assert graph_step.name == config_step["name"]
        # No resources are found in the resulting configuration
        assert "resources" not in config_step.keys()


def test_from_pipeline_definition(
    make_mock_registered_model_version,
    mocked_responses,
) -> None:
    """Test that a RegisteredPipeline object can be constructed from a pipeline
    definition.

    The model version's `_get_artifact` function is overidden in the
    mocked RMV fixture to return a simple, consistent pipeline definition.
    Calls related to the fetching of the RMV and RM are mocked.
    """
    rmv = make_mock_registered_model_version()
    mocked_responses.get(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/1",
        json={},
        status=200,
    )
    mocked_responses.get(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/2",
        json={},
        status=200,
    )
    mocked_responses.get(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/registered_models/0",
        json={},
        status=200,
    )
    pipeline = RegisteredPipeline._from_pipeline_definition(
        registered_model_version=rmv,
    )
    assert isinstance(pipeline, RegisteredPipeline)
    assert pipeline.id == rmv.id


def test_bad_mutation_of_graph_steps_exception(
    make_mock_registered_model,
    make_mock_registered_model_version,
    make_mock_pipeline_graph,
):
    """Test that we throw the correct exception when a user tries to mutate
    the steps of a graph in an inappropriate way.
    """
    mocked_rm = make_mock_registered_model(id=123, name="test_rm")
    mocked_rmv = make_mock_registered_model_version()
    with patch.object(
        verta.pipeline.PipelineStep, "_get_registered_model", return_value=mocked_rm
    ):
        graph = make_mock_pipeline_graph()

    graph.steps.add("not_a_step")
    with pytest.raises(TypeError) as err:
        RegisteredPipeline(graph=graph, registered_model_version=mocked_rmv)
    assert (
        str(err.value) == f"individual steps of a PipelineGraph must be type"
        f" PipelineStep, not <class 'str'>."
    )
