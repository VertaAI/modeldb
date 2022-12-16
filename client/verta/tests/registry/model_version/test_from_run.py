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

    def test_from_run_diff_workspaces(
        self, client, experiment_run, organization, created_entities
    ):
        registered_model = client.create_registered_model(workspace=organization.name)
        created_entities.append(registered_model)

        model_version = registered_model.create_version_from_run(
            run_id=experiment_run.id, name="From Run {}".format(experiment_run.id)
        )

        assert model_version.workspace != experiment_run.workspace

    def test_from_run_diff_workspaces_no_access_error(
        self, experiment_run, client_2, created_entities
    ):
        registered_model = client_2.create_registered_model()
        created_entities.append(registered_model)

        with pytest.raises(requests.HTTPError) as excinfo:
            registered_model.create_version_from_run(
                run_id=experiment_run.id, name="From Run {}".format(experiment_run.id)
            )

        exc_msg = str(excinfo.value).strip()
        assert exc_msg.startswith("404")
        assert "not found" in exc_msg
