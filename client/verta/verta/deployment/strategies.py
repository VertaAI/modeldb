# -*- coding: utf-8 -*-


class _UpdateStrategy(object):
    _STRATEGY = ""

class DirectUpdateStrategy(_UpdateStrategy):
    _STRATEGY = "rollout"

class CanaryUpdateStrategy(_UpdateStrategy):
    _STRATEGY = "canary"

    def __init__(self):
        raise NotImplementedError
