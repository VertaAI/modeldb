# -*- coding: utf-8 -*-

import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _AutoscalingMetric(object):
    _METRIC_ID = 0
    _NAME = ""

    def _as_dict(self):
        return {
            "metric_id": self._METRIC_ID,
            "name": self._NAME,
            "parameters": self._parameters_dict()
        }

    def _parameters_dict(self):
        raise NotImplementedError


class CpuUtilization(_AutoscalingMetric):
    _METRIC_ID = 1001
    _NAME = "cpu_utilization"

    def __init__(self, cpu_target, cpu_threshold):
        self._cpu_target = cpu_target
        self._cpu_threshold = cpu_threshold

    def _parameters_dict(self):
        return [
            {
                "name": "cpu_target",
                "value": self._cpu_target
            }
        ]
