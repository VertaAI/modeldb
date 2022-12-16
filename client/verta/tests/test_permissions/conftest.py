import pytest

from verta import Client
from verta._internal_utils import _utils


@pytest.fixture
def create_organization(client, created_entities):
    def _create_organization(name=None):
        org = client._create_organization(name or _utils.generate_default_name())
        created_entities.append(org)

        return org

    return _create_organization


@pytest.fixture
def create_client(host, email, dev_key):
    def _create_client():
        return Client(host, email=email, dev_key=dev_key, debug=True)

    return _create_client
