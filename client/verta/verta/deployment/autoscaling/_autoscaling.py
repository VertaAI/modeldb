# -*- coding: utf-8 -*-
from .metrics import _AutoscalingMetric

class Autoscaling(object):
    def __init__(self, min_replicas=None, max_replicas=None, min_scale=None, max_scale=None):
        if (min_scale is not None and not isinstance(min_scale, float)) or \
                (max_scale is not None and not isinstance(max_scale, float)):
            raise TypeError("`min_scale` and `max_scale` must be float.")

        if (min_replicas is not None and not isinstance(min_replicas, int)) or \
                (max_replicas is not None and not isinstance(max_replicas, int)):
            raise TypeError("`min_replicas` and `max_replicas` must be int.")

        if (min_replicas is not None and min_replicas < 0) or \
                (max_replicas is not None and max_replicas < 0) or \
                (min_scale is not None and min_scale < 0) or \
                (max_scale is not None and max_scale < 0):
            raise ValueError("`min_replicas`, `max_replicas`, `min_scale`, `max_scale` must be non-negative.")

        if min_replicas is not None and max_replicas is not None and min_replicas >= max_replicas:
            raise ValueError("`max_replicas` must be greater than `min_replicas`.")

        if min_scale is not None and max_scale is not None and min_scale >= max_scale:
            raise ValueError("`max_scale` must be greater than `min_scale`.")

        self._min_replicas = min_replicas
        self._max_replicas = max_replicas
        self._min_scale = min_scale
        self._max_scale = max_scale
        self._metrics = []

    def _as_json(self):
        return {
            "quantities": {
                "min_replicas": self._min_replicas,
                "max_replicas": self._max_replicas,
                "min_scale": self._min_scale,
                "max_scale": self._max_scale
            },
            "metrics": list(map(lambda metric: metric._as_json(), self._metrics))
        }

    def add_metric(self, metric):
        if not isinstance(metric, _AutoscalingMetric):
            raise TypeError("`metric` must be an object from verta.deployment.autoscaling.metrics")

        self._metrics.append(metric)
