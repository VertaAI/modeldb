import json
import os
import pickle
import zipfile

import pytest
from click.testing import CliRunner

from verta import Client
from verta._cli import cli
from verta._cli.registry.update import add_attributes
from verta._registry import RegisteredModel
from verta._internal_utils import _utils
from verta.environment import Python
from verta.utils import ModelAPI
from verta.endpoint.update._strategies import DirectUpdateStrategy


from ..utils import sys_path_manager

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestCreate:
    def test_create_model(self, client, created_registered_models):
        model_name = RegisteredModel._generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodel', model_name, '-l', 'label1', '-l', 'label2'],
        )

        assert not result.exception

        registered_model = client.get_registered_model(model_name)
        assert registered_model

        created_registered_models.append(registered_model)

    def test_create_version(self, registered_model, in_tempdir, requirements_file):
        LogisticRegression = pytest.importorskip('sklearn.linear_model').LogisticRegression

        model_name = registered_model.name
        version_name = "my version"

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        classifier_name = "tiny2.pth"
        classifier = LogisticRegression()
        with open(classifier_name, 'wb') as f:
            pickle.dump(classifier, f)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, '-l', 'label1', '-l', 'label2',
             "--artifact", "file={}".format(filename), "--model", classifier_name, "--requirements", requirements_file.name],
        )
        assert not result.exception

        model_version = registered_model.get_version(name=version_name)
        assert model_version.name in result.output
        assert model_version.get_artifact("file").read() == FILE_CONTENTS
        assert model_version.get_labels() == ["label1", "label2"]
        assert model_version.get_model().get_params() == classifier.get_params()

        # Check environment:
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=reqs)
        assert repr(env) == str(model_version.get_environment())

    def test_create_version_invalid_key(self, registered_model, in_tempdir):
        model_name = registered_model.name

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2 ** 16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, "my version",
             "--artifact", "model={}".format(filename)],
        )
        assert result.exception
        assert "the key \"model\" is reserved for model" in result.output

        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, "my version 2",
             "--artifact", "file={}".format(filename), "--artifact", "file={}".format(filename)],
        )
        assert result.exception
        assert "cannot have duplicate artifact keys" in result.output

    def test_create_version_wrong_model_name(self, strs):
        version_name = "my version"

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', strs[0], version_name],
        )

        assert result.exception
        assert result.output.strip().endswith("not found")

    def test_create_version_from_run(self, experiment_run, model_for_deployment, registered_model):
        np = pytest.importorskip("numpy")
        model_name = registered_model.name
        version_name = "from_run"

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        artifact = np.random.random((36, 12))
        experiment_run.log_artifact("some-artifact", artifact)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "--from-run", experiment_run.id],
        )
        assert not result.exception

        model_version = registered_model.get_version(name=version_name)
        assert model_version.name in result.output

        env_str = str(model_version.get_environment())
        assert 'scikit-learn' in env_str
        assert 'Python' in env_str

        assert model_for_deployment['model'].get_params() == model_version.get_model().get_params()
        assert np.array_equal(model_version.get_artifact("some-artifact"), artifact)

    def test_create_from_run_with_model_artifact_error(self, experiment_run, registered_model, in_tempdir):
        model_name = registered_model.name
        version_name = "from_run"

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "--artifact",
             "file {}".format(filename)],
        )
        error_message = "key and path for artifacts must be separated by a '='"
        assert result.exception
        assert error_message in result.output
        error_message = "--from-run cannot be provided alongside other options, except for --workspace"
        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "--artifact",
             "file={}".format(filename), "--from-run", experiment_run.id],
        )
        assert result.exception
        assert error_message in result.output

        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "--model", filename,
             "--from-run", experiment_run.id],
        )
        assert result.exception
        assert error_message in result.output

        result = runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "-l", "some label",
             "--from-run", experiment_run.id],
        )
        assert result.exception
        assert error_message in result.output

    def test_create_workspace_config(self, client, organization, in_tempdir, created_registered_models):
        model_name = _utils.generate_default_name()
        version_name = _utils.generate_default_name()

        client_config = {
            "workspace": organization.name
        }

        filepath = "verta_config.json"
        with open(filepath, "w") as f:
            json.dump(client_config, f)

        runner = CliRunner()
        runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodel', model_name],
        )

        client = Client()
        model = client.get_registered_model(model_name)
        created_registered_models.append(model)
        assert model.workspace == organization.name

    def test_create_version_with_custom_modules(self, client, registered_model, created_endpoints):
        torch = pytest.importorskip("torch")
        np = pytest.importorskip("numpy")

        model_name = registered_model.name
        version_name = "my version"

        with sys_path_manager() as sys_path:
            sys_path.append(".")

            from models.nets import FullyConnected
            train_data = torch.rand((2, 4))

            model_path = "classifier.pt"
            classifier = FullyConnected(num_features=4, hidden_size=32, dropout=0.2)

            torch.save(classifier, model_path)

            requirements_path = "requirements.txt"
            with open(requirements_path, "w") as f:
                f.write("torch=={}".format(torch.__version__))

            runner = CliRunner()
            result = runner.invoke(
                cli,
                ['registry', 'create', 'registeredmodelversion', model_name, version_name,
                 "--model", model_path, "--custom-module", "models/", "--requirements",
                 requirements_path],
            )
            assert not result.exception

            retrieved_model = registered_model.get_version(name=version_name).get_model()
            assert torch.allclose(classifier(train_data), retrieved_model(train_data))

            os.remove(model_path)
            os.remove(requirements_path)

            # TODO: consolidate these in the command above
            model_version = registered_model.get_version(name=version_name)

            # Log model api:
            model_api = ModelAPI(train_data.tolist(), classifier(train_data).tolist())
            model_api["model_packaging"] = {
                "deserialization": "cloudpickle",
                "type": "torch",
                "python_version": "2.7.17"
            }
            model_version.log_artifact("model_api.json", model_api, True, "json")

            path = _utils.generate_default_name()
            endpoint = client.set_endpoint(path)
            created_endpoints.append(endpoint)
            endpoint.update(model_version, DirectUpdateStrategy(), wait=True)

            test_data = torch.rand((4, 4))
            prediction = torch.tensor(endpoint.get_deployed_model().predict(test_data.tolist()))
            assert torch.all(classifier(test_data).eq(prediction))


