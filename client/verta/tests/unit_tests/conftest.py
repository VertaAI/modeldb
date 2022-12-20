# -*- coding: utf-8 -*-

"""Pytest fixtures for use in client unit tests."""

import os
from unittest.mock import patch

import pytest

from verta._internal_utils._utils import Connection, Configuration
from verta.credentials import EmailCredentials


@pytest.fixture(scope="session")
def mock_conn() -> Connection:
    """ Return a mocked object of the _internal_utils._utils.Connection class for use in tests """
    with patch.dict(os.environ, {'VERTA_EMAIL': 'test_email@verta.ai', 'VERTA_DEV_KEY':'123test1232dev1232key123'}):
        credentials = EmailCredentials.load_from_os_env()

    return Connection(
        scheme='https',
        socket='test_socket',
        credentials=credentials,
    )

@pytest.fixture(scope="session")
def mock_config() -> Configuration:
    """ Return a mocked object of the _internal_utils._utils.Configuration class for use in tests """
    return Configuration(use_git=False, debug=False)
