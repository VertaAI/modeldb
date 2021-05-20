# -*- coding: utf-8 -*-

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
