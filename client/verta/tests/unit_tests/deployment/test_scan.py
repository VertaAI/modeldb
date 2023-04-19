# -*- coding: utf-8 -*-

from hypothesis import given, HealthCheck, settings

from tests.unit_tests.strategies import build_scan_dict

from verta._internal_utils import time_utils
from verta.endpoint import build


@settings(suppress_health_check=[HealthCheck.function_scoped_fixture])
@given(build_scan_dict=build_scan_dict())
def test_instantiation(build_scan_dict):
    """Verify a BuildScan object can be instantated from a dict."""
    build_scan = build.BuildScan(build_scan_dict)

    assert build_scan.date_updated == time_utils.datetime_from_iso(
        build_scan_dict["date_updated"],
    )
    assert build_scan.progress == build.ScanProgressEnum(build_scan_dict["scan_status"])
    assert build_scan.status == build.ScanStatusEnum(build_scan_dict["safety_status"])
    assert build_scan.passed == (build_scan.status == build.ScanStatusEnum.SAFE)
    assert build_scan.failed == (
        build_scan.status in {build.ScanStatusEnum.UNKNOWN, build.ScanStatusEnum.UNSAFE}
    )
