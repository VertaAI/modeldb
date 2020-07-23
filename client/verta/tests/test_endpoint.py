from verta._deployment import Endpoint
from verta.deployment.strategies import DirectUpdateStrategy, CanaryUpdateStrategy

import time


class TestEndpoint:
    def test_update(self, experiment_run, model_for_deployment, experiment_run_2, model_for_deployment_2):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 449)

        original_status = endpoint.get_status()
        endpoint.update(experiment_run, DirectUpdateStrategy())

        time.sleep(2) # wait for updating to complete
        updated_status = endpoint.get_status()

        assert original_status["date_updated"] != updated_status["date_updated"]

        experiment_run_2.log_model_for_deployment(**model_for_deployment_2)
        endpoint.update(experiment_run_2, CanaryUpdateStrategy(interval=1, step=0.5))
