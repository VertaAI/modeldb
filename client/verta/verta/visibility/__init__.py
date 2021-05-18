# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._org_custom import OrgCustom
from ._org_default import OrgDefault
from ._private import Private
from ._workspace_default import _WorkspaceDefault


documentation.reassign_module(
    [
        OrgCustom,
        OrgDefault,
        Private,
    ],
    module_name=__name__,
)
