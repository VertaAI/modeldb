# -*- coding: utf-8 -*-

class Autoscaling(object):
    def __init__(self, min_replicas=None, max_replicas=None, min_scale=None, max_scale=None):
        if not isinstance(min_scale, float) or not isinstance(max_scale, float):
            raise TypeError("`min_scale` and `max_scale` must be float.")

        if not isinstance(min_replicas, int) or not isinstance(max_replicas, int):
            raise TypeError("`min_replicas` and `max_replicas` must be int.")

        if min_replicas >= max_replicas:
            raise ValueError("`max_replicas` must be greater than `min_replicas`.")

        if min_scale >= max_scale:
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
        self._metrics.append(metric)