class TestGet:
    def test_get_model(self, registered_model):
        model_name = registered_model.name

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodel', model_name],
        )

        assert not result.exception
        assert "name: {}".format(model_name) in result.output
        assert "id: {}".format(registered_model.id) in result.output

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

    def test_get_version(self, registered_model):
        model_name = registered_model.name
        version_name = "my-version"
        model_version = registered_model.get_or_create_version(version_name)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodelversion', model_name, version_name],
        )

        assert not result.exception
        assert "version: {}".format(version_name) in result.output
        assert str(model_version.id) in result.output
        assert str(model_version.registered_model_id) in result.output

    def test_get_version_output_json(self, registered_model):
        model_name = registered_model.name
        version_name = "my-version"
        model_version = registered_model.get_or_create_version(version_name)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodelversion', model_name, version_name, "--output=json"],
        )

        assert not result.exception
        version_json_str = result.output.strip().splitlines()[-1]
        version_json = json.loads(version_json_str)
        assert version_json['version'] == version_name

    def test_get_version_wrong_name_error(self, registered_model, strs):
        model_name = registered_model.name

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'get', 'registeredmodelversion', model_name, strs[0]],
        )

        assert result.exception
        assert result.output.strip().endswith("not found")


class TestList:
    def test_list_model(self, created_registered_models):
        client = Client()
        model1 = client.get_or_create_registered_model()
        created_registered_models.append(model1)
        label = model1._msg.name + "label1"
        model1.add_label(label)
        model1.add_label("label2")
        model2 = client.get_or_create_registered_model()
        created_registered_models.append(model2)
        model = client.get_or_create_registered_model()
        created_registered_models.append(model)
        model.add_label(label)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodel', '--filter', "labels == \"{}\"".format(label)],
        )

        assert not result.exception
        assert model1.name in result.output
        assert model.name in result.output

        result = runner.invoke(
            cli,
            ['registry', 'list', 'registeredmodel', "--output=json"],
        )

        assert not result.exception
        assert model1.name in result.output
        assert model.name in result.output
        assert model2.name in result.output

    def test_list_version(self, created_registered_models):
        client = Client()
        runner = CliRunner()

        model1 = client.get_or_create_registered_model()
        created_registered_models.append(model1)
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
        assert version1_name in result.output
        assert version2_name in result.output

        version2.add_label(label)
        model2 = client.get_or_create_registered_model()
        created_registered_models.append(model2)
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
        assert version2_1_name in result.output
        assert version2_name in result.output

