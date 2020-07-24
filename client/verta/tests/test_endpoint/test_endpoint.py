import pytest
from verta._internal_utils import _utils


class TestEndpoint:
    def test_create(self, client):
        name = _utils.generate_default_name()
        assert client.set_endpoint(name)

    def test_get(self, client):
        name = _utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_endpoint(name)

        endpoint = client.set_endpoint(name)

        assert endpoint.id == client.get_endpoint(endpoint.path).id
        assert endpoint.id == client.get_endpoint(id=endpoint.id).id

    def test_get_by_name(self, client):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(endpoint.path).id
        
    def test_get_by_id(self, client):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

    def test_list(self, client):
        name = _utils.generate_default_name()
        endpoint = client.set_endpoint(name)

        endpoints = client.endpoints()
        assert len(endpoints) >= 1
        has_new_id = False
        for item in endpoints:
            assert item.id
            if item.id == endpoint.id:
                has_new_id = True
        assert has_new_id


