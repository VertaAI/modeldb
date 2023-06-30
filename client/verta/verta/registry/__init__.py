# -*- coding: utf-8 -*-

"""Model registry."""

from verta._internal_utils import documentation

from ._check_model_dependencies import check_model_dependencies
from ._docker_image import DockerImage
from ._verify_io import verify_io
from ._verta_model_base import VertaModelBase
from ._validate_input import validate_input

documentation.reassign_module(
    [check_model_dependencies, DockerImage, verify_io, VertaModelBase, validate_input],
    module_name=__name__,
)
