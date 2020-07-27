from click.testing import CliRunner

from verta._cli import cli
from verta._internal_utils import _utils


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
