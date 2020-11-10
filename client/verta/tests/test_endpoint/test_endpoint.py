import json
import time

import pytest

import requests

import yaml

import verta
from verta.endpoint._endpoint import Endpoint
from verta.endpoint.resources import Resources
from verta.endpoint.autoscaling import Autoscaling
from verta.endpoint.autoscaling.metrics import CpuUtilizationTarget, MemoryUtilizationTarget, RequestsPerWorkerTarget
from verta.endpoint.update import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.endpoint.update.rules import MaximumAverageLatencyThresholdRule
from verta._internal_utils import _utils
from verta.environment import Python
from verta.utils import ModelAPI

from ..utils import (get_build_ids, sys_path_manager)

pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestEndpoint:
    def test_create(self, client, created_endpoints):
        name = _utils.generate_default_name()
        endpoint = client.set_endpoint(name)
        assert endpoint
        created_endpoints.append(endpoint)
        name = verta._internal_utils._utils.generate_default_name()
        endpoint = client.create_endpoint(name)
        assert endpoint
        with pytest.raises(requests.HTTPError) as excinfo:
            assert client.create_endpoint(name)
        excinfo_value = str(excinfo.value).strip()
        assert "409" in excinfo_value
        assert "already in use" in excinfo_value
        with pytest.warns(UserWarning, match='.*already exists.*'):
            client.set_endpoint(path=endpoint.path, description="new description")

    def test_get(self, client, created_endpoints):
        name = _utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_endpoint(name)

        endpoint = client.set_endpoint(name)
        created_endpoints.append(endpoint)

        assert endpoint.id == client.get_endpoint(endpoint.path).id
        assert endpoint.id == client.get_endpoint(id=endpoint.id).id

    def test_get_by_name(self, client, created_endpoints):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        dummy_endpoint = client.set_endpoint(path2)  # in case get erroneously fetches latest
        created_endpoints.append(dummy_endpoint)

        assert endpoint.id == client.set_endpoint(endpoint.path).id

    def test_get_by_id(self, client, created_endpoints):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        dummy_endpoint = client.set_endpoint(path2)  # in case get erroneously fetches latest
        created_endpoints.append(dummy_endpoint)

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

    def test_list(self, client, organization, created_endpoints):
        name = _utils.generate_default_name()
        endpoint = client.set_endpoint(name, workspace=organization.name)
        created_endpoints.append(endpoint)

        endpoints = client.endpoints.with_workspace(organization.name)
        assert len(endpoints) >= 1
        has_new_id = False
        for item in endpoints:
            assert item.id
            if item.id == endpoint.id:
                has_new_id = True
        assert has_new_id

    def test_get_status(self, client, created_endpoints):
        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        status = endpoint.get_status()

        # Check that some fields exist:
        assert status["status"]
        assert status["date_created"]
        assert status["stage_id"]

    def test_repr(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        str_repr = repr(endpoint)

        assert "path: {}".format(endpoint.path) in str_repr
        assert "url" in str_repr
        assert "id: {}".format(endpoint.id) in str_repr
        assert "curl: <Endpoint not deployed>" in str_repr

        # these fields might have changed:
        assert "status" in str_repr
        assert "date created" in str_repr
        assert "date updated" in str_repr
        assert "stage's date created" in str_repr
        assert "stage's date updated" in str_repr
        assert "components" in str_repr

        endpoint.update(experiment_run, DirectUpdateStrategy(), True)
        str_repr = repr(endpoint)
        assert "curl: {}".format(endpoint.get_deployed_model().get_curl()) in str_repr

    def test_download_manifest(self, client, in_tempdir):
        download_to_path = "manifest.yaml"
        path = verta._internal_utils._utils.generate_default_name()
        name = verta._internal_utils._utils.generate_default_name()

        strategy = CanaryUpdateStrategy(interval=10, step=0.1)
        strategy.add_rule(MaximumAverageLatencyThresholdRule(0.1))
        resources = Resources(cpu=.1, memory="128Mi")
        autoscaling = Autoscaling(min_replicas=1, max_replicas=10, min_scale=0.1, max_scale=2)
        autoscaling.add_metric(CpuUtilizationTarget(0.75))
        env_vars = {'env1': "var1", 'env2': "var2"}

        filepath = client.download_endpoint_manifest(
            download_to_path=download_to_path,
            path=path,
            name=name,
            strategy=strategy,
            autoscaling=autoscaling,
            resources=resources,
            env_vars=env_vars,
        )

        # can be loaded as YAML
        with open(filepath, 'rb') as f:
            manifest = yaml.safe_load(f)

        assert manifest['kind'] == "Endpoint"

        # check environment variables
        containers = manifest['spec']['function']['spec']['templates']['deployment']['spec']['template']['spec']['containers']
        retrieved_env_vars = {
            env_var['name']: env_var['value']
            for env_var
            in containers[0]['env']
        }
        assert retrieved_env_vars == env_vars

    def test_direct_update(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)
        updated_status = endpoint.update(experiment_run, DirectUpdateStrategy())

        # Check that a new build is added:
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_update_wait(self, client, created_endpoints, experiment_run, model_version, model_for_deployment):
        """This tests endpoint.update(..., wait=True), including the case of build error"""
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        status = endpoint.update(experiment_run, DirectUpdateStrategy(), True)

        assert status["status"] == "active"

        model_version.log_model(model_for_deployment['model'], custom_modules=[])
        model_version.log_environment(Python(requirements=['blahblahblah==3.6.0']))

        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(model_version, DirectUpdateStrategy(), True) # this should fail, and not take forever!

        excinfo_value = str(excinfo.value).strip()
        assert "Could not find a version that satisfies the requirement blahblahblah==3.6.0" in excinfo_value

    def test_canary_update(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)

        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(MaximumAverageLatencyThresholdRule(0.8))
        updated_status = endpoint.update(experiment_run, strategy)

        # Check that a new build is added:
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_update_from_json_config(self, client, in_tempdir, created_endpoints, experiment_run, model_for_deployment):
        json = pytest.importorskip("json")
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        # Creating config dict:
        strategy_dict = {
            "run_id": experiment_run.id,
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.5,
                "progress_interval_seconds": 1,
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

        updated_status = endpoint.update_from_config(filepath)
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_update_from_yaml_config(self, client, in_tempdir, created_endpoints, experiment_run, model_for_deployment):
        yaml = pytest.importorskip("yaml")
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        # Creating config dict:
        strategy_dict = {
            "run_id": experiment_run.id,
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.5,
                "progress_interval_seconds": 1,
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

        filepath = "config.yaml"
        with open(filepath, 'w') as f:
            yaml.safe_dump(strategy_dict, f)

        updated_status = endpoint.update_from_config(filepath)
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_update_with_parameters(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)

        strategy.add_rule(MaximumAverageLatencyThresholdRule(0.8))
        updated_status = endpoint.update(experiment_run, strategy, resources = Resources(cpu=.25, memory="512Mi"),
                                         env_vars = {'CUDA_VISIBLE_DEVICES': "1,2", "VERTA_HOST": "app.verta.ai"})

        # Check that a new build is added:
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_get_access_token(self, client, created_endpoints):
        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        token = endpoint.get_access_token()

        assert token is None

        token = verta._internal_utils._utils.generate_default_name()
        endpoint.create_access_token(token)
        assert endpoint.get_access_token() == token

    def test_create_update_body(self):
        endpoint = Endpoint(None, None, None, None)
        resources = Resources(cpu=.25, memory="512Mi")

        env_vars = {'CUDA_VISIBLE_DEVICES': "1,2", "VERTA_HOST": "app.verta.ai", "GIT_TERMINAL_PROMPT" : "1"}

        parameter_json = endpoint._create_update_body(DirectUpdateStrategy(), resources, None, env_vars)
        assert parameter_json == {
            'env': [
                {"name": 'CUDA_VISIBLE_DEVICES', 'value': '1,2'},
                {'name': 'GIT_TERMINAL_PROMPT', 'value': '1'},
                {"name": 'VERTA_HOST', 'value': 'app.verta.ai'}
            ],
            'resources': {'cpu_millis': 250, 'memory': '512Mi'},
            'strategy': 'rollout',
        }


    def test_get_deployed_model(self, client, experiment_run, model_version, model_for_deployment, created_endpoints):
        """
        Verifies prediction for a finished deployment, as well as for an endpoint in the middle of being updated.
        """
        np = pytest.importorskip("numpy")

        model = model_for_deployment['model'].fit(
            model_for_deployment['train_features'],
            model_for_deployment['train_targets'],
        )
        experiment_run.log_model(model, custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        endpoint.update(experiment_run, DirectUpdateStrategy(), wait=True)

        token = verta._internal_utils._utils.generate_default_name()
        endpoint.create_access_token(token)
        x = model_for_deployment['train_features'].iloc[1].values
        deployed_model = endpoint.get_deployed_model()

        assert np.allclose(deployed_model.predict([x]), model.predict([x]))
        deployed_model_curl = deployed_model.get_curl()
        assert endpoint.path in deployed_model_curl
        assert "-H \"Access-token: {}\"".format(token) in deployed_model_curl

        new_model = model_for_deployment['model'].fit(
            np.random.random(model_for_deployment['train_features'].shape),
            np.random.random(model_for_deployment['train_targets'].shape).round()
        )
        model_version.log_model(new_model)
        model_version.log_environment(Python(requirements=["scikit-learn"]))

        endpoint.update(model_version, DirectUpdateStrategy(), wait=False)
        endpoint.get_deployed_model().predict([x])  # should succeed, because the endpoint can still service requests

    def test_update_from_model_version(self, client, model_version, created_endpoints):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        endpoint.update(model_version, DirectUpdateStrategy(), wait=True)
        test_data = np.random.random((4, 12))
        assert np.array_equal(endpoint.get_deployed_model().predict(test_data), classifier.predict(test_data))

    def test_update_from_json_config_model_version(self, client, in_tempdir, created_endpoints, model_version):
        np = pytest.importorskip("numpy")
        json = pytest.importorskip("json")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier, custom_modules=[])

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        # Creating config dict:
        strategy_dict = {
            "model_version_id": model_version.id,
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.5,
                "progress_interval_seconds": 1,
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
        with open(filepath, "w") as f:
            json.dump(strategy_dict, f)

        endpoint.update_from_config(filepath, wait=True)

        test_data = np.random.random((4, 12))
        prediction = endpoint.get_deployed_model().predict(test_data)
        assert np.array_equal(prediction, classifier.predict(test_data))

    def test_update_autoscaling(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        autoscaling = Autoscaling(min_replicas=1, max_replicas=2, min_scale=0.5, max_scale=2.0)
        autoscaling.add_metric(CpuUtilizationTarget(0.5))
        autoscaling.add_metric(MemoryUtilizationTarget(0.7))
        autoscaling.add_metric(RequestsPerWorkerTarget(100))

        endpoint.update(experiment_run, DirectUpdateStrategy(), autoscaling=autoscaling)
        update_status = endpoint.get_update_status()

        autoscaling_metrics = update_status["update_request"]["autoscaling"]["metrics"]
        assert len(autoscaling_metrics) == 3
        for metric in autoscaling_metrics:
            assert metric["metric_id"] in [1001, 1002, 1003]

            if metric["metric_id"] == 1001:
                assert metric["parameters"][0]["value"] == "0.5"
            elif metric["metric_id"] == 1002:
                assert metric["parameters"][0]["value"] == "100"
            else:
                assert metric["parameters"][0]["value"] == "0.7"

    def test_update_with_custom_module(self, client, model_version, created_endpoints):
        torch = pytest.importorskip("torch")

        with sys_path_manager() as sys_path:
            sys_path.append(".")

            from models.nets import FullyConnected  # pylint: disable=import-error

            train_data = torch.rand((2, 4))

            classifier = FullyConnected(num_features=4, hidden_size=32, dropout=0.2)
            model_api = ModelAPI(train_data.tolist(), classifier(train_data).tolist())
            model_version.log_model(classifier, custom_modules=["models/"], model_api=model_api)

            env = Python(requirements=["torch={}".format(torch.__version__)])
            model_version.log_environment(env)


            path = verta._internal_utils._utils.generate_default_name()
            endpoint = client.set_endpoint(path)
            created_endpoints.append(endpoint)
            endpoint.update(model_version, DirectUpdateStrategy(), wait=True)


            test_data = torch.rand((4, 4))
            prediction = torch.tensor(endpoint.get_deployed_model().predict(test_data.tolist()))
            assert torch.all(classifier(test_data).eq(prediction))

    def test_update_from_json_config_with_params(self, client, in_tempdir, created_endpoints, experiment_run, model_for_deployment):
        yaml = pytest.importorskip("yaml")
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])


        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)


        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        # Creating config dict:
        config_dict = {
            "run_id": experiment_run.id,
            "strategy": "direct",
            "autoscaling": {
                "quantities": {"min_replicas": 1, "max_replicas": 4, "min_scale": 0.5, "max_scale": 2.0},
                "metrics": [
                    {"metric": "cpu_utilization", "parameters": [{"name": "target", "value": "0.5"}]},
                    {"metric": "memory_utilization", "parameters": [{"name": "target", "value": "0.7"}]}
                ]
            },
            "env_vars": {"VERTA_HOST": "app.verta.ai"},
            "resources": {"cpu": 0.25, "memory": "100M"}
        }

        filepath = "config.json"
        with open(filepath, 'w') as f:
            json.dump(config_dict, f)

        endpoint.update_from_config(filepath)
        update_status = endpoint.get_update_status()

        # Check autoscaling:
        autoscaling_parameters = update_status["update_request"]["autoscaling"]
        autoscaling_quantities = autoscaling_parameters["quantities"]

        assert autoscaling_quantities == config_dict["autoscaling"]["quantities"]

        autoscaling_metrics = autoscaling_parameters["metrics"]
        assert len(autoscaling_metrics) == 2
        for metric in autoscaling_metrics:
            assert metric["metric_id"] in [1001, 1002, 1003]

            if metric["metric_id"] == 1001:
                assert metric["parameters"][0]["value"] == "0.5"
            else:
                assert metric["parameters"][0]["value"] == "0.7"

        # Check env_vars:
        assert update_status["update_request"]["env"][0]["name"] == "VERTA_HOST"
        assert update_status["update_request"]["env"][0]["value"] == "app.verta.ai"

        # Check resources:
        resources_dict = Resources._from_dict(config_dict["resources"])._as_dict()  # config is `cpu`, wire is `cpu_millis`
        assert endpoint.get_update_status()['update_request']['resources'] == resources_dict

    def test_update_twice(self, client, registered_model, created_endpoints):
        np = pytest.importorskip("numpy")
        json = pytest.importorskip("json")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        env = Python(requirements=["scikit-learn"])

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version = registered_model.create_version("first-version")
        model_version.log_model(classifier)
        model_version.log_environment(env)

        new_classifier = LogisticRegression()
        new_classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        new_model_version = registered_model.create_version("second-version")
        new_model_version.log_model(new_classifier)
        new_model_version.log_environment(env)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        endpoint.update(model_version, wait=True)

        # updating endpoint
        endpoint.update(new_model_version, DirectUpdateStrategy(), wait=True)
        test_data = np.random.random((4, 12))
        assert np.array_equal(endpoint.get_deployed_model().predict(test_data), new_classifier.predict(test_data))

    def test_update_from_run_diff_workspace(self, client, organization, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.create_endpoint(path, workspace=organization.name)
        created_endpoints.append(endpoint)

        endpoint.update(experiment_run, DirectUpdateStrategy(), wait=True)
        assert endpoint.workspace != experiment_run.workspace

    def test_update_from_version_diff_workspace(self, client, model_version, organization, created_endpoints):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.create_endpoint(path, workspace=organization.name)
        created_endpoints.append(endpoint)

        endpoint.update(model_version, DirectUpdateStrategy(), wait=True)
        assert endpoint.workspace != model_version.workspace

    def test_update_from_run_diff_workspace_no_access_error(self, client_2, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client_2.create_endpoint(path)
        created_endpoints.append(endpoint)

        with pytest.raises(requests.HTTPError) as excinfo:
            endpoint.update(experiment_run, DirectUpdateStrategy(), wait=True)

        excinfo_value = str(excinfo.value).strip()
        assert "403" in excinfo_value
        assert "Access Denied" in excinfo_value

    def test_update_from_version_diff_workspace_no_access_error(self, client_2, model_version, created_endpoints):
        np = pytest.importorskip("numpy")
        sklearn = pytest.importorskip("sklearn")
        from sklearn.linear_model import LogisticRegression

        classifier = LogisticRegression()
        classifier.fit(np.random.random((36, 12)), np.random.random(36).round())
        model_version.log_model(classifier)

        env = Python(requirements=["scikit-learn"])
        model_version.log_environment(env)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client_2.create_endpoint(path)
        created_endpoints.append(endpoint)

        with pytest.raises(requests.HTTPError) as excinfo:
            endpoint.update(model_version, DirectUpdateStrategy(), wait=True)

        excinfo_value = str(excinfo.value).strip()
        assert "403" in excinfo_value
        assert "Access Denied" in excinfo_value
