# -*- coding: utf-8 -*-

"""Base classes used across the client."""

from verta._internal_utils import documentation

from ._lazy_list import _LazyList


documentation.reassign_module(
    [_LazyList],
    module_name=__name__,
)
