# -*- coding: utf-8 -*-

import pytest

from verta.credentials import EmailCredentials, JWTCredentials
from verta.deployment import DeployedModel

from verta._internal_utils._utils import _GRPC_PREFIX


class TestDeployedModel:

    prediction_url = "https://app.verta.ai/api/v1/predict/fake-deployed-model"

    def test_authn_free_session(self, mock_env_authn_missing):
        deployed_model = DeployedModel(self.prediction_url, token=None, creds=None)
        headers = deployed_model.headers()
        assert (_GRPC_PREFIX + "source") not in headers
        assert "Access-Token" not in headers

    def test_access_token_deployment(self, mock_env_authn_missing):
        access_token = "fake-access-token"
        deployed_model = DeployedModel(self.prediction_url, token=access_token, creds=None)
        headers = deployed_model.headers()
        assert (_GRPC_PREFIX + "source") not in headers
        assert headers.get("Access-Token") == access_token

    def test_user_dev_key_deployment(self, mock_env_authn_missing):
        email_creds = EmailCredentials("fake-email", "fake-dev-key")
        deployed_model = DeployedModel(self.prediction_url, token=None, creds=email_creds)
        headers = deployed_model.headers()
        assert "Access-Token" not in headers
        assert headers.get(_GRPC_PREFIX + "source") == "PythonClient"
        assert headers.get(_GRPC_PREFIX + "email") == "fake-email"

    def test_user_jwt_deployment(self, mock_env_authn_missing):
        jwt_creds = JWTCredentials("fake-token", "fake-token-sig")
        deployed_model = DeployedModel(self.prediction_url, token=None, creds=jwt_creds)
        headers = deployed_model.headers()
        assert "Access-Token" not in headers
        assert headers.get(_GRPC_PREFIX + "source") == "JWT"
        assert headers.get(_GRPC_PREFIX + "bearer_access_token") == "fake-token"

    def test_both_authn_deployment(self, mock_env_authn_missing):
        email_creds = EmailCredentials("fake-email", "fake-dev-key")
        access_token = "fake-access-token"
        deployed_model = DeployedModel(self.prediction_url, token=access_token, creds=email_creds)
        headers = deployed_model.headers()
        assert headers.get("Access-Token") == access_token
        assert headers.get(_GRPC_PREFIX + "source") == "PythonClient"
        assert headers.get(_GRPC_PREFIX + "email") == "fake-email"

    def test_authn_load_from_env(self, mock_env_dev_key_auth):
        deployed_model = DeployedModel(self.prediction_url, token=None, creds=None)
        headers = deployed_model.headers()
        assert "Access-Token" not in headers
        assert headers.get(_GRPC_PREFIX + "source") == "PythonClient"
        assert (_GRPC_PREFIX + "email") in headers

    def test_jwt_authn_load_from_env(self, mock_env_jwt_auth):
        deployed_model = DeployedModel(self.prediction_url, token=None, creds=None)
        headers = deployed_model.headers()
        assert "Access-Token" not in headers
        assert headers.get(_GRPC_PREFIX + "source") == "JWT"
        assert (_GRPC_PREFIX + "bearer_access_token") in headers
