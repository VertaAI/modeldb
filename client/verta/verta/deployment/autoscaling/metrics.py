# -*- coding: utf-8 -*-

import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _AutoscalingMetric(object):
    def __init__(self):
        raise NotImplementedError

    def _as_json(self):
        raise NotImplementedError
