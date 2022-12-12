# -*- coding: utf-8 -*-

import pytest
import requests

from verta.environment import Python


pytestmark = pytest.mark.not_oss  # skip if run in oss setup. Applied to entire module


class TestFromRun:
    def test_from_run(self, experiment_run, model_for_deployment, registered_model):
        np = pytest.importorskip("numpy")

        experiment_run.log_model(model_for_deployment["model"], custom_modules=[])
        experiment_run.log_environment(Python(["scikit-learn"]))

        artifact = np.random.random((36, 12))
        experiment_run.log_artifact("some-artifact", artifact)

        for i, run_id_arg in enumerate(
            [experiment_run.id, experiment_run]
        ):  # also accept run obj
            model_version = registered_model.create_version_from_run(
                run_id=run_id_arg,
                name="From Run {} {}".format(experiment_run.id, i),
            )

            env_str = str(model_version.get_environment())
            assert "scikit-learn" in env_str
            assert "Python" in env_str

            assert (
                model_for_deployment["model"].get_params()
                == model_version.get_model().get_params()
            )
            assert np.array_equal(model_version.get_artifact("some-artifact"), artifact)

# TODO: test_from_run_diff_workspaces with separate workspace