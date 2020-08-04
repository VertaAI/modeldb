# -*- coding: utf-8 -*-
from .metrics import _AutoscalingMetric

class Autoscaling(object):
    def __init__(self, min_replicas=None, max_replicas=None, min_scale=None, max_scale=None):
        self._min_replicas = min_replicas
        self._max_replicas = max_replicas
        self._min_scale = min_scale
        self._max_scale = max_scale
        self._metrics = []

    def _as_dict(self):
        return {
            "quantities": {
                "min_replicas": self._min_replicas,
                "max_replicas": self._max_replicas,
                "min_scale": self._min_scale,
                "max_scale": self._max_scale
            },
            "metrics": list(map(lambda metric: metric._as_dict(), self._metrics))
        }

    def add_metric(self, metric):
        if not isinstance(metric, _AutoscalingMetric):
            raise TypeError("`metric` must be an object from verta.deployment.autoscaling.metrics")

        self._metrics.append(metric)
