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

    def test_repr(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)
        status = endpoint.update(experiment_run, DirectUpdateStrategy())

        str_repr = str(endpoint)

        assert "path: {}".format(endpoint.path) in str_repr
        assert "id: {}".format(endpoint.id) in str_repr

        # these fields might have changed:
        assert "status" in str_repr
        assert "date created" in str_repr
        assert "date updated" in str_repr
        assert "components" in str_repr

    def test_direct_update(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)
        updated_status = endpoint.update(experiment_run, DirectUpdateStrategy())

        # Check that a new build is added:
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_update_wait(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        status = endpoint.update(experiment_run, DirectUpdateStrategy(), True)

        assert status["status"] == "active"

    def test_canary_update(self, client, experiment_run, model_for_deployment):
        experiment_run.log_model(model_for_deployment['model'], custom_modules=[])
        experiment_run.log_requirements(['scikit-learn'])

        path = verta._internal_utils._utils.generate_default_name()
        endpoint = client.set_endpoint(path)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)
        
        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(AverageLatencyThreshold(0.8))
        updated_status = endpoint.update(experiment_run, strategy)

        # Check that a new build is added:
        new_build_ids = get_build_ids(updated_status)
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0
