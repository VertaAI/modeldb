# -*- coding: utf-8 -*-
"""Utilities for defining and interacting with deployable models."""

from verta._internal_utils import documentation

from ._deployedmodel import (
    DeployedModel,
    prediction_input_unpack,
    prediction_io_cleanup,
)


documentation.reassign_module(
    [
        DeployedModel,
        prediction_input_unpack,
        prediction_io_cleanup,
    ],
    module_name=__name__,
)
