# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._endpoint import Endpoint
from ._endpoints import Endpoints


documentation.reassign_module(
    [
        Endpoint,
        Endpoints,
    ],
    module_name=__name__,
)
