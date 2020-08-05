import time

import pytest
import requests

import verta
from verta._deployment import Endpoint
from verta.deployment.resources import CpuMillis, Memory
from verta.deployment.autoscaling import Autoscaling
from verta.deployment.autoscaling.metrics import CpuTarget, MemoryTarget, RequestsPerWorkerTarget
from verta.deployment.update import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update.rules import AverageLatencyThresholdRule
from verta._internal_utils import _utils
from verta.environment import Python

from ..utils import get_build_ids


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

    def test_list(self, client, created_endpoints):
        name = _utils.generate_default_name()
        endpoint = client.set_endpoint(name)
        created_endpoints.append(endpoint)

        endpoints = client.endpoints
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
        assert "id: {}".format(endpoint.id) in str_repr

        # these fields might have changed:
        assert "status" in str_repr
        assert "date created" in str_repr
        assert "date updated" in str_repr
        assert "stage's date created" in str_repr
        assert "stage's date updated" in str_repr
        assert "components" in str_repr

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

    def test_update_wait(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        status = endpoint.update(experiment_run, DirectUpdateStrategy(), True)

        assert status["status"] == "active"

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

        strategy.add_rule(AverageLatencyThresholdRule(0.8))
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
                "progress_step": 0.05,
                "progress_interval_seconds": 30,
                "rules": [
                    {"rule": "latency",
                     "rule_parameters": [
                         {"name": "latency_avg",
                          "value": "0.1"}
                    ]},
                    {"rule": "error_rate",
                     "rule_parameters": [
                        {"name": "error_rate",
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
                "progress_step": 0.05,
                "progress_interval_seconds": 30,
                "rules": [
                    {"rule": "latency",
                     "rule_parameters": [
                         {"name": "latency_avg",
                          "value": "0.1"}
                    ]},
                    {"rule": "error_rate",
                     "rule_parameters": [
                        {"name": "error_rate",
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

        strategy.add_rule(AverageLatencyThresholdRule(0.8))
        updated_status = endpoint.update(experiment_run, strategy, resources = [ CpuMillis(500), Memory("500Mi"), ],
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

    def test_form_update_body(self):
        endpoint = Endpoint(None, None, None, None)
        resources = [
            CpuMillis(500),
            Memory("500Mi"),
        ]

        env_vars = {'CUDA_VISIBLE_DEVICES': "1,2", "VERTA_HOST": "app.verta.ai"}

        parameter_json = endpoint._form_update_body(resources, DirectUpdateStrategy(), None, env_vars, 0)
        assert parameter_json == {'build_id': 0, 'env': [{"name":'CUDA_VISIBLE_DEVICES', 'value':'1,2'},
                                                         {"name":'VERTA_HOST', 'value':'app.verta.ai'}],
                                  'resources': {'cpu_millis': 500, 'memory': '500Mi'}, 'strategy': 'rollout'}


    def test_get_deployed_model(self, client, experiment_run, model_for_deployment, created_endpoints):
        model = model_for_deployment['model'].fit(
            model_for_deployment['train_features'],
            model_for_deployment['train_targets'],
        )
        experiment_run.log_model(model, custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)
        endpoint.update(experiment_run, DirectUpdateStrategy())

        while not endpoint.get_status()['status'] == "active":
            time.sleep(3)
        x = model_for_deployment['train_features'].iloc[1].values
        assert endpoint.get_deployed_model().predict([x]) == [2]

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
        model_version.log_model(classifier)

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
                "progress_step": 0.05,
                "progress_interval_seconds": 30,
                "rules": [
                    {"rule": "latency",
                     "rule_parameters": [
                         {"name": "latency_avg",
                          "value": "0.1"}
                    ]},
                    {"rule": "error_rate",
                     "rule_parameters": [
                        {"name": "error_rate",
                         "value": "1"}
                    ]}
                ]
            }
        }

        filepath = "config.json"
        with open(filepath, "wb") as f:
            json.dump(strategy_dict, f)

        endpoint.update_from_config(filepath)

        while not endpoint.get_status()['status'] == "active":
            time.sleep(3)

        test_data = np.random.random((4, 12))
        assert np.array_equal(endpoint.get_deployed_model().predict(test_data), classifier.predict(test_data))

    def test_update_autoscaling(self, client, created_endpoints, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        created_endpoints.append(endpoint)

        autoscaling = Autoscaling(min_replicas=0, max_replicas=2, min_scale=0.5, max_scale=2.0)
        autoscaling.add_metric(CpuTarget(0.5))
        autoscaling.add_metric(MemoryTarget(0.7))
        autoscaling.add_metric(RequestsPerWorkerTarget(100))

        endpoint.update(experiment_run, DirectUpdateStrategy(), autoscaling=autoscaling)
        update_status = endpoint.get_update_status()
        
        autoscaling_metrics = update_status["update_request"]["autoscaling"]["metrics"]
        assert len(autoscaling_metrics) == 3
        for metric in autoscaling_metrics:
            assert metric["metric_id"] in [1001, 1002, 1003]

            if metric["metric_id"] == 1001:
                assert metric["parameters"][0]["name"] == "cpu_target"
                assert metric["parameters"][0]["value"] == "0.5"
            elif metric["metric_id"] == 1002:
                assert metric["parameters"][0]["name"] == "requests_per_worker_target"
                assert metric["parameters"][0]["value"] == "100"
            else:
                assert metric["parameters"][0]["name"] == "memory_target"
                assert metric["parameters"][0]["value"] == "0.7"
