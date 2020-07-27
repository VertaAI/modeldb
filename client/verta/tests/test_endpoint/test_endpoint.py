import time

import pytest

import verta
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update_rules import AverageLatencyThreshold
from verta._internal_utils import _utils


def get_build_ids(status):
    # get the set of build_ids in the status of the stage:
    return set(map(lambda comp: comp["build_id"], status["components"]))

class TestEndpoint:
    def test_create(self, client):
        name = _utils.generate_default_name()
        assert client.set_endpoint(name)

    def test_get(self, client):
        name = _utils.generate_default_name()

        with pytest.raises(ValueError):
            client.get_endpoint(name)

        endpoint = client.set_endpoint(name)

        assert endpoint.id == client.get_endpoint(endpoint.path).id
        assert endpoint.id == client.get_endpoint(id=endpoint.id).id

    def test_get_by_name(self, client):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(endpoint.path).id
        
    def test_get_by_id(self, client):
        path = _utils.generate_default_name()
        path2 = _utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        client.set_endpoint(path2)  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

    def test_list(self, client):
        name = _utils.generate_default_name()
        endpoint = client.set_endpoint(name)

        endpoints = client.endpoints()
        assert len(endpoints) >= 1
        has_new_id = False
        for item in endpoints:
            assert item.id
            if item.id == endpoint.id:
                has_new_id = True
        assert has_new_id


    def test_get_status(self, client):
        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        status = endpoint.get_status()

        # Check that some fields exist:
        assert "status" in status
        assert "date_created" in status
        assert "id" in status

    def test_direct_update(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)
        endpoint.update(experiment_run, DirectUpdateStrategy())

        # Check that a new build is added:
        new_build_ids = get_build_ids(endpoint.get_status())
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_canary_update(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)
        
        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(AverageLatencyThreshold(0.8))
        endpoint.update(experiment_run, strategy)

        # Check that a new build is added:
        new_build_ids = get_build_ids(endpoint.get_status())
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_get_access_token(self, client):
        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        token = endpoint.get_access_token()

        assert token is None

    def test_get_deployed_model(self, client, experiment_run, model_for_deployment):
        model = model_for_deployment['model'].fit(
            model_for_deployment['train_features'],
            model_for_deployment['train_targets'],
        )
        experiment_run.log_model_for_deployment(model)

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        endpoint.update(experiment_run, DirectUpdateStrategy())

        while not endpoint.get_status()['status'] == "active":
            time.sleep(3)
        x = model_for_deployment['train_features'].iloc[1].values
        endpoint.get_deployed_model().predict([x])

