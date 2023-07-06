# -*- coding: utf-8 -*-

"""Classes and utilities for inference pipelines."""

from verta._internal_utils import documentation

from ._orchestrator import LocalOrchestrator


documentation.reassign_module(
    [
        LocalOrchestrator,
    ],
    module_name=__name__,
)
