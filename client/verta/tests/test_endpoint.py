import pytest

from verta._deployment import Endpoint
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy
from verta.deployment.update_rules import AverageLatencyThreshold

import time


class TestEndpoint:
    def test_update(self, strs, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)

        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 210119)

        original_status = endpoint.get_status()
        endpoint.update(experiment_run, DirectUpdateStrategy())

        time.sleep(2) # wait for updating to complete
        updated_status = endpoint.get_status()

        assert original_status["date_updated"] != updated_status["date_updated"]

        strategy = CanaryUpdateStrategy(interval=1, step=0.5)
        
        with pytest.raises(RuntimeError) as excinfo:
            endpoint.update(experiment_run, strategy)

        assert "canary update strategy must have at least one rule" in str(excinfo.value)

        strategy.add_rule(AverageLatencyThreshold(0.8))
        endpoint.update(experiment_run, strategy)
        time.sleep(2) # wait for updating to complete
        updated_status_2 = endpoint.get_status()

        assert updated_status["date_updated"] != updated_status_2["date_updated"]
        assert original_status["date_updated"] != updated_status_2["date_updated"]
