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

    @staticmethod
    def _from_dict(metric_dict):
        parent_name = metric_dict["metric"]
        metric_name = metric_dict["parameters"][0]["name"]
        metric_value = metric_dict["parameters"][0]["value"]

        METRIC_SUBCLASSES = [CpuUtilizationTarget, RequestsPerWorkerTarget, MemoryUtilizationTarget]

        for Subclass in METRIC_SUBCLASSES:
            if parent_name == Subclass._PARENT_NAME and metric_name == Subclass._NAME:
                metric = Subclass(metric_value)
                break
        else:
            # does not match any rule
            raise ValueError("no metric with name {} and parameter name {} exists".format(parent_name, metric_name))

        return metric


class CpuUtilizationTarget(_AutoscalingMetric):
    """
    CPU utilization target to trigger autoscaling.

    The JSON equivalent for this (as an element of autoscaling metrics JSON) is:

    .. code-block:: json

        {"metric": "cpu_utilization", "parameters": [{"name": "target", "value": 0.5}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.autoscaling.metrics import CpuUtilizationTarget
        metric = CpuUtilizationTarget(0.6)
    """
    _METRIC_ID = 1001
    _PARENT_NAME = "cpu_utilization"
    _NAME = "target"


class RequestsPerWorkerTarget(_AutoscalingMetric):
    """
    Number of requests per worker target to trigger autoscaling.

    The JSON equivalent for this (as an element of autoscaling metrics JSON) is:

    .. code-block:: json

        {"metric": "requests_per_worker", "parameters": [{"name": "target", "value": 1000}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.autoscaling.metrics import RequestsPerWorkerTarget
        metric = RequestsPerWorkerTarget(1000)

    """
    _METRIC_ID = 1002
    _PARENT_NAME = "requests_per_worker"
    _NAME = "target"


class MemoryUtilizationTarget(_AutoscalingMetric):
    """
    Memory utilization target to trigger autoscaling.

    The JSON equivalent for this (as an element of autoscaling metrics JSON) is:

    .. code-block:: json

        {"metric": "memory_utilization", "parameters": [{"name": "target", "value": 0.5}]}

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.autoscaling.metrics import MemoryUtilizationTarget
        metric = MemoryUtilizationTarget(0.7)

    """
    _METRIC_ID = 1003
    _PARENT_NAME = "memory_utilization"
    _NAME = "target"
