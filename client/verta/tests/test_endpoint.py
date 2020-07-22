from verta._deployment import Endpoint
from verta.deployment import DirectUpdateStrategy


class TestEndpoint:
    def test_update(self, experiment_run, model_for_deployment):
        experiment_run.log_model_for_deployment(**model_for_deployment)
        # TODO: remove hardcoding
        endpoint = Endpoint(experiment_run._conn, experiment_run._conf, "Nhat_Pham", 449)
        endpoint._path = "/string"

        endpoint.update(experiment_run, DirectUpdateStrategy)

