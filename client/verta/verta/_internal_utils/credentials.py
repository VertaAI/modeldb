# -*- coding: utf-8 -*-

import abc
import os
import re

from verta.external import six


def build(email=None, dev_key=None, jwt_token=None, jwt_token_sig=None):
    if email and dev_key:
        return EmailCredentials(email, dev_key)
    elif jwt_token and jwt_token_sig:
        return JWTCredentials(jwt_token, jwt_token_sig)
    elif email or dev_key:
        raise ValueError("`email` and `dev_key` must be provided together")
    elif jwt_token or jwt_token_sig:
        raise ValueError("`jwt_token` and `jwt_token_sig` must be provided together")
    else:
        return None

def load_from_os_env():
    credentials = EmailCredentials.load_from_os_env()
    if not credentials:
        credentials = JWTCredentials.load_from_os_env()
    return credentials

@six.add_metaclass(abc.ABCMeta)
class Credentials(object):

    @abc.abstractmethod
    def export_env_vars_to_os(self):
        raise NotImplementedError

    @abc.abstractmethod
    def headers(self):
        raise NotImplementedError


class EmailCredentials(Credentials):

    EMAIL_ENV = "VERTA_EMAIL"
    DEV_KEY_ENV = "VERTA_DEV_KEY"

    def __init__(self, email, dev_key):
        self.email = email
        self.dev_key = dev_key

    def export_env_vars_to_os(self):
        os.environ[self.EMAIL_ENV] = self.email
        os.environ[self.DEV_KEY_ENV] = self.dev_key

    def headers(self):
        return {
            'source': 'PythonClient',
            'email': self.email,
            'developer_key': self.dev_key,
            # without underscore, for NGINX support
            # https://www.nginx.com/resources/wiki/start/topics/tutorials/config_pitfalls#missing-disappearing-http-headers
            'developer-key': self.dev_key,
        }

    def __repr__(self):
        key = self.dev_key[:8] + re.sub(r"[^-]", '*', self.dev_key[8:])
        return "EmailCredentials({}, {})".format(self.email, key)

    @classmethod
    def load_from_os_env(cls):
        email_env = os.environ.get(cls.EMAIL_ENV)
        dev_key_env = os.environ.get(cls.DEV_KEY_ENV)
        if email_env and dev_key_env:
            return cls(email_env, dev_key_env)
        else:
            return None

class JWTCredentials(Credentials):

    JWT_TOKEN_ENV = "VERTA_JWT_TOKEN"
    JWT_TOKEN_SIG_ENV = "VERTA_JWT_TOKEN_SIG"

    def __init__(self, jwt_token, jwt_token_sig):
        self.jwt_token = jwt_token
        self.jwt_token_sig = jwt_token_sig

    def export_env_vars_to_os(self):
        os.environ[self.JWT_TOKEN_ENV] = self.jwt_token
        os.environ[self.JWT_TOKEN_SIG_ENV] = self.jwt_token_sig

    def headers(self):
        return {
            'source': 'JWT',
            'bearer_access_token': self.jwt_token,
            'bearer_access_token_sig': self.jwt_token_sig,
            # without underscore, for NGINX support
            # https://www.nginx.com/resources/wiki/start/topics/tutorials/config_pitfalls#missing-disappearing-http-headers
            'bearer-access-token': self.jwt_token,
            'bearer-access-token-sig': self.jwt_token_sig,
        }

    def __repr__(self):
        token = self.jwt_token[:8] + re.sub(r"[^-]", '*', self.jwt_token[8:])
        return "JWTCredentials({}, {})".format(token, self.jwt_token_sig)

    @classmethod
    def load_from_os_env(cls):
        jwt_env = os.environ.get(cls.JWT_TOKEN_ENV)
        jwt_sig_env = os.environ.get(cls.JWT_TOKEN_SIG_ENV)
        if jwt_env and jwt_sig_env:
            return cls(jwt_env, jwt_sig_env)
        else:
            return None
