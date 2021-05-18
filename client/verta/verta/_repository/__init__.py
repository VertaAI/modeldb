# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from .commit import Commit
from .repository import Repository


documentation.reassign_module(
    [
        Commit,
        Repository,
    ],
    module_name=__name__,
)
