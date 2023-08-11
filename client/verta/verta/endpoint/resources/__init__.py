# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._resources import Resources
from .nvidia_gpu import NvidiaGPU, NvidiaGPUModel

documentation.reassign_module(
    [
        Resources,
        NvidiaGPU,
        NvidiaGPUModel,
    ],
    module_name=__name__,
)
