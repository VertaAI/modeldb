# -*- coding: utf-8 -*-

import os

import hypothesis
import hypothesis.strategies as st
import pytest
import requests
import six

import verta
from verta._internal_utils import _utils


class TestClient:

    @pytest.mark.skipif('VERTA_EMAIL' not in os.environ or 'VERTA_DEV_KEY' not in os.environ, reason="insufficient Verta credentials")
    def test_auth_headers(self, host):
        expected_headers = {
            'Grpc-Metadata-email': os.environ['VERTA_EMAIL'],
            'Grpc-Metadata-developer_key': os.environ['VERTA_DEV_KEY'],
            'Grpc-Metadata-developer-key': os.environ['VERTA_DEV_KEY'],
        }
        def assert_contains_expected_headers(headers):
            for key, val in expected_headers.items():
                assert key in headers
                assert headers[key] == val

        # present in Client state
        client = verta.Client(host)
        assert_contains_expected_headers(client._conn.auth)

        # sent in requests
        with pytest.raises(requests.HTTPError) as exc_info:
            verta.Client("www.google.com")
        request = exc_info.value.request
        assert_contains_expected_headers(request.headers)

    @hypothesis.given(
        headers=st.dictionaries(
            keys=st.text(min_size=1),
            values=st.text(min_size=1),
            min_size=1,
        )
    )
    def test_extra_auth_headers_in_conn(self, headers):
        client = verta.Client(extra_auth_headers=headers, _connect=False)

        for key, val in headers.items():
            assert key in client._conn.auth
            assert val == client._conn.auth[key]

    def test_extra_auth_headers_in_request(self, strs):
        headers = dict(zip(strs[:len(strs)//2], strs[len(strs)//2:]))
        client = verta.Client(extra_auth_headers=headers)

        url = "http://httpbin.org/anything"
        response = _utils.make_request("GET", url, client._conn)

        for key, val in headers.items():
            assert key in response.request.headers
            assert val == response.request.headers[key]

    @pytest.mark.oss
    def test_no_auth(self, host):
        EMAIL_KEY, DEV_KEY_KEY = "VERTA_EMAIL", "VERTA_DEV_KEY"
        EMAIL, DEV_KEY = os.environ.pop(EMAIL_KEY, None), os.environ.pop(DEV_KEY_KEY, None)
        try:
            client = verta.Client(host)

            # still has source set
            assert 'Grpc-Metadata-source' in client._conn.auth

            assert client.set_project()

            client.proj.delete()
        finally:
            if EMAIL is not None:
                os.environ[EMAIL_KEY] = EMAIL
            if DEV_KEY is not None:
                os.environ[DEV_KEY_KEY] = DEV_KEY


    @pytest.mark.skipif('VERTA_EMAIL' not in os.environ or 'VERTA_DEV_KEY' not in os.environ, reason="insufficient Verta credentials")
    def test_verta_https(self):
        hosts = [
            "app.verta.ai",
        ]

        for host in hosts:
            # https by default
            conn = verta.Client(host)._conn
            assert conn.scheme == "https"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

            # http if provided
            conn = verta.Client("http://{}".format(host))._conn
            assert conn.scheme == "http"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

            # https if provided
            conn = verta.Client("https://{}".format(host))._conn
            assert conn.scheme == "https"
            assert conn.scheme == conn.auth['Grpc-Metadata-scheme']

    def test_else_http(self):
        # test hosts must not redirect http to https
        hosts = [
            "www.google.com",
        ]

        for host in hosts:
            # http by default
            try:
                verta.Client(host, max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "http"
            else:
                raise RuntimeError("faulty test; expected error")

            # http if provided
            try:
                verta.Client("http://{}".format(host), max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "http"
            else:
                raise RuntimeError("faulty test; expected error")

            # https if provided
            try:
                verta.Client("https://{}".format(host), max_retries=0)
            except requests.HTTPError as e:
                assert e.request.url.split(':', 1)[0] == "https"
            else:
                raise RuntimeError("faulty test; expected error")

    @pytest.mark.not_oss
    def test_wrong_credentials(self):
        EMAIL_KEY, DEV_KEY_KEY = "VERTA_EMAIL", "VERTA_DEV_KEY"
        old_email, old_dev_key = os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY]

        try:
            os.environ[EMAIL_KEY] = "abc@email.com"
            os.environ[DEV_KEY_KEY] = "def"

            with pytest.raises(requests.exceptions.HTTPError) as excinfo:
                verta.Client()

            excinfo_value = str(excinfo.value).strip()
            assert "401 Client Error" in excinfo_value
            assert "authentication failed; please check `VERTA_EMAIL` and `VERTA_DEV_KEY`" in excinfo_value
        finally:
            os.environ[EMAIL_KEY], os.environ[DEV_KEY_KEY] = old_email, old_dev_key

    @pytest.mark.not_oss
    def test_ca_bundle_env_var(self, https_client):
        """Verify make_request() honors REQUESTS_CA_BUNDLE env var."""
        REQUESTS_CA_BUNDLE_ENV_VAR = "REQUESTS_CA_BUNDLE"
        good_ca_bundle_path = os.environ.get(REQUESTS_CA_BUNDLE_ENV_VAR)
        conn = https_client._conn

        url = "{}://{}/".format(conn.scheme, conn.socket)

        # as a control, make sure request works
        response = _utils.make_request("GET", url, conn)
        conn.must_response(response)

        bad_ca_bundle_path = "foo"
        msg_match = (
            "^Could not find a suitable TLS CA certificate bundle,"
            " invalid path: {}$".format(bad_ca_bundle_path)
        )
        error_type = IOError if six.PY2 else OSError
        try:
            os.environ[REQUESTS_CA_BUNDLE_ENV_VAR] = bad_ca_bundle_path

            with pytest.raises(error_type, match=msg_match):
                _utils.make_request("GET", url, https_client._conn)
        finally:
            if good_ca_bundle_path:
                os.environ[REQUESTS_CA_BUNDLE_ENV_VAR] = good_ca_bundle_path
            else:
                del os.environ[REQUESTS_CA_BUNDLE_ENV_VAR]
