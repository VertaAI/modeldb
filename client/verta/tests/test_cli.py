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


class TestRemote:
    def test_add(self):
        # TODO: create a new repo and use it

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])

            result = runner.invoke(cli, ['remote', 'add', 'banana', 'https://www.verta.ai/'])
            assert not result.exception
            # TODO: assert remote added

    def test_use(self):
        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])
            runner.invoke(cli, ['remote', 'add', 'banana', 'https://www.verta.ai/'])

            result = runner.invoke(cli, ['remote', 'use', 'banana'])
            assert not result.exception
            # TODO: assert remote is current
