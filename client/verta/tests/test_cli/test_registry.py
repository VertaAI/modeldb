import json

import pytest
from click.testing import CliRunner

from verta._cli import cli
from verta._registry.model import RegisteredModel


pytest.skip("registry not yet available in backend", allow_module_level=True)


class TestCreate:
    def test_create_model(self):
        model_name = RegisteredModel._generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodel', model_name, '-l', 'label1', '-l', 'label2'],
        )

        assert not result.exception

        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodel', model_name],
        )

        assert not result.exception
        assert "name: \"{}\"".format(model_name) in result.output

class TestGet:
    def test_get_model(self, registered_model):
        model_name = registered_model.name

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodel', model_name],
        )

        assert not result.exception
        assert "name: \"{}\"".format(model_name) in result.output

    def test_get_model_output_json(self, registered_model):
        model_name = registered_model.name

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodel', model_name, "--output=json"],
        )

        assert not result.exception
        model_json_str = result.output.strip().splitlines()[-1]
        model_json = json.loads(model_json_str)
        assert model_json['name'] == model_name

    def test_get_model_wrong_name_error(self, strs):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodel', strs[0]],
        )

        assert result.exception
        assert result.output.strip().endswith("not found")


class TestList:
    pass


class TestUpdate:
    pass
