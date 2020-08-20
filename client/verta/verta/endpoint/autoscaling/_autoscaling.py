# -*- coding: utf-8 -*-
from .metrics import _AutoscalingMetric


class Autoscaling(object):
    """
    Represents autoscaling configuration for Endpoint.

    To be passed to :meth:`Endpoint.update() <verta._deployment.endpoint.Endpoint.update>`.

    The JSON equivalent for this is:

    .. code-block:: json

        {
            "autoscaling": {
                "quantities": {"min_replicas": 2, "max_replicas": 7, "min_scale": 0.2, "max_scale": 0.7},
                "metrics": []
            }
        }

    Parameters
    ----------
    min_replicas : int
        Minimum number of replicas to scale down to.
    max_replicas : int
        Maximum number of replicas to scale up to.
    min_scale : float in (0, 1]
        Minimum growth factor for scaling.
    max_scale : float in (0, 1]
        Maximum growth factor for scaling.

    """
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

    @classmethod
    def _from_dict(cls, autoscaling_dict):
        return cls(
            autoscaling_dict.get("min_replicas"),
            autoscaling_dict.get("max_replicas"),
            autoscaling_dict.get("min_scale"),
            autoscaling_dict.get("max_scale"),
        )

    def add_metric(self, metric):
        """
        Adds a metric.

        Parameters
        ----------
        metric : subclass of :class:`~verta.endpoint.autoscaling.metrics._AutoscalingMetric`
            Metric to add.

        """
        if not isinstance(metric, _AutoscalingMetric):
            raise TypeError("`metric` must be an object from verta.endpoint.autoscaling.metrics")

        self._metrics.append(metric)
