# -*- coding: utf-8 -*-
"""Model Monitoring."""

from verta._internal_utils import documentation

from ._monitor import Monitor


documentation.reassign_module(
    [
        Monitor,
    ],
    module_name=__name__,
)