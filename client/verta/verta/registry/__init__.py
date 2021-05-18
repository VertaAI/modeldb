# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._entities.model import RegisteredModel
from ._entities.models import RegisteredModels
from ._entities.modelversion import RegisteredModelVersion
from ._entities.modelversions import RegisteredModelVersions


documentation.reassign_module(
    [
        RegisteredModel,
        RegisteredModels,
        RegisteredModelVersion,
        RegisteredModelVersions,
    ],
    module_name=__name__,
)
