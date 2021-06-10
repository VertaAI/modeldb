# -*- coding: utf-8 -*-

"""Model registry."""

from verta._internal_utils import documentation

from ._verta_model_base import VertaModelBase

documentation.reassign_module(
    [VertaModelBase],
    module_name=__name__,
)
