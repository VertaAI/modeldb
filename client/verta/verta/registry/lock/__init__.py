# -*- coding: utf-8 -*-
"""Lock levels for registered models."""

from verta._internal_utils import documentation

from ._lock_level import _LockLevel
from ._closed import Closed
from ._open import Open
from ._redact import Redact


documentation.reassign_module(
    [
        Closed,
        Open,
        Redact,
    ],
    module_name=__name__,
)

closed = Closed()
open = Open()
redact = Redact()
