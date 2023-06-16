# -*- coding: utf-8 -*-
"""Classes and functions related to credentials used with the Verta Platform.

.. versionadded:: 0.20.0

"""

import abc
import os
import re

from verta._vendored import six


@six.add_metaclass(abc.ABCMeta)
class Credentials(object):
    """Abstract base class for credentials usable with the Verta platform.

    Users of this library should not extend or use this class directly but
    instead use one of its subclasses.
    """

    @abc.abstractmethod
    def export_env_vars_to_os(self):
        """Write credential data to appropriate os environment variables."""
        raise NotImplementedError

    @abc.abstractmethod
    def headers(self):
        """Return headers carrying the information of these credentials.

        Returns
        -------
        dict
            Dictionary of headers carrying credential information.
        """
        raise NotImplementedError


class EmailCredentials(Credentials):
    """Container class for email and dev key credentials for the Verta Platform.

    Attributes
    ----------
    email : str
        A user email address.
    dev_key : str
        A dev key to use for authentication.
    organization_id : str, optional
        An organization ID to use for authentication.
    """

    EMAIL_ENV = "VERTA_EMAIL"
    DEV_KEY_ENV = "VERTA_DEV_KEY"

    def __init__(self, email, dev_key, organization_id=None):
        self.email = email
        self.dev_key = dev_key
        self.organization_id = organization_id

    def export_env_vars_to_os(self):
        os.environ[self.EMAIL_ENV] = self.email
        os.environ[self.DEV_KEY_ENV] = self.dev_key

    def headers(self):
        headers = {
            "source": "PythonClient",
            "email": self.email,
            "developer_key": self.dev_key,
            # without underscore, for NGINX support
            # https://www.nginx.com/resources/wiki/start/topics/tutorials/config_pitfalls#missing-disappearing-http-headers
            "developer-key": self.dev_key,
        }
        if self.organization_id:
            headers["organization-id"] = self.organization_id
        return headers

    def __repr__(self):
        key = self.dev_key[:8] + re.sub(r"[^-]", "*", self.dev_key[8:])
        return "EmailCredentials({}, {}, {})".format(
            self.email,
            key,
            self.organization_id,
        )

    @classmethod
    def load_from_os_env(cls):
        email_env = os.environ.get(cls.EMAIL_ENV)
        dev_key_env = os.environ.get(cls.DEV_KEY_ENV)
        if email_env and dev_key_env:
            return cls(email_env, dev_key_env)
        else:
            return None


class JWTCredentials(Credentials):
    """Container class for JWT token credentials for the Verta Platform.

    Attributes
    ----------
    jwt_token : str
        A jwt token.
    jwt_token_sig : str
        A jwt token signature.
    organization_id : str, optional
        An organization ID to use for authentication.
    """

    JWT_TOKEN_ENV = "VERTA_JWT_TOKEN"
    JWT_TOKEN_SIG_ENV = "VERTA_JWT_TOKEN_SIG"

    def __init__(self, jwt_token, jwt_token_sig, organization_id=None):
        self.jwt_token = jwt_token
        self.jwt_token_sig = jwt_token_sig
        self.organization_id = organization_id

    def export_env_vars_to_os(self):
        os.environ[self.JWT_TOKEN_ENV] = self.jwt_token
        if self.jwt_token_sig:
            os.environ[self.JWT_TOKEN_SIG_ENV] = self.jwt_token_sig

    def headers(self):
        headers = {
            "source": "JWT",
            "bearer_access_token": self.jwt_token,
            # without underscore, for NGINX support
            # https://www.nginx.com/resources/wiki/start/topics/tutorials/config_pitfalls#missing-disappearing-http-headers
            "bearer-access-token": self.jwt_token,
            "organization-id": self.organization_id or "",
        }
        if self.jwt_token_sig:
            headers["bearer_access_token_sig"] = headers[
                "bearer-access-token-sig"
            ] = self.jwt_token_sig
        if self.organization_id:
            headers["organization-id"] = self.organization_id
        return headers

    def __repr__(self):
        token = self.jwt_token[:8] + re.sub(r"[^-]", "*", self.jwt_token[8:])
        return "JWTCredentials({}, {}, {})".format(
            token,
            self.jwt_token_sig,
            self.organization_id,
        )

    @classmethod
    def load_from_os_env(cls):
        jwt_env = os.environ.get(cls.JWT_TOKEN_ENV)
        jwt_sig_env = os.environ.get(cls.JWT_TOKEN_SIG_ENV)
        if jwt_env and jwt_sig_env:
            return cls(jwt_env, jwt_sig_env)
        else:
            return None


def load_from_os_env():
    """Loads credentials from environment variables.

    Attempts to load credentials from dev key and email first, and falls back
    to loading credentials from JWT token variables otherwise. Returns `None` if
    no complete credentials are found.

    Returns
    -------
    :class:`~verta.credentials.Credentials` or None
        Credentials if discovered.
    """
    credentials = EmailCredentials.load_from_os_env()
    if not credentials:
        credentials = JWTCredentials.load_from_os_env()
    return credentials


def _build(
    email=None,
    dev_key=None,
    jwt_token=None,
    jwt_token_sig=None,
    organization_id=None,
):
    if email and dev_key:
        return EmailCredentials(email, dev_key, organization_id=organization_id)
    elif jwt_token:
        return JWTCredentials(jwt_token, jwt_token_sig, organization_id=organization_id)
    elif email or dev_key:
        raise ValueError("`email` and `dev_key` must be provided together")
    else:
        return None
