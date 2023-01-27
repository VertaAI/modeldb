import pytest

from verta import Client


@pytest.fixture
def create_client(host, email, dev_key):
    def _create_client():
        return Client(host, email=email, dev_key=dev_key, debug=True)

    return _create_client
