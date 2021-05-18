# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._alerter import (
    _Alerter,
    FixedAlerter,
    ReferenceAlerter,
)


documentation.reassign_module(
    [
        FixedAlerter,
        ReferenceAlerter,
    ],
    module_name=__name__,
)
