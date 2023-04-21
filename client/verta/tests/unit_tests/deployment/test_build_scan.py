# -*- coding: utf-8 -*-

from hypothesis import given, HealthCheck, settings
import pytest

from tests.unit_tests.strategies import build_scan_dict

from verta._internal_utils import time_utils
from verta.endpoint.build import BuildScan, ScanProgressEnum, ScanStatusEnum


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
