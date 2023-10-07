"""Test :class:`verta.Client`'s argument handling."""

import string

import hypothesis
import hypothesis.strategies as st
import pytest


@hypothesis.given(org_id=st.uuids(), org_name=st.text(string.printable, min_size=1))
def test_organization_id_and_name_error(make_mock_client, org_id, org_name):
    """Verify setting both org id and name raises an exception."""
    org_id = str(org_id)
    error = ValueError("cannot provide both `organization_id` and `organization_name`")
    raises_expected_error = pytest.raises(type(error), match=str(error))

    with raises_expected_error:
        make_mock_client(organization_id=org_id, organization_name=org_name)

    with pytest.MonkeyPatch.context() as monkeypatch:
        monkeypatch.setenv("VERTA_ORG_ID", org_id)
        monkeypatch.setenv("VERTA_ORG_NAME", org_name)
        with raises_expected_error:
            make_mock_client()

    with pytest.MonkeyPatch.context() as monkeypatch:
        monkeypatch.setenv("VERTA_ORG_ID", org_id)
        with raises_expected_error:
            make_mock_client(organization_name=org_name)

    with pytest.MonkeyPatch.context() as monkeypatch:
        monkeypatch.setenv("VERTA_ORG_NAME", org_name)
        with raises_expected_error:
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
