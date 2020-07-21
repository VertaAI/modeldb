import json

import pytest
from click.testing import CliRunner

from verta import Client
from verta._cli import cli


#pytest.skip("registry not yet available in backend", allow_module_level=True)

class TestCreate:
    pass


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
    def test_list_model(self):
        client = Client()
        model1 = client.get_or_create_registered_model()
        label = model1._msg.name + "label1"
        model1.add_label(label)
        model1.add_label("label2")
        client.get_or_create_registered_model()
        model = client.get_or_create_registered_model()
        model.add_label(label)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodel', '--filter', "labels == \"{}\"".format(label)],
        )

        assert not result.exception
        assert 'result count: 2' in result.output
        assert str(model1._msg.name) in result.output
        assert str(model._msg.name) in result.output

        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodel', '--filter', "labels == \"{}\"".format(label), "--output=json"],
        )

        assert not result.exception
        assert 'result count: 2' in result.output
        assert str(model1._msg.name) in result.output
        assert str(model._msg.name) in result.output

    def test_list_version(self):
        client = Client()
        runner = CliRunner()

        model1 = client.get_or_create_registered_model()
        version1_name = "version1"
        version2_name = "version2"
        model1.get_or_create_version(version1_name)
        version2 = model1.get_or_create_version(version2_name)
        label = model1._msg.name + "label1"
        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodelversion', model1._msg.name]
        )

        assert not result.exception
        assert 'result count: 2' in result.output
        assert version1_name in result.output
        assert version2_name in result.output

        version2.add_label(label)
        model2 = client.get_or_create_registered_model()
        version2_1_name = "version2_1"
        version2_2_name = "version2_2"
        version21 = model2.get_or_create_version(version2_1_name)
        version21.add_label(label)
        model2.get_or_create_version(version2_2_name)
        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodelversion', '--filter', "labels == \"{}\"".format(label), "--output=json"]
        )

        assert not result.exception
        assert 'result count: 2' in result.output
        assert version2_1_name in result.output
        assert version2_name in result.output

class TestUpdate:
    pass

