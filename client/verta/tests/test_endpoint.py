import pytest

from verta._deployment import Endpoint
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update_rules import AverageLatencyThreshold

import time


def get_build_ids(status):
    # get the set of build_ids in the status of the stage:
    return set(map(lambda comp: comp["build_id"], status["components"]))


class TestEndpoint:
    def test_get_status(self, client):
        # TODO: remove hardcoding
        endpoint = Endpoint(client._conn, client._conf, "Nhat_Pham", 210119)
        status = endpoint.get_status()

        # Check that some fields exist:
        assert "status" in status
        assert "date_created" in status
        assert "id" in status

    def test_direct_update(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 210119)

        original_status = endpoint.get_status()
        original_build_ids = get_build_ids(original_status)
        endpoint.update(experiment_run, DirectUpdateStrategy())
        updated_status = endpoint.get_status()

        # Check that a new build is added:
        new_build_ids = get_build_ids(endpoint.get_status())
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

    def test_canary_update(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 210119)
        original_status = endpoint.get_status()
        original_build_ids = list(map(lambda comp: comp["build_id"], original_status["components"]))

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)
        
        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(AverageLatencyThreshold(0.8))
        endpoint.update(experiment_run, strategy)
        updated_status = endpoint.get_status()

        # Check that a new build is added:
        new_build_ids = get_build_ids(endpoint.get_status())
        assert len(new_build_ids) - len(new_build_ids.intersection(original_build_ids)) > 0

