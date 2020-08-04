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

    def __init__(self, cpu_target):
        self._cpu_target = cpu_target

    def _parameters_dict(self):
        return [
            {
                "name": "cpu_target",
                "value": self._cpu_target
            }
        ]


class RequestsPerWorker(_AutoscalingMetric):
    _METRIC_ID = 1002
    _NAME = "requests_per_worker"

    def __init__(self, requests_per_worker_target):
        self._requests_per_worker_target = requests_per_worker_target

    def _parameters_dict(self):
        return [
            {
                "name": "requests_per_worker_target",
                "value": self._requests_per_worker_target
            }
        ]


class MemoryUtilization(_AutoscalingMetric):
    _METRIC_ID = 1003
    _NAME = "memory_utilization"

    def __init__(self, memory_target):
        self._memory_target = memory_target

    def _parameters_dict(self):
        return [
            {
                "name": "memory_target",
                "value": self._memory_target
            }
        ]
