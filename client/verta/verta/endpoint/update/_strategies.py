# -*- coding: utf-8 -*-

import abc

from ...external import six
from .rules import _UpdateRule


@six.add_metaclass(abc.ABCMeta)
class _UpdateStrategy(object):
    _STRATEGY = ""

    @abc.abstractmethod
    def _as_build_update_req_body(self):
        """
        Returns
        -------
        dict
            JSON to be passed as the body for an Endpoint update request.

        """
        pass


class DirectUpdateStrategy(_UpdateStrategy):
    """A direct endpoint update strategy.

    The JSON equivalent for this is:

    .. code-block:: json

        {
            "strategy": "direct"
        }

    Represents direct update strategy for Endpoint.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update import DirectUpdateStrategy
        strategy = DirectUpdateStrategy()

    """
    _STRATEGY = "rollout"

    def _as_build_update_req_body(self):
        return {
            'strategy': self._STRATEGY,
        }


class CanaryUpdateStrategy(_UpdateStrategy):
    """A rule-based canary endpoint update strategy.

    The JSON equivalent for this is:

    .. code-block:: json

        {
            "strategy": "canary",
            "canary_strategy": {
                "progress_step": 0.2,
                "progress_interval_seconds": 10,
                "rules": []
            }
        }

    Represents canary update strategy for Endpoint.

    Parameters
    ----------
    interval : int
        Rollout interval, in seconds.
    step : float in (0, 1]
        Ratio of deployment to roll out per `interval`.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update import CanaryUpdateStrategy
        strategy = CanaryUpdateStrategy(interval=10, step=.1)

    """
    _STRATEGY = "canary"

    def __init__(self, interval, step):
        interval_err_msg = "`interval` must be int greater than 0"
        if not isinstance(interval, int):
            raise TypeError(interval_err_msg)
        if not interval > 0:
            raise ValueError(interval_err_msg)

        step_err_msg = "`step` must be float in (0, 1]"
        if not isinstance(step, float):
            raise TypeError(step_err_msg)
        if not 0 < step <= 1:
            raise ValueError(step_err_msg)

        self._progress_interval_seconds = interval
        self._progress_step = step
        self._rules = []

    def _as_build_update_req_body(self):
        if not self._rules:
            raise RuntimeError("canary update strategy must have at least one rule")

        return {
            'strategy': self._STRATEGY,
            'canary_strategy': {
                'progress_interval_seconds': self._progress_interval_seconds,
                'progress_step': self._progress_step,
                'rules': list(map(lambda rule: rule._as_dict(), self._rules)),
            },
        }

    def add_rule(self, rule):
        if not isinstance(rule, _UpdateRule):
            raise TypeError("strategy must be an object from verta.endpoint.update_rules")

        self._rules.append(rule)
