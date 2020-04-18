import os

import yaml

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
        name = "banana"
        url = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])

            result = runner.invoke(cli, ['remote', 'add', name, url])
            assert not result.exception
            with _config_utils.read_config() as config:
                remotes = config['remotes']
                remote = remotes[name]
                assert remote['url'] == url
                assert remote['branch'] == "master"

    def test_use(self):
        # TODO: create a new repo and use it
        name = "banana"
        url = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])
            runner.invoke(cli, ['remote', 'add', name, url])

            result = runner.invoke(cli, ['remote', 'use', name])
            assert not result.exception
            with _config_utils.read_config() as config:
                assert config['current-remote'] == name
