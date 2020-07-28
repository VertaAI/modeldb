# -*- coding: utf-8 -*-

class Autoscaling(object):
    def __init__(self, min_replicas=None, max_replicas=None, min_scale=None, max_scale=None):
        raise NotImplementedError

    def _as_json(self):
        raise NotImplementedError

    def add_metric(self, metric):
        raise NotImplementedError
