# -*- coding: utf-8 -*-

"""Verta model builds."""

from verta._internal_utils import documentation

from ._build import Build


documentation.reassign_module(
    [
        Build,
    ],
    module_name=__name__,
)
