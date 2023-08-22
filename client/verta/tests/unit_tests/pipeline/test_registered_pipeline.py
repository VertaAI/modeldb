# -*- coding: utf-8 -*-
"""Unit tests for the RegisteredPipeline class"""

import pytest
from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import pipeline_definition
from verta.pipeline import RegisteredPipeline


def test_copy_graph(
    make_mock_pipeline_graph, make_mock_registered_model_version
) -> None:
    """Test that the graph of a RegisteredPipeline can be copied"""
    graph = make_mock_pipeline_graph()
    pipeline = RegisteredPipeline(
        graph=graph,
        registered_model_version=make_mock_registered_model_version(),
    )
    copied_graph = pipeline.copy_graph()
    assert copied_graph.steps == graph.steps  # same steps
    assert copied_graph is not graph  # different objects


@given(pipeline_definition=pipeline_definition())
@settings(
    suppress_health_check=[HealthCheck.function_scoped_fixture],
    deadline=None,
)
def test_log_pipeline_definition_artifact(
    pipeline_definition,
    mocked_responses,
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
) -> None:
    """
    Verify the expected sequence of calls when a pipeline definition
    is logged as an artifact to the pipeline's model version.
    """
    rmv = make_mock_registered_model_version()
    pipeline = RegisteredPipeline(
        graph=make_mock_pipeline_graph(),
        registered_model_version=rmv,
    )
    # Fetch the model
    mocked_responses.get(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/{pipeline.id}",
        json={},
        status=200,
    )
    # Fetch the model version
    mocked_responses.put(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/registered_models/0/model_versions/{pipeline.id}",
        json={},
        status=200,
    )
    # Fetch the artifact upload URL
    mocked_responses.post(
        f"{rmv._conn.scheme}://{rmv._conn.socket}/api/v1/registry/model_versions/{pipeline.id}/getUrlForArtifact",
        json={
            "url": f"https://account.s3.amazonaws.com/development/ModelVersionEntity/"
            f"{pipeline.id}/pipeline.json"
        },
        status=200,
    )
    # Upload the artifact
    mocked_responses.put(
        f"https://account.s3.amazonaws.com/development/ModelVersionEntity/{pipeline.id}/pipeline.json",
        json={},
        status=200,
    )
    pipeline._log_pipeline_definition_artifact()


def test_to_pipeline_definition(
    make_mock_pipeline_graph, make_mock_registered_model_version
) -> None:
    """Test that a pipeline definition can be constructed from a
    RegisteredPipeline object.

    In depth testing of the `_to_graph_definition`
    and `to_steps_definition` functions are handled in unit tests for
    PipelineGraph.
    """
    graph = make_mock_pipeline_graph()
    pipeline = RegisteredPipeline(
        graph=graph,
        registered_model_version=make_mock_registered_model_version(),
    )
    pipeline_definition = pipeline._to_pipeline_definition()
    assert pipeline_definition == {
        "pipeline_version_id": pipeline.id,
        "graph": graph._to_graph_definition(),
        "predecessors": graph._to_steps_definition(),
    }


def test_to_pipeline_configuration_valid(
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_step_resources,
) -> None:
    """Test that a valid pipeline configuration can be constructed from a
    RegisteredPipeline object and a valid list of pipeline resources.
    """
    graph = make_mock_pipeline_graph()
    step_names = [step.name for step in graph.steps]
    mock_res = make_mock_step_resources(step_names)
    pipeline = RegisteredPipeline(
        graph=graph,
        registered_model_version=make_mock_registered_model_version(),
    )

    pipeline_configuration = pipeline._to_pipeline_configuration(
        pipeline_resources=mock_res
    )
    assert pipeline_configuration["pipeline_version_id"] == pipeline.id
    for graph_step, config_step in zip(graph.steps, pipeline_configuration["steps"]):
        # All steps are included in the configuration
        assert graph_step.name == config_step["name"]
        # All steps in the config have resources
        assert "resources" in config_step.keys()


def test_to_pipeline_configuration_invalid_resources(
    make_mock_pipeline_graph,
    make_mock_registered_model_version,
    make_mock_step_resources,
) -> None:
    """Test that a ValueError is raised when an invalid step name is included
    in the provided pipeline resources. (Does not match a step name in the
    pipeline's graph)
    """
    graph = make_mock_pipeline_graph()
    step_names = [step.name for step in graph.steps]
    mock_res = make_mock_step_resources(step_names)
    mock_res["invalid_step_name"] = make_mock_step_resources(["invalid_step_name"])
    pipeline = RegisteredPipeline(
        graph=graph,
        registered_model_version=make_mock_registered_model_version(),
    )

    with pytest.raises(ValueError):
        pipeline._to_pipeline_configuration(pipeline_resources=mock_res)


def test_to_pipeline_configuration_no_resources(
    make_mock_pipeline_graph, make_mock_registered_model_version
) -> None:
    """Test that a pipeline configuration can be constructed from a
    RegisteredPipeline object without providing pipeline resources.
    """
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
        # No resources are found in the configuration
        assert "resources" not in config_step.keys()


def test_from_pipeline_definition(
    make_mock_registered_model_version,
    mock_conn,
    mock_config,
    mocked_responses,
) -> None:
    """Test that a RegisteredPipeline object can be constructed from a pipeline
    definition.

    The model version's `_get_artifact` function is mocked to
    return a simple, consistent pipeline definition. Calls related to the
    fetching of the artifact are mocked.
    """
    mocked_responses.get(
        "https://test_socket/api/v1/registry/model_versions/1",
        json={},
        status=200,
    )
    mocked_responses.get(
        "https://test_socket/api/v1/registry/model_versions/2",
        json={},
        status=200,
    )
    mocked_responses.get(
        "https://test_socket/api/v1/registry/registered_models/0",
        json={},
        status=200,
    )

    rmv = make_mock_registered_model_version()
    pipeline = RegisteredPipeline._from_pipeline_definition(
        registered_model_version=rmv,
        conn=mock_conn,
        conf=mock_config,
    )
    assert isinstance(pipeline, RegisteredPipeline)
    assert pipeline.id == rmv.id
