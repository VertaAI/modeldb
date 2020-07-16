import os

import pytest
from click.testing import CliRunner
from verta._cli import cli

from verta._internal_utils import _config_utils


pytest.skip("repository sub-CLI has been disabled", allow_module_level=True)


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
        name = "origin"
        url = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])

            result = runner.invoke(cli, ['remote', 'add', name, url])
            assert not result.exception
            with _config_utils.read_merged_config() as config:
                remotes = config['remotes']
                remote = remotes[name]
                assert remote['url'] == url
                assert remote['branch'] == "master"

    def test_add_and_use_if_only(self):
        # TODO: create a new repo and use it
        name1 = "origin"
        url1 = "https://www.verta.ai/"
        name2 = "upstream"
        url2 = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])

            runner.invoke(cli, ['remote', 'add', name1, url1])
            with _config_utils.read_merged_config() as config:
                assert config['current-remote'] == name1

            runner.invoke(cli, ['remote', 'add', name2, url2])
            with _config_utils.read_merged_config() as config:
                # unchanged; still name1
                assert config['current-remote'] == name1

    def test_add_existing_error(self):
        # TODO: create a new repo and use it
        name = "origin"
        url = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])
            runner.invoke(cli, ['remote', 'add', name, url])

            result = runner.invoke(cli, ['remote', 'add', name, url])
            assert result.exception
            assert " already exists" in result.output

    def test_use(self):
        # TODO: create a new repo and use it
        name1 = "origin"
        url1 = "https://www.verta.ai/"
        name2 = "upstream"
        url2 = "https://www.verta.ai/"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])
            runner.invoke(cli, ['remote', 'add', name1, url1])
            runner.invoke(cli, ['remote', 'add', name2, url2])

            result = runner.invoke(cli, ['remote', 'use', name2])
            assert not result.exception
            with _config_utils.read_merged_config() as config:
                assert config['current-remote'] == name2

    def test_use_nonexisting_error(self):
        name = "origin"

        runner = CliRunner()
        with runner.isolated_filesystem():
            runner.invoke(cli, ['init'])

            result = runner.invoke(cli, ['remote', 'use', name])
            assert result.exception
            assert "no such remote: " in result.output
