import pytest

import verta
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update_rules import AverageLatencyThreshold


def get_build_ids(status):
    # get the set of build_ids in the status of the stage:
    return set(map(lambda comp: comp["build_id"], status["components"]))

@pytest.mark.skip("functionality not completed yet")
class TestEndpoint:
    def test_create(self, client):
        name = verta._internal_utils._utils.generate_default_name()
        assert client.set_endpoint(name)

    def test_get_by_id(self, client):
        endpoint = client.set_endpoint("/path1")

        client.set_endpoint("/path2")  # in case get erroneously fetches latest

        assert endpoint.id == client.set_endpoint(id=endpoint.id).id

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
