import pytest
import verta


@pytest.mark.skip("endpoint not yet available in backend")
class TestEndpoint:
    def test_create(self, client):
        assert client.set_endpoint("/path")
        assert client.endpoint is not None

    def test_get_by_id(self, client):
        endpoint = client.set_endpoint("/path1")

        client.set_endpoint("/path2")  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

