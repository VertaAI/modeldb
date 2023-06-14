# -*- coding: utf-8 -*-

"""Classes and utilities for inference pipelines."""

from verta._internal_utils import documentation

from ._orchestrator import DeployedOrchestrator, LocalOrchestrator


documentation.reassign_module(
    [
        DeployedOrchestrator,
        LocalOrchestrator,
    ],
    module_name=__name__,
)
