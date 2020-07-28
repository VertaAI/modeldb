from click.testing import CliRunner

from verta._cli import cli
from verta._internal_utils import _utils


class TestCreate:
    def test_create_endpoint(self, client, created_endpoints):
        endpoint_name = _utils.generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'create', 'endpoint', endpoint_name],
        )

        assert not result.exception

        endpoint = client.get_endpoint(endpoint_name)
        assert endpoint

        created_endpoints.append(endpoint)
