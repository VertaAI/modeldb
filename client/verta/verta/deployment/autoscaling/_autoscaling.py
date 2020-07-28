# -*- coding: utf-8 -*-

class Autoscaling(object):
    def __init__(self, min_replicas, max_replicas, min_scale, max_scale):
        raise NotImplementedError

    def _as_json(self):
        raise NotImplementedError

    def add_metric(self, metric):
        raise NotImplementedError
