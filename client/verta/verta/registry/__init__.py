# -*- coding: utf-8 -*-

"""Model registry."""

from verta._internal_utils import documentation

from ._docker_image import DockerImage
from ._verify_io import verify_io
from ._verta_model_base import VertaModelBase

documentation.reassign_module(
    [DockerImage, verify_io, VertaModelBase],
    module_name=__name__,
)
