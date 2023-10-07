"""Test :class:`verta.Client`'s argument handling."""

import string

import hypothesis
import hypothesis.strategies as st
import pytest
import responses


class TestOrganizationIdAndNameError:
    """Verify setting both org id and name raises an exception."""

    _error = ValueError("cannot provide both `organization_id` and `organization_name`")
    raises_expected_error = pytest.raises(type(_error), match=str(_error))

    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_args(self, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with self.raises_expected_error:
            make_mock_client(organization_id=org_id, organization_name=org_name)

    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_env_vars(self, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with pytest.MonkeyPatch.context() as monkeypatch:
            monkeypatch.setenv("VERTA_ORG_ID", org_id)
            monkeypatch.setenv("VERTA_ORG_NAME", org_name)
            with self.raises_expected_error:
                make_mock_client()

    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_id_env_var_name_arg(self, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with pytest.MonkeyPatch.context() as monkeypatch:
            monkeypatch.setenv("VERTA_ORG_ID", org_id)
            with self.raises_expected_error:
                make_mock_client(organization_name=org_name)

    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_name_env_var_id_arg(self, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with pytest.MonkeyPatch.context() as monkeypatch:
            monkeypatch.setenv("VERTA_ORG_NAME", org_name)
            with self.raises_expected_error:
                make_mock_client(organization_id=org_id)


class TestOrganizationId:
    @hypothesis.given(org_id=st.uuids())
    def test_arg(self, make_mock_client, org_id):
        org_id = str(org_id)

        client = make_mock_client(organization_id=org_id)
        assert client._conn._credentials.organization_id == org_id

    @hypothesis.given(org_id=st.uuids())
    def test_env_var(self, make_mock_client, org_id):
        org_id = str(org_id)

        with pytest.MonkeyPatch.context() as monkeypatch:
            monkeypatch.setenv("VERTA_ORG_ID", org_id)
            client = make_mock_client()
        assert client._conn._credentials.organization_id == org_id

    @hypothesis.given(org_ids=st.lists(st.uuids(), min_size=2, max_size=2))
    def test_arg_overrides_env_var(self, make_mock_client, org_ids):
        org_id1, org_id2 = map(str, org_ids)

        with pytest.MonkeyPatch.context() as monkeypatch:
            monkeypatch.setenv("VERTA_ORG_ID", org_id1)
            client = make_mock_client(organization_id=org_id2)
        assert client._conn._credentials.organization_id == org_id2


class TestOrganizationName:
    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_arg(self, mock_conn, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with responses.RequestsMock() as rsps:
            rsps.get(
                url=f"{mock_conn.scheme}://{mock_conn.socket}/api/v2/uac-proxy/organization",
                status=200,
                json={"organizations": [{"id": org_id, "name": org_name}]},
            )

            client = make_mock_client(organization_name=org_name)
        assert client._conn._credentials.organization_id == org_id

    @hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
    def test_env_var(self, mock_conn, make_mock_client, org_id, org_name):
        org_id = str(org_id)

        with responses.RequestsMock() as rsps:
            rsps.get(
                url=f"{mock_conn.scheme}://{mock_conn.socket}/api/v2/uac-proxy/organization",
                status=200,
                json={"organizations": [{"id": org_id, "name": org_name}]},
            )

            with pytest.MonkeyPatch.context() as monkeypatch:
                monkeypatch.setenv("VERTA_ORG_NAME", org_name)
                client = make_mock_client()
        assert client._conn._credentials.organization_id == org_id

    @hypothesis.given(
        org_id=st.uuids(),
        org_names=st.lists(
            st.text(string.printable, min_size=1),
            min_size=2,
            max_size=2,
        ),
    )
    def test_arg_overrides_env_var(
        self,
        mock_conn,
        make_mock_client,
        org_id,
        org_names,
    ):
        org_id = str(org_id)
        org_name1, org_name2 = org_names

        with responses.RequestsMock() as rsps:
            rsps.get(
                url=f"{mock_conn.scheme}://{mock_conn.socket}/api/v2/uac-proxy/organization",
                status=200,
                json={"organizations": [{"id": org_id, "name": org_name2}]},
            )

            with pytest.MonkeyPatch.context() as monkeypatch:
                monkeypatch.setenv("VERTA_ORG_NAME", org_name1)
                client = make_mock_client(organization_name=org_name2)
        assert client._conn._credentials.organization_id == org_id
