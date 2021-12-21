# -*- coding: utf-8 -*-

import pytest

pytest.skip(
    "alert functionality has breaking backend changes (https://github.com/VertaAI/services/pull/1537)",
    allow_module_level=True,
)
