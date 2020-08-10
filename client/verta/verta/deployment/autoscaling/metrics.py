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
            "name": self._NAME,
            "parameters": [{
                "name": "target",
                "value": self._value
            }]
        }


class CpuUtilization(_AutoscalingMetric):
    """
    Memory utilization target to trigger autoscaling.

    """
    _METRIC_ID = 1001
    _NAME = "cpu_utilization"


class RequestsPerWorker(_AutoscalingMetric):
    """
    CPU utilization target to trigger autoscaling.

    """
    _METRIC_ID = 1002
    _NAME = "requests_per_worker"


class MemoryUtilization(_AutoscalingMetric):
    """
    Number of requests per worker target to trigger autoscaling.

    """
    _METRIC_ID = 1003
    _NAME = "memory_utilization"
