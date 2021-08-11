# -*- coding: utf-8 -*-
"""Alert statuses."""

from verta._internal_utils import documentation

from ._status import (
    _AlertStatus,
    Alerting,
    Ok,
)

documentation.reassign_module(
    [
        Alerting,
        Ok,
    ],
    module_name=__name__,
)
