# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._autoscaling import Autoscaling


documentation.reassign_module(
    [Autoscaling],
    module_name=__name__,
)
