# -*- coding: utf-8 -*-
"""Visibility settings for use with entities."""

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
