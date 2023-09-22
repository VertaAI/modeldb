# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st
import responses
from responses.matchers import json_params_matcher


@hypothesis.given(name=st.text())
def test_rename(make_mock_experiment_run, name):
    run = make_mock_experiment_run()

    with responses.RequestsMock() as rsps:
        rsps.post(
            f"{run._conn.scheme}://{run._conn.socket}/api/v1/modeldb/experiment-run/updateExperimentRunName",
            match=[json_params_matcher({"id": run.id, "name": name})],
            status=200,
        )

        run.rename(name)
