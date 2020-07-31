# -*- coding: utf-8 -*-

import abc
import json

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _UpdateRule(object):
    _RULE_ID = 0
    _NAME = ""

    def __init__(self, value):
        self._value = value

    def _as_json(self):
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
        rule_id = int(rule_dict["rule_id"])
        rule_name = rule_dict['rule_parameters'][0]['name']
        rule_value = float(rule_dict['rule_parameters'][0]['value'])

        RULE_SUBCLASSES = [AverageLatencyThresholdRule, P90LatencyThresholdRule, ErrorRateRule]

        for Subclass in RULE_SUBCLASSES:
            if rule_name == Subclass._NAME:
                rule = Subclass(rule_value)
                break
        else:
            # does not match any rule
            raise ValueError("no rule with name {} exists".format(rule_name))

        if rule._RULE_ID != rule_id:
            raise ValueError("expected rule ID {} for rule {}, not {}.".format(
                rule._RULE_ID,
                rule_name,
                rule_id
            ))

        return rule


class AverageLatencyThresholdRule(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_avg"


class P90LatencyThresholdRule(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_p90"


class ErrorRateRule(_UpdateRule):
    _RULE_ID = 1002
    _NAME = "error_rate"
