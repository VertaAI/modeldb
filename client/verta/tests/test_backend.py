import six

import multiprocessing

import pytest


class TestLoad:
    @staticmethod
    def run_fake_experiment(args):
        client, floats = args
        floats = iter(floats)
        run = client.set_experiment_run()

        run.log_attribute("is_test", True)
        run.get_attribute("is_test")

        run.log_hyperparameters({
            'C': next(floats),
            'solver': next(floats),
            'max_iter': next(floats),
        })
        run.get_hyperparameter("C")
        run.get_hyperparameter("solver")
        run.get_hyperparameter("max_iter")

        run.log_observation("rand_val", next(floats))
        run.log_observation("rand_val", next(floats))
        run.log_observation("rand_val", next(floats))
        run.get_observation("rand_val")

        run.log_metric("val_acc", next(floats))
        run.get_metric("val_acc")

        run.log_artifact("self", run)
        run.get_artifact("self")

    @pytest.mark.skipif(six.PY2, reason="multiprocessing.Pool has issues in Python 2")
    def test_load(self, client, floats):
        client.set_project()
        client.set_experiment()
        pool = multiprocessing.Pool(36)
        pool.map(self.run_fake_experiment, [(client, floats)]*144)
        pool.close()
