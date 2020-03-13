import requests

import pytest


class TestGetChildren:
    def test_get_experiments_in_project(self, client):
        expt_ids = []

        proj = client.set_project()
        for _ in range(3):
            expt_ids.append(client.set_experiment().id)

        response = requests.get("http://{}/api/v1/modeldb/experiment/getExperimentsInProject".format(client._conn.socket),
                                params={'project_id': proj.id}, headers=client._conn.auth)
        response.raise_for_status()
        assert set(expt_ids) == set(experiment['id'] for experiment in response.json()['experiments'])

    def test_get_experiment_runs_in_project(self, client):
        run_ids = []

        proj = client.set_project()
        expt = client.set_experiment()
        for _ in range(3):
            run_ids.append(client.set_experiment_run().id)
        expt = client.set_experiment()
        for _ in range(3):
            run_ids.append(client.set_experiment_run().id)

        response = requests.get("http://{}/api/v1/modeldb/experiment-run/getExperimentRunsInProject".format(client._conn.socket),
                                params={'project_id': proj.id}, headers=client._conn.auth)
        response.raise_for_status()
        assert set(run_ids) == set(experiment_run['id'] for experiment_run in response.json()['experiment_runs'])

    def test_get_experiment_runs_in_experiment(self, client):
        run_ids = []

        proj = client.set_project()
        expt = client.set_experiment()
        for _ in range(3):
            run_ids.append(client.set_experiment_run().id)

        response = requests.get("http://{}/api/v1/modeldb/experiment-run/getExperimentRunsInExperiment".format(client._conn.socket),
                                params={'experiment_id': expt.id}, headers=client._conn.auth)
        response.raise_for_status()
        assert set(run_ids) == set(experiment_run['id'] for experiment_run in response.json()['experiment_runs'])
