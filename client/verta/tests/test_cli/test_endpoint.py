from click.testing import CliRunner

import pytest
import time
import tarfile
import json

from verta import Client
from verta._cli import cli
from verta._internal_utils import _utils
from verta.environment import Python
from verta.endpoint.update._strategies import DirectUpdateStrategy
from verta.endpoint.resources import Resources

from ..utils import get_build_ids

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestList:
    def test_list_endpoint(self, organization, created_entities):
        client = Client()
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint1 = client.get_or_create_endpoint(path, workspace=organization.name)
        endpoint2 = client.get_or_create_endpoint(path2, workspace=organization.name)
        created_entities.append(endpoint1)
        created_entities.append(endpoint2)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'list', 'endpoint', '--workspace', organization.name],
        )

        assert not result.exception
        assert path in result.output
        assert path2 in result.output

class TestCreate:
    def test_create_endpoint(self, client, created_entities):
        endpoint_name = _utils.generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'create', 'endpoint', endpoint_name],
        )

        assert not result.exception

        endpoint = client.get_endpoint(endpoint_name)
        assert endpoint

        created_entities.append(endpoint)

    def test_create_workspace_config(self, client, organization, in_tempdir, created_entities):
        client_config = {
            "workspace": organization.name
        }

        filepath = "verta_config.json"
        with open(filepath, "w") as f:
            json.dump(client_config, f)

        endpoint_name = _utils.generate_default_name()

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'create', 'endpoint', endpoint_name],
        )

        assert not result.exception

        client = Client()
        endpoint = client.get_endpoint(endpoint_name)
        assert endpoint.workspace == organization.name

        created_entities.append(endpoint)


class TestGet:
    def test_get(self, client, created_entities, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        path = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_entities.append(endpoint)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'get', 'endpoint', path],
        )
        assert not result.exception
        assert "path: {}".format(endpoint.path) in result.output
        assert "id: {}".format(endpoint.id) in result.output
        assert "curl: <endpoint not deployed>" in result.output

        assert "status" in result.output
        assert "date created" in result.output
        assert "date updated" in result.output
        assert "stage's date created" in result.output
        assert "stage's date updated" in result.output
        assert "components" in result.output

        updated_status = endpoint.update(experiment_run, DirectUpdateStrategy(), True)
        result = runner.invoke(
            cli,
            ['deployment', 'get', 'endpoint', path],
        )
        assert "curl: {}".format(endpoint.get_deployed_model().get_curl()) in result.output


