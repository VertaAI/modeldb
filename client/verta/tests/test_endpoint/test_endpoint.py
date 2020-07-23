import pytest
import verta


@pytest.mark.skip("endpoint not yet available in backend")
class TestEndpoint:
    def test_create(self, client):
        name = verta._internal_utils._utils.generate_default_name()
        assert client.set_endpoint(name)

    def test_get(self, client):
        name = verta._internal_utils._utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_endpoint(name)

        endpoint = client.set_endpoint(name)

        assert endpoint.id == client.get_endpoint(endpoint.path).id
        assert endpoint.id == client.get_endpoint(id=endpoint.id).id

    def test_get_by_name(self, client):
        path = verta._internal_utils._utils.generate_default_name()
        path2 = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(endpoint.path).id
        
    def test_get_by_id(self, client):
        path = verta._internal_utils._utils.generate_default_name()
        path2 = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

