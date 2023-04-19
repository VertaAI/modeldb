# -*- coding: utf-8 -*-

from hypothesis import given, HealthCheck, settings
import pytest

from tests.unit_tests.strategies import build_dict, build_scan_dict

from verta._internal_utils import time_utils
from verta.endpoint.build import Build, BuildScan, ScanProgressEnum, ScanStatusEnum


@given(build_scan_dict=build_scan_dict())
def test_instantiation(build_scan_dict):
    """Verify a BuildScan object can be instantated from a dict."""
    build_scan = BuildScan(build_scan_dict)

    assert build_scan.date_updated == time_utils.datetime_from_iso(
        build_scan_dict["date_updated"],
    )
    assert build_scan.progress == ScanProgressEnum(build_scan_dict["scan_status"])
    if build_scan.progress == ScanProgressEnum.SCANNED:
        assert build_scan.passed == (build_scan.status == ScanStatusEnum.SAFE)
        assert build_scan.status == ScanStatusEnum(build_scan_dict["safety_status"])
    else:
        assert build_scan.passed is False
        assert build_scan.status is None


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(build_dict=build_dict(), build_scan_dict=build_scan_dict())
def test_get_scan(mock_conn, mocked_responses, build_dict, build_scan_dict):
    """Verify we can construct a BuildScan object from get_scan()."""
    build = Build(mock_conn, build_dict)

    deployment_url = f"{mock_conn.scheme}://{mock_conn.socket}/api/v1/deployment"
    scan_url = f"{deployment_url}/builds/{build.id}/scan"

    with mocked_responses as rsps:
        rsps.get(url=scan_url, status=200, json=build_scan_dict)

        build_scan = build.get_scan()

    assert build_scan.date_updated == time_utils.datetime_from_iso(
        build_scan_dict["date_updated"],
    )
    assert build_scan.progress == ScanProgressEnum(build_scan_dict["scan_status"])
    if build_scan.progress == ScanProgressEnum.SCANNED:
        assert build_scan.passed == (build_scan.status == ScanStatusEnum.SAFE)
        assert build_scan.status == ScanStatusEnum(build_scan_dict["safety_status"])
    else:
        assert build_scan.passed is False
        assert build_scan.status is None