class TestUpdate:
    def test_update_model(self, registered_model):
        model_name = registered_model.name
        assert registered_model.get_labels() == []

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodel', model_name, '-l', 'label1', '-l', 'label2'],
        )
        assert not result.exception
        registered_model._fetch_with_no_cache() # invalidate cache
        assert registered_model.get_labels() == ["label1", "label2"]

        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodel', model_name],
        )
        assert not result.exception
        registered_model._fetch_with_no_cache() # invalidate cache
        assert registered_model.get_labels() == ["label1", "label2"]

        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodel', model_name, '-l', 'label1'],
        )
        assert not result.exception
        registered_model._fetch_with_no_cache() # invalidate cache
        assert registered_model.get_labels() == ["label1", "label2"]


    def test_update_version(self, registered_model, in_tempdir, requirements_file):
        LogisticRegression = pytest.importorskip('sklearn.linear_model').LogisticRegression

        model_name = registered_model.name
        version_name = "my version"
        registered_model.get_or_create_version(version_name)

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        classifier_name = "tiny2.pth"
        classifier = LogisticRegression()
        with open(classifier_name, 'wb') as f:
            pickle.dump(classifier, f)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name,
             '-l', 'label1', '-l', 'label2', "--artifact", "file={}".format(filename),
             "--model", classifier_name, "--requirements", requirements_file.name,
             "--attribute", "att_key=\"value\""],
        )
        assert not result.exception

        model_version = registered_model.get_version(name=version_name)
        assert model_version.get_artifact("file").read() == FILE_CONTENTS
        assert model_version.get_labels() == ["label1", "label2"]
        assert model_version.get_attribute("att_key") == "value"
        assert model_version.get_model().get_params() == classifier.get_params()

        # Check environment:
        reqs = Python.read_pip_file(requirements_file.name)
        env = Python(requirements=reqs)
        assert repr(env) == str(model_version.get_environment())

    def test_update_version_str_value_not_in_quote_error(self, registered_model):
        model_name = registered_model.name
        version_name = "my version"
        registered_model.get_or_create_version(version_name)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name,
             "--attribute", "att_key=value"],
        )
        assert result.exception
        assert "if the attribute's value is a string, consider wrapping it in quotes." in result.output

    def test_update_version_invalid_key(self, registered_model, in_tempdir):
        model_name = registered_model.name
        version_name = "my version"
        registered_model.get_or_create_version(version_name)

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "model={}".format(filename)],
        )
        assert result.exception
        assert "the key \"model\" is reserved for model" in result.output

        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "file={}".format(filename), "--artifact", "file={}".format(filename)],
        )
        assert result.exception
        assert "cannot have duplicate artifact keys" in result.output

        runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "file={}".format(filename)],
        )
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "file={}".format(filename)],
        )
        assert result.exception
        assert "key \"file\" already exists" in result.output

    def test_model_already_logged_error(self, registered_model, in_tempdir):
        LogisticRegression = pytest.importorskip('sklearn.linear_model').LogisticRegression

        model_name = registered_model.name
        version_name = "my version"

        classifier_name = "tiny2.pth"
        classifier = LogisticRegression()
        with open(classifier_name, 'wb') as f:
            pickle.dump(classifier, f)

        runner = CliRunner()
        runner.invoke(
            cli,
            ['registry', 'create', 'registeredmodelversion', model_name, version_name, "--model", classifier_name],
        )

        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--model", classifier_name],
        )
        assert result.exception
        assert "a model has already been associated with the version" in result.output

    def test_overwrite(self, registered_model, in_tempdir):
        LogisticRegression = pytest.importorskip('sklearn.linear_model').LogisticRegression

        model_name = registered_model.name
        version_name = "my version"
        registered_model.get_or_create_version(version_name)

        filename = "tiny1.bin"
        FILE_CONTENTS = os.urandom(2**16)
        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS)

        classifier_name = "tiny2.pth"
        classifier = LogisticRegression()
        with open(classifier_name, 'wb') as f:
            pickle.dump(classifier, f)

        runner = CliRunner()
        runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "file={}".format(filename), "--model", classifier_name],
        )

        # Overwriting:
        filename = "tiny1.bin"
        FILE_CONTENTS_2 = os.urandom(2**16)
        while FILE_CONTENTS_2 == FILE_CONTENTS:
            FILE_CONTENTS_2 = os.urandom(2 ** 16)

        with open(filename, 'wb') as f:
            f.write(FILE_CONTENTS_2)

        classifier_name = "tiny2.pth"
        classifier2 = LogisticRegression(C=0.1)
        with open(classifier_name, 'wb') as f:
            pickle.dump(classifier2, f)

        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--artifact", "file={}".format(filename), "--model", classifier_name, "--overwrite"],
        )
        assert not result.exception

        # Check that the model and artifact are updated:
        model_version = registered_model.get_version(name=version_name)
        assert model_version.get_artifact("file").read() != FILE_CONTENTS
        assert model_version.get_artifact("file").read() == FILE_CONTENTS_2
        assert model_version.get_model().get_params() != classifier
        assert model_version.get_model().get_params() == classifier2.get_params()

    def test_update_with_no_custom_modules(self, registered_model, in_tempdir):
        LogisticRegression = pytest.importorskip('sklearn.linear_model').LogisticRegression

        model_name = registered_model.name
        version_name = "my version"
        registered_model.get_or_create_version(version_name)

        classifier_name = "tiny2.pth"
        CLASSIFIER_CONTENTS = pickle.dumps(LogisticRegression())
        with open(classifier_name, 'wb') as f:
            f.write(CLASSIFIER_CONTENTS)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['registry', 'update', 'registeredmodelversion', model_name, version_name, "--model", classifier_name, "--no-custom-modules"],
        )
        assert not result.exception

        custom_module_filenames = {"__init__.py", "_verta_config.py"}
        model_version = registered_model.get_version(name=version_name)
        with zipfile.ZipFile(model_version.get_artifact("custom_modules"), 'r') as zipf:
            assert custom_module_filenames == set(map(os.path.basename, zipf.namelist()))


@pytest.mark.parametrize(("key", "value", "arg"), (
    ["num", 3.6, ["num=3.6"]], ["str", '3.6', ['str="3.6"']],
    ["dict", {"a": 1, "b": 2}, ['dict={"a": 1, "b": 2}']]))
def test_add_attributes(key, value, arg):
    class TestModelVersion:
        def add_attribute(self, key, value0, overwrite):
            assert value == value0

        def _get_attribute_keys(self):
            return [key]

    model_version = TestModelVersion()
    add_attributes(model_version, arg, True)


def test_multiple_attributes():
    call_number = [0]
    values = [3.6, {"a": 1, "b": 2}]

    class TestModelVersion:
        def add_attribute(self, key, value0, overwrite):
            assert values[call_number[0]] == value0
            call_number[0] += 1

        def _get_attribute_keys(self):
            return ["numl", "dict"]

    model_version = TestModelVersion()
    add_attributes(model_version, ["num=3.6", 'dict={"a": 1, "b": 2}'], True)
