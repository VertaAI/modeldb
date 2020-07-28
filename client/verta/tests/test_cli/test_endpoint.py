from click.testing import CliRunner

from verta import Client
from verta._cli import cli
from verta._internal_utils import _utils


class TestList:
    def test_list_endpoint(self):
        client = Client()
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint1 = client.get_or_create_endpoint(path)
        endpoint2 = client.get_or_create_endpoint(path2)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'list', 'endpoint'],
        )

        assert not result.exception
        assert path in result.output
        assert path2 in result.output

class TestCreate:
    def test_create_endpoint(self):
        endpoint_name = _utils.generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'create', 'endpoint', endpoint_name],
        )

        assert not result.exception

        result = runner.invoke(
            cli,
            ['deployment', 'get', 'endpoint', endpoint_name],
        )

        assert not result.exception
        assert endpoint_name in result.output
        