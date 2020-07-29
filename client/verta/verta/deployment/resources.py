# -*- coding: utf-8 -*-

import abc
import re

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _Resource(object):
    def __init__(self, parameter):
        self.parameter = parameter


class CpuMilli(_Resource):
    def __init__(self, parameter):
        try:
            if parameter <= 0:
                raise ValueError('Wrong parameter provided')
        except TypeError:
            raise ValueError('Wrong parameter type provided')
        super(CpuMilli, self).__init__(parameter)

    def to_dict(self):
        return {"cpu_millis": self.parameter}


class Memory(_Resource):
    def __init__(self, parameter):
        if not self._validate(parameter):
            raise ValueError('Wrong parameter provided')
        super(Memory, self).__init__(parameter)

    def _validate(self, parameter):
        # one of these: 128974848, 129e6, 129M, 123Mi
        return isinstance(parameter, str) and re.match(r'^[0-9]+[e]?[0-9]*[E|P|T|G|M|K]?[i]?$', parameter)

    def to_dict(self):
        return {"memory": self.parameter}
