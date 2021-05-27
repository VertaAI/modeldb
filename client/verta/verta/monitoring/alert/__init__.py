# -*- coding: utf-8 -*-
"""Monitoring alerts."""

from verta._internal_utils import documentation

from ._alerter import (
    _Alerter,
    FixedAlerter,
    ReferenceAlerter,
    RangeAlerter
)


documentation.reassign_module(
    [
        FixedAlerter,
        ReferenceAlerter,
        RangeAlerter
    ],
    module_name=__name__,
)
