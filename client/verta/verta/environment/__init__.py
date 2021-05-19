# -*- coding: utf-8 -*-
"""Utilities for environment versioning."""

from verta._internal_utils import documentation

from ._environment import _Environment
from ._python import Python


documentation.reassign_module(
    [Python],
    module_name=__name__,
)
