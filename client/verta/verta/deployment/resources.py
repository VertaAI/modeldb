# -*- coding: utf-8 -*-

import abc

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _Resource(object):
    raise NotImplementedError


class CpuMilli(_Resource):
    raise NotImplementedError


class Memory(_Resource):
    raise NotImplementedError
