import pytest
import verta


@pytest.mark.skip("functionality not completed yet")
class TestEndpoint:
    def test_create(self, client):
        name = verta._internal_utils._utils.generate_default_name()
        assert client.set_endpoint(name)

    def test_get_by_id(self, client):
        endpoint = client.set_endpoint("/path1")

        client.set_endpoint("/path2")  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

