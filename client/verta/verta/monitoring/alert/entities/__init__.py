# -*- coding: utf-8 -*-
"""Entities for defining alerts in the Verta backend."""

from verta._internal_utils import documentation

from ._alert import (
    Alert,
    Alerts,
    AlertHistoryItem,
)


documentation.reassign_module(
    [
        Alert,
        Alerts,
        AlertHistoryItem,
    ],
    module_name=__name__,
)
