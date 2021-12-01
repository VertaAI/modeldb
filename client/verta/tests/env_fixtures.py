# -*- coding: utf-8 -*-

import pytest

from verta.credentials import EmailCredentials, JWTCredentials


@pytest.fixture
def mock_env_dev_key_auth(monkeypatch):
    monkeypatch.setenv(EmailCredentials.EMAIL_ENV, "email-env")
    monkeypatch.setenv(EmailCredentials.DEV_KEY_ENV, "dev-key-env")
    monkeypatch.delenv(JWTCredentials.JWT_TOKEN_ENV, raising=False)
    monkeypatch.delenv(JWTCredentials.JWT_TOKEN_SIG_ENV, raising=False)


@pytest.fixture
def mock_env_jwt_auth(monkeypatch):
    monkeypatch.delenv(EmailCredentials.EMAIL_ENV, raising=False)
    monkeypatch.delenv(EmailCredentials.DEV_KEY_ENV, raising=False)
    monkeypatch.setenv(JWTCredentials.JWT_TOKEN_ENV, "token-env")
    monkeypatch.setenv(JWTCredentials.JWT_TOKEN_SIG_ENV, "token-sig-env")


@pytest.fixture
def mock_env_authn_missing(monkeypatch):
    monkeypatch.delenv(EmailCredentials.EMAIL_ENV, raising=False)
    monkeypatch.delenv(EmailCredentials.DEV_KEY_ENV, raising=False)
    monkeypatch.delenv(JWTCredentials.JWT_TOKEN_ENV, raising=False)
    monkeypatch.delenv(JWTCredentials.JWT_TOKEN_SIG_ENV, raising=False)
