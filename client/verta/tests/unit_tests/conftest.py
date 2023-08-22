# -*- coding: utf-8 -*-

"""Pytest fixtures for use in client unit tests."""

import json
import os
import random
from typing import Any, Callable, Dict, List
from unittest.mock import patch

import pytest
import responses

from verta._internal_utils._utils import Configuration, Connection
from verta._protos.public.registry import RegistryService_pb2 as _RegistryService
from verta.client import Client
from verta.credentials import EmailCredentials
from verta.endpoint import Endpoint
from verta.endpoint.resources import NvidiaGPU, NvidiaGPUModel, Resources
from verta.pipeline import PipelineGraph, PipelineStep
from verta.registry.entities import RegisteredModelVersion


@pytest.fixture(scope="session")
def mock_client(mock_conn) -> Client:
    """Return a mocked object of the Client class for use in tests"""
    # with patch.dict(os.environ, {'VERTA_EMAIL': 'test_email@verta.ai', 'VERTA_DEV_KEY':'123test1232dev1232key123'}):
    client = Client(_connect=False)
    client._conn = mock_conn
    return client


@pytest.fixture(scope="session")
def mock_conn() -> Connection:
    """Return a mocked object of the _internal_utils._utils.Connection class for use in tests"""
    with patch.dict(
        os.environ,
        {
            "VERTA_EMAIL": "test_email@verta.ai",
            "VERTA_DEV_KEY": "123test1232dev1232key123",
        },
    ):
        credentials = EmailCredentials.load_from_os_env()

    return Connection(scheme="https", socket="test_socket", credentials=credentials)


@pytest.fixture(scope="session")
def mock_config() -> Configuration:
    """Return a mocked object of the _internal_utils._utils.Configuration class for use in tests"""
    return Configuration(use_git=False, debug=False)


@pytest.fixture
def mocked_responses():
    with responses.RequestsMock() as rsps:
        yield rsps


@pytest.fixture
def mock_endpoint(mock_conn, mock_config) -> Endpoint:
    """Return a mocked object of the Endpoint class for use in tests"""

    class MockEndpoint(Endpoint):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    return MockEndpoint(conn=mock_conn, conf=mock_config, workspace=456, id=123)


@pytest.fixture(scope="session")
def make_mock_simple_pipeline_definition() -> Callable:
    """
    Return a callable function for creating a simple mocked pipeline
    definition for use in tests, including a parameter for the pipeline
    id to ensure consistency in tests that mock creation of a pipeline
    object from a pipeline definition.
    """

    def simple_pipeline_definition(id: int) -> Dict[str, Any]:
        return {
            "graph": [
                {"predecessors": [], "name": "step1"},
                {"predecessors": ["step1"], "name": "step2"},
            ],
            "pipeline_version_id": id,
            "steps": [
                {
                    "model_version_id": 1,
                    "name": "step1",
                },
                {
                    "model_version_id": 2,
                    "name": "step2",
                },
            ],
        }

    return simple_pipeline_definition


@pytest.fixture(scope="session")
def make_mock_registered_model_version(
    mock_conn, mock_config, make_mock_simple_pipeline_definition
) -> Callable:
    """Return a callable function for creating mocked objects of the
    RegisteredModelVersion class for use in tests that require multiple
    unique instances.
    """

    class MockRegisteredModelVersion(RegisteredModelVersion):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

        def _get_artifact(self, key=None, artifact_type=None):
            return json.dumps(make_mock_simple_pipeline_definition(id=self.id)).encode(
                "utf-8"
            )

    def _make_mock_registered_model_version():
        """Return a mocked ``RegisteredModelVersion``.

        ``id`` and ``registered_model_id`` will be random and unique for the
        test session.

        """
        ids = set()
        model_ver_id = random.randint(1, 1000000)
        while model_ver_id in ids:
            model_ver_id = random.randint(1, 1000000)
        ids.add(model_ver_id)

        reg_model_id = random.randint(1, 1000000)
        while reg_model_id in ids:
            reg_model_id = random.randint(1, 1000000)
        ids.add(reg_model_id)

        return MockRegisteredModelVersion(
            mock_conn,
            mock_config,
            _RegistryService.ModelVersion(
                id=model_ver_id,
                registered_model_id=reg_model_id,
                version="test_version_name",
            ),
        )

    return _make_mock_registered_model_version


@pytest.fixture(scope="session")
def make_mock_pipeline_step(make_mock_registered_model_version) -> Callable:
    """
    Return a callable function for creating mocked objects of the PipelineStep
    class for use in tests that require multiple unique instances.
    """

    class MockPipelineStep(PipelineStep):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    def _make_mock_pipeline_step():
        return MockPipelineStep(
            model_version=make_mock_registered_model_version(),
            name="test_pipeline_step_name",
            predecessors=[],
        )

    return _make_mock_pipeline_step


@pytest.fixture(scope="session")
def make_mock_pipeline_graph(make_mock_pipeline_step) -> Callable:
    """
    Return a callable function for creating mocked objects of the PipelineGraph
    class for use in tests that require multiple unique instances.
    """

    class MockPipelineGraph(PipelineGraph):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    def _make_mock_pipeline_graph():
        step1 = make_mock_pipeline_step()
        step1.set_name("step1")
        step2 = make_mock_pipeline_step()
        step2.set_name("step2")
        step3 = make_mock_pipeline_step()
        step3.set_name("step3")
        return MockPipelineGraph(steps=[step1, step2, step3])

    return _make_mock_pipeline_graph


@pytest.fixture(scope="session")
def make_mock_step_resources() -> Callable:
    """
    Return a callable function for generating a list of mocked resources for
    a given list of step names.
    """

    def _make_mock_step_resources(step_names: List[str]) -> Dict[str, Resources]:
        res = dict()
        for name in step_names:
            res.update(
                {
                    name: Resources(
                        cpu=random.randint(1, 10),
                        memory="5Gi",
                        nvidia_gpu=NvidiaGPU(
                            model=NvidiaGPUModel.T4, number=random.randint(1, 10)
                        ),
                    ),
                }
            )
        return res

    return _make_mock_step_resources
