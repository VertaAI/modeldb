# -*- coding: utf-8 -*-

"""Pytest fixtures for use in client unit tests."""

import os
import random
from unittest.mock import patch

import pytest
import responses

from verta._internal_utils._utils import Configuration, Connection
from verta._protos.public.registry import RegistryService_pb2 as _RegistryService
from verta.client import Client
from verta.credentials import EmailCredentials
from verta.endpoint import Endpoint
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
def mock_registered_model_version(make_mock_registered_model_version):
    """Return a mocked object of the RegisteredModelVersion class for use in tests"""

    return make_mock_registered_model_version()


@pytest.fixture(scope="session")
def make_mock_registered_model_version(mock_conn, mock_config):
    """Factory fixture for creating mocked ``RegisteredModelVersion``s."""

    class MockRegisteredModelVersion(RegisteredModelVersion):
        def __repr__(self):  # avoid network calls when displaying test results
            return object.__repr__(self)

    model_ver_ids = set()
    reg_model_ids = set()

    def _make_mock_registered_model_version():
        """Return a mocked ``RegisteredModelVersion``.

        ``id`` and ``registered_model_id`` will be random and unique for the
        test session.

        """
        model_ver_id = random.randint(1, 999)
        while model_ver_id in model_ver_ids:
            model_ver_id = random.randint(1, 999)

        reg_model_id = random.randint(1, 999)
        while reg_model_id in reg_model_ids:
            reg_model_id = random.randint(1, 999)

        return MockRegisteredModelVersion(
            mock_conn,
            mock_config,
            _RegistryService.ModelVersion(
                id=model_ver_id,
                registered_model_id=reg_model_id,
            ),
        )

    return _make_mock_registered_model_version
