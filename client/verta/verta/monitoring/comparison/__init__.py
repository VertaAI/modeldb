# -*- coding: utf-8 -*-
"""Utilities for representing numerical comparisons."""

from verta._internal_utils import documentation

from ._verta_comparison import _VertaComparison
from ._equal_to import EqualTo
from ._not_equal_to import NotEqualTo
from ._greater_than import GreaterThan
from ._greater_than_or_equal_to import GreaterThanOrEqualTo
from ._less_than import LessThan
from ._less_than_or_equal_to import LessThanOrEqualTo


documentation.reassign_module(
    [
        EqualTo,
        NotEqualTo,
        GreaterThan,
        GreaterThanOrEqualTo,
        LessThan,
        LessThanOrEqualTo,
    ],
    module_name=__name__,
)
