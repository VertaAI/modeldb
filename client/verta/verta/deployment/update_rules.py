# -*- coding: utf-8 -*-

import abc

from ..external import six


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

class AverageLatencyThreshold(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_avg"

class P90LatencyThreshold(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_p90"

class ErrorRate(_UpdateRule):
    _RULE_ID = 1002
    _NAME = "error_rate"
