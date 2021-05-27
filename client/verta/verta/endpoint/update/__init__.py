# -*- coding: utf-8 -*-
"""Strategies for endpoint rollouts."""

from verta._internal_utils import documentation

from ._strategies import (
    DirectUpdateStrategy,
    CanaryUpdateStrategy,
)


documentation.reassign_module(
    [
        DirectUpdateStrategy,
        CanaryUpdateStrategy,
    ],
    module_name=__name__,
)
