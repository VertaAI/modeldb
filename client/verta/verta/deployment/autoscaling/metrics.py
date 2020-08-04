# -*- coding: utf-8 -*-

import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _AutoscalingMetric(object):
    _METRIC_ID = 0
    _NAME = ""

    def __init__(self, value):
        self._value = value

    def _as_json(self):
        return {
            "metric_id": self._METRIC_ID,
            "parameters": [
                {
                    "name": self._NAME,
                    "value": self._value
                }
            ]
        }


class CpuUtilization(_AutoscalingMetric):
    _METRIC_ID = 1001
    _NAME = "cpu_utilization"
