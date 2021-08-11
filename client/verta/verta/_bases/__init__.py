# -*- coding: utf-8 -*-

"""Base classes used across multiple client modules."""

from verta._internal_utils import documentation

from ._lazy_list import _LazyList
from ._paginated_iterable import _PaginatedIterable


documentation.reassign_module(
    [_PaginatedIterable, _LazyList],
    module_name=__name__,
)
