# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._step_handler import ModelContainerStepHandler, ModelObjectStepHandler


documentation.reassign_module(
    [
        ModelContainerStepHandler,
        ModelObjectStepHandler,
    ],
    module_name=__name__,
)
