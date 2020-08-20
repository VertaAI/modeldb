# -*- coding: utf-8 -*-

import abc
import re

from ..external import six


@six.add_metaclass(abc.ABCMeta)
class _Resource(object):
    def __init__(self, parameter):
        self.parameter = parameter

    @staticmethod
    def _from_dict(resources_dict):
        # NOTE: Return a list of resources
        resources = []

        RESOURCE_SUBCLASSES = {
            "cpu_millis": CpuMillis,
            "memory": Memory
        }

        for name in resources_dict:
            parameter = resources_dict[name]

            if isinstance(parameter, six.string_types):
                parameter = six.ensure_str(parameter)

            resources.append(RESOURCE_SUBCLASSES[name](parameter))

        return resources


class CpuMillis(_Resource):
    """
    Number of CPU milli allowed for Endpoint. Must be an integer greater than 0.

    """
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
    """
    Amount of memory allowed for Endpoint.

    Must be a string representing a plain integer or a fixed-point integer with one of the following suffixes:
    "E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki."

    For example: "128974848", "129e6", "129M", "123Mi"

    """
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
