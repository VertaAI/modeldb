import os

import pytest
from click.testing import CliRunner
from verta._cli import cli

from verta._internal_utils import _config_utils


class TestInit:
    def test_init(self):
        runner = CliRunner()
        with runner.isolated_filesystem():
            result = runner.invoke(cli, ['init'])
            assert not result.exception
            assert result.output.startswith("initialized empty config file ")
            assert os.path.isfile(_config_utils.CONFIG_YAML_FILENAME)

            result = runner.invoke(cli, ['init'])
            assert not result.exception
            assert result.output.startswith("found existing config file ")
