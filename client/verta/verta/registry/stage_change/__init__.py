# -*- coding: utf-8 -*-

"""Stage changes for registered model versions."""

from verta._internal_utils import documentation

from ._stage_change import _StageChange
from ._archived import Archived
from ._development import Development
from ._production import Production
from ._staging import Staging


documentation.reassign_module(
    [
        Archived,
        Development,
        Production,
        Staging,
    ],
    module_name=__name__,
)
