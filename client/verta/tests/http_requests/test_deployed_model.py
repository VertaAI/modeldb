# -*- coding: utf-8 -*-

import os

import pytest
import six

from verta.environment import Python
from verta._internal_utils._utils import generate_default_name


pytestmark = pytest.mark.not_oss  # skip if run in oss setup


class TestDeployedModel:
    @pytest.mark.not_oss
    def test_ca_bundle_env_var(self, https_client, created_entities):
        """Verify predict() honors REQUESTS_CA_BUNDLE env var."""
        LogisticRegression = pytest.importorskip(
            "sklearn.linear_model",
        ).LogisticRegression

        # create entities
        registered_model = https_client.create_registered_model(generate_default_name())
        created_entities.append(registered_model)
        endpoint = https_client.create_endpoint(generate_default_name())
        created_entities.append(endpoint)

        REQUESTS_CA_BUNDLE_ENV_VAR = "REQUESTS_CA_BUNDLE"
        good_ca_bundle_path = os.environ.get(REQUESTS_CA_BUNDLE_ENV_VAR)

        # log and deploy model
        input, output = [[1], [2], [3]], [0, 1, 1]
        model = LogisticRegression().fit(input, output)
        model_version = registered_model.create_standard_model_from_sklearn(
            model,
            Python(["scikit-learn"]),
        )
        endpoint.update(model_version, wait=True)
        deployed_model = endpoint.get_deployed_model()

        # as a control, make sure request works
        assert deployed_model.predict(input)

        bad_ca_bundle_path = "foo"
        msg_match = (
            "^Could not find a suitable TLS CA certificate bundle,"
            " invalid path: {}$".format(bad_ca_bundle_path)
        )
        error_type = IOError if six.PY2 else OSError
        try:
            os.environ[REQUESTS_CA_BUNDLE_ENV_VAR] = bad_ca_bundle_path

            with pytest.raises(error_type, match=msg_match):
                deployed_model.predict(input)
        finally:
            if good_ca_bundle_path:
                os.environ[REQUESTS_CA_BUNDLE_ENV_VAR] = good_ca_bundle_path
            else:
                del os.environ[REQUESTS_CA_BUNDLE_ENV_VAR]
