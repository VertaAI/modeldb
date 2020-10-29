import six
import multiprocessing
import pytest
from .run_functions import log_observation, upload_artifact

from operator import itemgetter
from functools import partial


class TestConcurrency:
    def test_multiple_runs_log_obs(self, client):
        client.set_project()
        client.set_experiment()

        pool = multiprocessing.Pool(36)
        result = pool.map(partial(log_observation, client), range(10))
        pool.close()

        def extract_obs_value(obs):
            return list(map(itemgetter(0), obs))

        check_results = list(map(lambda res: extract_obs_value((res["run"].get_observation("obs"))) == res["obs"], result))
        assert all(check_results)

    def test_multiple_runs_upload_artifacts(self, client, in_tempdir):
        client.set_project()
        client.set_experiment()

        pool = multiprocessing.Pool(36)
        result = pool.map(partial(upload_artifact, client), range(5))
        pool.close()

        check_results = list(map(lambda res: res["run"].get_artifact("artifact").read() == res["artifact"], result))
        assert all(check_results)
