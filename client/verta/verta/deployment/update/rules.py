# -*- coding: utf-8 -*-

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

    @staticmethod
    def _from_dict(rule_dict):
        parent_name = rule_dict['rule']
        rule_name = rule_dict['rule_parameters'][0]['name']
        rule_value = float(rule_dict['rule_parameters'][0]['value'])

        RULE_SUBCLASSES = [
            MaximumAverageLatencyThresholdRule,
            MaximumP90LatencyThresholdRule,
            MaximumRequestErrorPercentageThresholdRule,
            MaximumServerErrorPercentageThresholdRule
        ]

        for Subclass in RULE_SUBCLASSES:
            if parent_name == Subclass._PARENT_NAME and rule_name == Subclass._NAME:
                rule = Subclass(rule_value)
                break
        else:
            # does not match any rule
            raise ValueError("no rule with name {} and parameter name {} exists".format(parent_name, rule_name))

        return rule


class MaximumAverageLatencyThresholdRule(_UpdateRule):
    """
    Rule for maximum average latency threshold.

    """
    _RULE_ID = 1005
    _PARENT_NAME = "latency_avg_max"
    _NAME = "threshold"


class MaximumP90LatencyThresholdRule(_UpdateRule):
    """
    Rule for maximum p90 latency threshold.

    """
    _RULE_ID = 1006
    _PARENT_NAME = "latency_p90_max"
    _NAME = "threshold"


class MaximumRequestErrorPercentageThresholdRule(_UpdateRule):
    """
    Rule for maximum request error percentage threshold.

    """
    _RULE_ID = 1007
    _PARENT_NAME = "error_4xx_rate"
    _NAME = "threshold"


class MaximumServerErrorPercentageThresholdRule(_UpdateRule):
    """
    Rule for maximum server error percentage threshold.

    """
    _RULE_ID = 1008
    _PARENT_NAME = "error_5xx_rate"
    _NAME = "threshold"
