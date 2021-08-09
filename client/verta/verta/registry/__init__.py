# -*- coding: utf-8 -*-

"""Model registry."""

from verta._internal_utils import documentation

from ._verify_io import verify_io
from ._verta_model_base import VertaModelBase

documentation.reassign_module(
    [verify_io, VertaModelBase],
    module_name=__name__,
)
