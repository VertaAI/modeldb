# -*- coding: utf-8 -*-

import abc
import re

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _Resource(object):
    def __init__(self, parameter):
        self.parameter = parameter


class CpuMillis(_Resource):
    milli_err_msg = "`cpu_millis` must be int greater than 0"

    def __init__(self, parameter):
        if not isinstance(parameter, int):
            raise TypeError(self.milli_err_msg)
        if parameter <= 0:
            raise ValueError(self.milli_err_msg)
        super(CpuMillis, self).__init__(parameter)

    def to_dict(self):
        return {"cpu_millis": self.parameter}


class Memory(_Resource):
    memory_err_msg = "`memory` must be a string representing a plain integer or a fixed-point integer with suffixes " \
                     "E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki, for example: 128974848, 129e6, 129M, 123Mi "

    def __init__(self, parameter):
        self._validate(parameter)
        super(Memory, self).__init__(parameter)

    def _validate(self, parameter):
        if not isinstance(parameter, str):
            raise TypeError(self.memory_err_msg)
        if not re.match(r'^[0-9]+[e]?[0-9]*[E|P|T|G|M|K]?[i]?$', parameter):
            raise ValueError(self.memory_err_msg)

    def to_dict(self):
        return {"memory": self.parameter}
