# -*- coding: utf-8 -*-

"""Pytest fixtures for use in client unit tests."""

import json
import os
import random
from typing import Any, Callable, Dict, Optional
from unittest.mock import patch
import uuid

import pytest
import responses

from verta._internal_utils._utils import Configuration, Connection
from verta._protos.public.registry import RegistryService_pb2 as _RegistryService
from verta._protos.public.modeldb import (
    ExperimentRunService_pb2 as _ExperimentRunService,
)
from verta.client import Client
from verta.credentials import EmailCredentials
from verta.endpoint import Endpoint
from verta.pipeline import PipelineGraph, PipelineStep
from verta.registry.entities import RegisteredModel, RegisteredModelVersion
from verta.tracking.entities import ExperimentRun


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
    """Return a callable function for creating a simple mocked pipeline
    definition.

    For use in tests, including a parameter for the pipeline
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
def make_mock_registered_model(mock_conn, mock_config) -> Callable:
    """Return a callable function for creating mocked objects of the
    RegisteredModel class.
    """

    class MockRegisteredModel(RegisteredModel):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    def _make_mock_registered_model(id: int, name: str):
        """Return a mocked RegisteredModel object."""

        return MockRegisteredModel(
            mock_conn,
            mock_config,
            _RegistryService.RegisteredModel(
                id=id,
                name=name,
            ),
        )

    return _make_mock_registered_model


@pytest.fixture(scope="session")
def make_mock_registered_model_version(
    mock_conn, mock_config, make_mock_simple_pipeline_definition
) -> Callable:
    """Return a callable function for creating mocked objects of the
    RegisteredModelVersion class.
    """
    unique_ids = set()

    class MockRegisteredModelVersion(RegisteredModelVersion):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

        def _get_artifact(self, key=None, artifact_type=None):
            if key == "pipeline.json":
                return json.dumps(
                    make_mock_simple_pipeline_definition(id=self.id)
                ).encode("utf-8")

    def _make_mock_registered_model_version():
        """Return a mocked ``RegisteredModelVersion``.

        ``id`` and ``registered_model_id`` will be random and unique for the
        test session.

        """
        model_ver_id = random.randint(1, 1000000)
        while model_ver_id in unique_ids:
            model_ver_id = random.randint(1, 1000000)
        unique_ids.add(model_ver_id)

        reg_model_id = random.randint(1, 1000000)
        while reg_model_id in unique_ids:
            reg_model_id = random.randint(1, 1000000)
        unique_ids.add(reg_model_id)

        return MockRegisteredModelVersion(
            mock_conn,
            mock_config,
            _RegistryService.ModelVersion(
                id=model_ver_id,
                registered_model_id=reg_model_id,
                version="test_model_version_name",
            ),
        )

    return _make_mock_registered_model_version


@pytest.fixture(scope="session")
def make_mock_experiment_run(mock_conn, mock_config) -> Callable:
    """Factory fixture for mocked ExperimentRun objects."""
    unique_ids = set()

    class MockExperimentRun(ExperimentRun):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    def _make_mock_experiment_run():
        """Return a mocked ``RegisteredModelVersion``.

        ``id`` will be random and unique for the test session.

        """
        return MockExperimentRun(
            mock_conn,
            mock_config,
            _ExperimentRunService.ExperimentRun(id=str(uuid.uuid4())),
        )

    return _make_mock_experiment_run


@pytest.fixture(scope="session")
def make_mock_pipeline_step(make_mock_registered_model_version) -> Callable:
    """Return a callable function for creating mocked objects of the PipelineStep
    class.

    The optional `name` parameter is for use in tests where names must be
    known for assertions.
    """
    unique_names = set()

    class MockPipelineStep(PipelineStep):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    def _make_unique_name():
        name = f"step{random.randint(1, 1000000)}"
        while name in unique_names:
            name = f"step{random.randint(1, 1000000)}"
        unique_names.add(name)
        return name

    def _make_mock_pipeline_step(name: Optional[str] = None):
        return MockPipelineStep(
            registered_model_version=make_mock_registered_model_version(),
            name=name if name else _make_unique_name(),
            predecessors=set(),
        )

    return _make_mock_pipeline_step


@pytest.fixture(scope="session")
def make_mock_pipeline_graph(make_mock_pipeline_step) -> Callable:
    """Return a callable function for creating mocked objects of the PipelineGraph
    class.
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
        return MockPipelineGraph(steps={step1, step2, step3})

    return _make_mock_pipeline_graph
