# -*- coding: utf-8 -*-

import warnings

from .deployment import DeployedModel


warnings.warn("`DeployedModel` is being moved to the `verta.deployment` module,"
              " and `verta._demo_utils` will be removed in a future version;"
              " consider using `from verta.deployment import DeployedModel` instead",
              category=FutureWarning)
