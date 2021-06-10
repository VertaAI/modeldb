# -*- coding: utf-8 -*-
"""Utilities for code versioning."""

from verta._internal_utils import documentation

from ._code import _Code
from ._git import Git
from ._notebook import Notebook


documentation.reassign_module(
    [
        Git,
        Notebook,
    ],
    module_name=__name__,
)
