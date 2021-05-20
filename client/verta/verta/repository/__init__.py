# -*- coding: utf-8 -*-
"""Model ingredient versioning."""

from verta._internal_utils import documentation

from ._repository import Repository
from ._commit import Commit
from ._diff import Diff


documentation.reassign_module(
    [
        Repository,
        Commit,
        Diff,
    ],
    module_name=__name__,
)
