# -*- coding: utf-8 -*-

"""Verta model builds."""

from verta._internal_utils import documentation

from ._build import Build
from ._build_scan import BuildScan, ScanProgressEnum, ScanResultEnum


documentation.reassign_module(
    [
        Build,
        BuildScan,
        ScanProgressEnum,
        ScanResultEnum,
    ],
    module_name=__name__,
)
