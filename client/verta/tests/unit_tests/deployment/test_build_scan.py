# -*- coding: utf-8 -*-

from typing import Any, Dict

from hypothesis import given, HealthCheck, settings
from responses.matchers import json_params_matcher

from tests.unit_tests.strategies import build_dict, build_scan_dict, mock_workspace

from verta._internal_utils import time_utils
from verta.endpoint.build import Build, BuildScan, ScanProgressEnum, ScanResultEnum


def assert_build_scan_fields(
    build_scan: BuildScan,
    build_scan_dict: Dict[str, Any],
) -> None:
    assert build_scan.date_updated == time_utils.datetime_from_iso(
        build_scan_dict["date_updated"],
    )
    assert build_scan.progress == ScanProgressEnum(build_scan_dict["scan_status"])
    if build_scan.progress == ScanProgressEnum.SCANNED:
        assert build_scan.passed == (build_scan.result == ScanResultEnum.SAFE)
        assert build_scan.result == ScanResultEnum(build_scan_dict["safety_status"])
    else:
        assert build_scan.passed is False
        assert build_scan.result is None


@given(build_scan_dict=build_scan_dict())
def test_instantiation(build_scan_dict):
    """Verify a BuildScan object can be instantated from a dict."""
    build_scan = BuildScan(build_scan_dict)

    assert_build_scan_fields(build_scan, build_scan_dict)


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(
    build_dict=build_dict(),
    build_scan_dict=build_scan_dict(),
    workspace=mock_workspace(),
)
def test_get_scan(mock_conn, mocked_responses, build_dict, build_scan_dict, workspace):
    """Verify we can construct a BuildScan object from get_scan()."""
    build = Build(mock_conn, workspace, build_dict)

    deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
    scan_url = f"{deployment_url}/builds/{build.id}/scan"

    with mocked_responses as rsps:
        rsps.get(url=scan_url, status=200, json=build_scan_dict)

        build_scan = build.get_scan()

    assert_build_scan_fields(build_scan, build_scan_dict)


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(
    build_dict=build_dict(external_scan=True),
    build_scan_dict=build_scan_dict(external_scan=True),
    workspace=mock_workspace(),
)
def test_start_external_scan(
    mock_conn, mocked_responses, build_dict, build_scan_dict, workspace
):
    """Verify we can construct a BuildScan object from start_scan(external=True)."""
    build = Build(mock_conn, workspace, build_dict)

    deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
    scan_url = f"{deployment_url}/workspace/{workspace}/builds/{build.id}/scan"

    with mocked_responses as rsps:
        rsps.post(
            url=scan_url,
            status=200,
            match=[json_params_matcher({"scan_external": True})],
            json=build_scan_dict,
        )

        build_scan = build.start_scan(external=True)

    assert_build_scan_fields(build_scan, build_scan_dict)
