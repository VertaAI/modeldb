# -*- coding: utf-8 -*-
"""Rules to guide canary endpoint updates."""

import abc
import json

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _UpdateRule(object):
    _RULE_ID = 0
    _NAME = ""
    _PARENT_NAME = ""

    def __init__(self, value):
        self._value = str(value)

    def _as_dict(self):
        return {
            'rule_id': self._RULE_ID,
            'rule_parameters': [
                {
                    'name': self._NAME,
                    'value': self._value,
                },
            ],
        }

    @classmethod
    def _from_dict(cls, rule_dict):
        parent_name = rule_dict['rule']
        rule_name = rule_dict['rule_parameters'][0]['name']
        rule_value = float(rule_dict['rule_parameters'][0]['value'])

        for subcls in cls.__subclasses__():
            if parent_name == subcls._PARENT_NAME and rule_name == subcls._NAME:
                rule = subcls(rule_value)
                break
        else:
            # does not match any rule
            raise ValueError("no rule with name {} and parameter name {} exists".format(parent_name, rule_name))

        return rule


class MaximumAverageLatencyThresholdRule(_UpdateRule):
    """
    Rule for maximum average latency threshold.

    The JSON equivalent for this (as an element of canary rules JSON) is:

    .. code-block:: json

        {"rule": "latency_avg_max", "rule_parameters": [{"name": "threshold", "value": 0.1}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update.rules import MaximumAverageLatencyThresholdRule
        rule = MaximumAverageLatencyThresholdRule(100)

    """
    _RULE_ID = 1005
    _PARENT_NAME = "latency_avg_max"
    _NAME = "threshold"


class MaximumP90LatencyThresholdRule(_UpdateRule):
    """
    Rule for maximum p90 latency threshold.

    The JSON equivalent for this (as an element of canary rules JSON) is:

    .. code-block:: json

        {"rule": "latency_p90_max", "rule_parameters": [{"name": "threshold", "value": 0.1}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update.rules import MaximumP90LatencyThresholdRule
        rule = MaximumP90LatencyThresholdRule(100)

    """
    _RULE_ID = 1006
    _PARENT_NAME = "latency_p90_max"
    _NAME = "threshold"


class MaximumRequestErrorPercentageThresholdRule(_UpdateRule):
    """
    Rule for maximum request error percentage threshold.

    The JSON equivalent for this (as an element of canary rules JSON) is:

    .. code-block:: json

        {"rule": "error_4xx_rate", "rule_parameters": [{"name": "threshold", "value": 0.1}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update.rules import MaximumRequestErrorPercentageThresholdRule
        rule = MaximumRequestErrorPercentageThresholdRule(0.5)

    """
    _RULE_ID = 1007
    _PARENT_NAME = "error_4xx_rate"
    _NAME = "threshold"


class MaximumServerErrorPercentageThresholdRule(_UpdateRule):
    """
    Rule for maximum server error percentage threshold.

    The JSON equivalent for this (as an element of canary rules JSON) is:

    .. code-block:: json

        {"rule": "error_5xx_rate", "rule_parameters": [{"name": "threshold", "value": 0.1}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.update.rules import MaximumServerErrorPercentageThresholdRule
        rule = MaximumServerErrorPercentageThresholdRule(0.5)

    """
    _RULE_ID = 1008
    _PARENT_NAME = "error_5xx_rate"
    _NAME = "threshold"
