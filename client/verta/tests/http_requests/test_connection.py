# -*- coding: utf-8 -*-

from verta import credentials
from verta._internal_utils._utils import Connection, _GRPC_PREFIX


https_scheme = "https"
fake_socket = "test:8080"
custom_headers = {"custom-header": "custom-header-value"}


def assert_dictionary_is_subset(subset_dict, test_dict):
    for key, value in subset_dict.items():
        assert key in test_dict
        assert test_dict.get(key) == value


class TestConnection:
    def test_no_headers(self):
        conn = Connection(scheme=https_scheme, socket=fake_socket, credentials=None)
        assert isinstance(conn.headers, dict)
        conn.headers = None
        assert isinstance(conn.headers, dict)
        assert conn.headers.get(_GRPC_PREFIX + "scheme") == https_scheme

    def test_no_auth(self):
        conn = Connection(
            scheme=https_scheme,
            socket=fake_socket,
            credentials=None,
            headers=custom_headers,
        )
        assert conn.credentials is None
        conn_headers = conn.headers
        assert_dictionary_is_subset(custom_headers, conn_headers)

    def test_dev_key_auth(self):
        fake_email = "test@verta.ai"
        fake_dev_key = "1234"
        email_credentials = credentials._build(email=fake_email, dev_key=fake_dev_key)
        conn = Connection(
            scheme=https_scheme,
            socket=fake_socket,
            credentials=email_credentials,
            headers=custom_headers,
        )
        conn_headers = conn.headers
        expected = {
            _GRPC_PREFIX + "scheme": https_scheme,
            _GRPC_PREFIX + "source": "PythonClient",
            _GRPC_PREFIX + "email": fake_email,
            _GRPC_PREFIX + "developer_key": fake_dev_key,
            _GRPC_PREFIX + "developer-key": fake_dev_key,
        }
        assert_dictionary_is_subset(custom_headers, conn_headers)
        assert_dictionary_is_subset(expected, conn_headers)

    def test_jwt_auth(self):
        fake_token = "token"
        fake_token_sig = "token_sig"
        jwt_credentials = credentials._build(
            jwt_token=fake_token, jwt_token_sig=fake_token_sig
        )
        conn = Connection(
            scheme=https_scheme,
            socket=fake_socket,
            credentials=jwt_credentials,
            headers=custom_headers,
        )
        conn_headers = conn.headers
        expected = {
            _GRPC_PREFIX + "scheme": https_scheme,
            _GRPC_PREFIX + "source": "JWT",
            _GRPC_PREFIX + "bearer_access_token": fake_token,
            _GRPC_PREFIX + "bearer_access_token_sig": fake_token_sig,
            _GRPC_PREFIX + "bearer-access-token": fake_token,
            _GRPC_PREFIX + "bearer-access-token-sig": fake_token_sig,
        }
        assert_dictionary_is_subset(custom_headers, conn_headers)
        assert_dictionary_is_subset(expected, conn_headers)

    def test_jwt_auth_without_sig(self):
        fake_token = "token"
        jwt_credentials = credentials._build(jwt_token=fake_token)
        conn = Connection(
            scheme=https_scheme,
            socket=fake_socket,
            credentials=jwt_credentials,
            headers=custom_headers,
        )
        conn_headers = conn.headers
        expected = {
            _GRPC_PREFIX + "scheme": https_scheme,
            _GRPC_PREFIX + "source": "JWT",
            _GRPC_PREFIX + "bearer_access_token": fake_token,
            _GRPC_PREFIX + "bearer-access-token": fake_token,
        }
        assert_dictionary_is_subset(custom_headers, conn_headers)
        assert_dictionary_is_subset(expected, conn_headers)