class TestUpdate:
    def test_direct_update_endpoint(self, client, created_entities, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        created_entities.append(endpoint)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "--strategy", "direct"],
        )
        assert not result.exception

        updated_build_ids = get_build_ids(endpoint.get_status())

        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0

    def test_canary_update_endpoint(self, client, created_entities, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        created_entities.append(endpoint)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        canary_rule = json.dumps({
            "rule": "latency_avg_max",
            "rule_parameters": [{"name": "threshold", "value": 0.8}]}
        )
        canary_rule_2 = json.dumps({
            "rule": "error_4xx_rate",
            "rule_parameters": [{"name": "threshold", "value": "0.8"}]}
        )

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-c', canary_rule_2, '-i', 1, "--canary-step", 0.3],
        )

        assert not result.exception

        updated_build_ids = get_build_ids(endpoint.get_status())

        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0

    def test_canary_update_endpoint_env_vars(self, client, created_entities, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        created_entities.append(endpoint)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        canary_rule = json.dumps({
            "rule": "latency_avg_max",
            "rule_parameters": [{"name": "threshold", "value": 0.8}]}
        )

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-i', 1, "--canary-step", 0.3, '--env-vars', '{"VERTA_HOST": "app.verta.ai"}'],
        )

        assert not result.exception

        updated_build_ids = get_build_ids(endpoint.get_status())

        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0

    def test_update_invalid_parameters_error(self, client, created_entities, experiment_run):
        error_msg_1 = "--canary-rule, --canary-interval, and --canary-step can only be used alongside --strategy=canary"
        error_msg_2 = "--canary-rule, --canary-interval, and --canary-step must be provided alongside --strategy=canary"
        error_msg_3 = '`env_vars` must be dictionary of str keys and str values'

        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        created_entities.append(endpoint)

        canary_rule = json.dumps({
            "rule": "latency_avg_max",
            "rule_parameters": [{"name": "threshold", "value": 0.8}]
        })

        # Extra parameters provided:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '-i', 1],
        )
        assert result.exception
        assert error_msg_1 in result.output

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '--canary-step', 0.3],
        )
        assert result.exception
        assert error_msg_1 in result.output


        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '-c', canary_rule],
        )
        assert result.exception
        assert error_msg_1 in result.output

        # Missing canary rule:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-i', 1, "--canary-step", 0.3],
        )
        assert result.exception
        assert error_msg_2 in result.output

        # Missing interval:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, "--canary-step", 0.3],
        )
        assert result.exception
        assert error_msg_2 in result.output

        # Missing step:
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-i', 1],
        )
        assert result.exception
        assert error_msg_2 in result.output

        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "canary",
             '-c', canary_rule, '-i', 1, "--canary-step", 0.3, '--env-vars', '{"VERTA_HOST": 3}'],
        )
        assert result.exception
        assert error_msg_3 in str(result.exception)

    def test_update_from_version(self, client, model_version, created_entities):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)

        path = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_entities.append(endpoint)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', path, '--model-version-id', model_version.id, "--strategy",
             "direct"],
        )
        assert not result.exception

        while not endpoint.get_status()['status'] == "active":
            time.sleep(3)

        test_data = np.random.random((4, 12))
        assert np.array_equal(endpoint.get_deployed_model().predict(test_data), classifier.predict(test_data))

    def test_update_from_json_config(self, client, in_tempdir, created_entities, experiment_run, model_for_deployment):
        json = pytest.importorskip("json")

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        path = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_entities.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        # Creating config dict:
        strategy_dict = {
            "run_id": experiment_run.id,
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.05,
                "progress_interval_seconds": 30,
                "rules": [
                    {"rule": "latency_avg_max",
                     "rule_parameters": [
                         {"name": "threshold",
                          "value": "0.1"}
                    ]},
                    {"rule": "error_4xx_rate",
                     "rule_parameters": [
                        {"name": "threshold",
                         "value": "1"}
                    ]}
                ]
            }
        }

        filepath = "config.json"
        with open(filepath, 'w') as f:
            json.dump(strategy_dict, f)


        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', path, "-f", filepath],
        )
        assert not result.exception

        print(endpoint.get_update_status())
        updated_build_ids = get_build_ids(endpoint.get_status())
        assert len(updated_build_ids) - len(updated_build_ids.intersection(original_build_ids)) > 0

    def test_update_with_resources(self, client, created_entities, experiment_run, model_for_deployment):
        endpoint_name = _utils.generate_default_name()
        endpoint = client.set_endpoint(endpoint_name)
        created_entities.append(endpoint)
        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        resources = '{"cpu": 0.25, "memory": "100M"}'

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', endpoint_name, '--run-id', experiment_run.id, "-s", "direct",
             '--resources', resources],
        )
        assert not result.exception
        resources_dict = Resources._from_dict(json.loads(resources))._as_dict()  # config is `cpu`, wire is `cpu_millis`
        assert endpoint.get_update_status()['update_request']['resources'] == resources_dict

    def test_update_autoscaling(self, client, created_entities, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        path = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_entities.append(endpoint)

        autoscaling_option = '{"min_replicas": 1, "max_replicas": 4, "min_scale": 0.5, "max_scale": 2.0}'
        cpu_metric = '{"metric": "cpu_utilization", "parameters": [{"name": "target", "value": "0.5"}]}'
        memory_metric = '{"metric": "memory_utilization", "parameters": [{"name": "target", "value": "0.7"}]}'

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'update', 'endpoint', path, '--run-id', experiment_run.id, '--autoscaling', autoscaling_option,
             "--autoscaling-metric", cpu_metric, "--autoscaling-metric", memory_metric, "--strategy", "direct"],
        )
        assert not result.exception

        autoscaling_parameters = endpoint.get_update_status()["update_request"]["autoscaling"]
        autoscaling_quantities = autoscaling_parameters["quantities"]

        assert autoscaling_quantities == json.loads(autoscaling_option)

        autoscaling_metrics = autoscaling_parameters["metrics"]
        assert len(autoscaling_metrics) == 2
        for metric in autoscaling_metrics:
            assert metric["metric_id"] in [1001, 1002, 1003]

            if metric["metric_id"] == 1001:
                assert metric["parameters"][0]["name"] == "target"
                assert metric["parameters"][0]["value"] == "0.5"
            else:
                assert metric["parameters"][0]["name"] == "target"
                assert metric["parameters"][0]["value"] == "0.7"


class TestDownload:
    def test_download_context(self, experiment_run, model_for_deployment, registered_model, in_tempdir, created_entities, model_version):
        np = pytest.importorskip("numpy")
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        artifact = np.random.random((36, 12))
        experiment_run.log_artifact("some-artifact", artifact)

        download_to_path = "context_cli.tgz"

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'download', 'dockercontext', '--run-id', experiment_run.id, '--output',
             download_to_path],
        )
        assert not result.exception

        # can be loaded as tgz
        with tarfile.open(download_to_path, 'r:gz') as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths

        model_version.log_model(model_for_deployment['model'], custom_modules=[])

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'download', 'dockercontext', '--model-version-id', model_version.id,
             '--output', download_to_path],
        )

        assert not result.exception

        # can be loaded as tgz
        with tarfile.open(download_to_path, 'r:gz') as f:
            filepaths = set(f.getnames())

        assert "Dockerfile" in filepaths


class TestPredict:
    def test_predict(self, client, experiment_run, created_entities):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())

        test_data = np.random.random((4, 12))
        test_data_str = json.dumps(test_data.tolist())

        experiment_run.log_model(classifier, custom_modules=[])
        experiment_run.log_environment(Python(['scikit-learn']))

        path = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_entities.append(endpoint)
        endpoint.update(experiment_run, DirectUpdateStrategy(), wait=True)

        runner = CliRunner()
        result = runner.invoke(
            cli,
            ['deployment', 'predict', 'endpoint', path, '--data', test_data_str],
        )

        assert not result.exception
        assert json.dumps(classifier.predict(test_data).tolist()) in result.output
