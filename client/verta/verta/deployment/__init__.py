# -*- coding: utf-8 -*-
"""Utilities for defining and interacting with deployable models."""
# NOTE: This submodule is to be merged into verta.endpoint (VR-5882), and
#       should not be expanded further.

from verta._internal_utils import documentation

from ._deployedmodel import (
    DeployedModel,
    prediction_input_unpack,
    prediction_io_cleanup,
)

from ._pipeline import Pipeline
from ._pipelinestep import PipelineStep


documentation.reassign_module(
    [
        DeployedModel,
        Pipeline,
        PipelineStep,
        prediction_input_unpack,
        prediction_io_cleanup,
    ],
    module_name=__name__,
)
