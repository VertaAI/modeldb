import pytest

from verta._deployment import Endpoint
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update_rules import AverageLatencyThreshold

import time


class TestEndpoint:
    def test_get_status(self, client):
        # TODO: remove hardcoding
        endpoint = Endpoint(client._conn, client._conf, "Nhat_Pham", 210119)
        status = endpoint.get_status()

        # Check that some fields exist:
        assert "status" in status
        assert "date_created" in status
        assert "id" in status

    def test_update(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 210119)

        original_status = endpoint.get_status()
        endpoint.update(experiment_run, DirectUpdateStrategy())

    def test_canary_update(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 210119)

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)
        
        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(AverageLatencyThreshold(0.8))
        endpoint.update(experiment_run, strategy)
        time.sleep(2) # wait for updating to complete
        updated_status = endpoint.get_status()
