# -*- coding: utf-8 -*-

import abc

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


class AverageLatencyThresholdRule(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_avg"


class P90LatencyThresholdRule(_UpdateRule):
    _RULE_ID = 1001
    _NAME = "latency_p90"


class ErrorRateRule(_UpdateRule):
    _RULE_ID = 1002
    _NAME = "error_rate"
