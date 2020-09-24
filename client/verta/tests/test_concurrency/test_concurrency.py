import six
import multiprocessing
import pytest
from .run_functions import log_observation, upload_artifact

from operator import itemgetter
from functools import partial


class TestConcurrency:
    @pytest.mark.skipif(six.PY2, reason="multiprocessing.Pool has issues in Python 2")
    def test_multiple_runs_log_obs(self, client):
        client.set_project()
        client.set_experiment()

        with multiprocessing.Pool(36) as pool:
            result = pool.map(partial(log_observation, client), range(10))

            def extract_obs_value(obs):
                return list(map(itemgetter(0), obs))

            assert all(map(lambda res: extract_obs_value((res["run"].get_observation("obs"))) == res["obs"], result))

    @pytest.mark.skipif(six.PY2, reason="multiprocessing.Pool has issues in Python 2")
    def test_multiple_runs_upload_artifacts(self, client, in_tempdir):
        client.set_project()
        client.set_experiment()

        with multiprocessing.Pool(36) as pool:
            result = pool.map(partial(upload_artifact, client), range(5))

            assert all(map(lambda res: res["run"].get_artifact("artifact").read() == res["artifact"], result))

