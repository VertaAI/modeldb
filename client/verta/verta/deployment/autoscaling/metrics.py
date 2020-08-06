# -*- coding: utf-8 -*-

import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _AutoscalingMetric(object):
    _METRIC_ID = 0
    _PARENT_NAME = ""
    _NAME = ""

    def __init__(self, value):
        self._value = str(value)

    def _as_dict(self):
        return {
            "metric_id": self._METRIC_ID,
            "name": self._PARENT_NAME,
            "parameters": [{
                "name": self._NAME,
                "value": self._value
            }]
        }


class CpuTarget(_AutoscalingMetric):
    _METRIC_ID = 1001
    _PARENT_NAME = "cpu_utilization"
    _NAME = "cpu_target"


class RequestsPerWorkerTarget(_AutoscalingMetric):
    _METRIC_ID = 1002
    _PARENT_NAME = "requests_per_worker"
    _NAME = "requests_per_worker_target"


class MemoryTarget(_AutoscalingMetric):
    _METRIC_ID = 1003
    _PARENT_NAME = "memory_utilization"
    _NAME = "memory_target"
