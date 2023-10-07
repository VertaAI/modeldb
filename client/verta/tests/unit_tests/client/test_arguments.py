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
