# -*- coding: utf-8 -*-

from typing import Any, Dict

from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import build_dict

from verta._internal_utils import time_utils
from verta.endpoint.build import Build


def assert_build_fields(build: Build, build_dict: Dict[str, Any]) -> None:
    assert build.id == build_dict["id"]
    assert build.date_created == time_utils.datetime_from_iso(
        build_dict["date_created"],
    )
    assert build.status == build_dict["status"]
    assert build.message == build_dict["message"] or Build._EMPTY_MESSAGE


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(build_dict=build_dict())
def test_instantiation(build_dict):
    """Verify a Build object can be instantated from a dict."""
    build = Build(build_dict)

    assert_build_fields(build, build_dict)


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(build_dict=build_dict())
def test_endpoint_get_current_build(
    mock_endpoint,
    mock_conn,
    mocked_responses,
    build_dict,
):
    """Verify we can construct a Build object from get_current_build()."""
    deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
    stages_url = f"{deployment_url}/workspace/{mock_endpoint.workspace}/endpoints/{mock_endpoint.id}/stages"
    build_url = f"{deployment_url}/workspace/{mock_endpoint.workspace}/builds/{build_dict['id']}"

    with mocked_responses as rsps:
        rsps.get(
            url=stages_url,
            status=200,
            json={"stages": [{"components": [{"build_id": build_dict["id"]}]}]},
        )
        rsps.get(url=build_url, status=200, json=build_dict)

        build = mock_endpoint.get_current_build()

    assert_build_fields(build, build_dict)
