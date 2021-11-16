# -*- coding: utf-8 -*-
"""Model deployment and management."""

from verta._internal_utils import documentation

from ._build import Build
from ._kafka_settings import KafkaSettings
from ._endpoint import Endpoint
from ._endpoints import Endpoints


documentation.reassign_module(
    [
        Build,
        KafkaSettings,
        Endpoint,
        Endpoints,
    ],
    module_name=__name__,
)
