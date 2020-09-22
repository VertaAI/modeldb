# -*- coding: utf-8 -*-

import abc
import re

from ..external import six


class Resources(object):
    """
    Computational resources allowed for an endpoint's model, to be passed to
    :meth:`Endpoint.update() <verta.endpoint._endpoint.Endpoint.update>`.

    Verta uses the same representation for memory `as Kubernetes
    <https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#meaning-of-memory>`__:

        You can express memory as a plain integer or as a fixed-point integer
        using one of these suffixes: **E, P, T, G, M, K**. You can also use the
        power-of-two equivalents: **Ei, Pi, Ti, Gi, Mi, Ki**. For example, the
        following represent roughly the same value: 128974848, 129e6, 129M, 123Mi.

    The JSON equivalent for this is:

    .. code-block:: json

        {
            "resources": {"cpu": 0.25, "memory": "512Mi"}
        }

    Parameters
    ----------
    cpu : float > 0
        CPU cores allowed for an endpoint's model.
    memory : str
        Memory allows for an endpoint's model.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.resources import Resources
        resources = Resources(cpu=.25, memory="512Mi")

    """
    CPU_ERR_MSG = "`cpu` must be a number greater than 0"
    MEMORY_ERR_MSG = ' '.join([
        "`memory` must be a string representing a plain integer",
        "or a fixed-point integer with suffixes",
        "E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki;",
        "for example: 128974848, 129e6, 129M, 123Mi",
    ])

    def __init__(self, cpu=None, memory=None):
        if cpu is not None:
            self._validate_cpu(cpu)
        if memory is not None:
            self._validate_memory(memory)

        self.cpu = cpu
        self.memory = memory

    def _validate_cpu(self, cpu):
        if not isinstance(cpu, (six.integer_types, float)):
            raise TypeError(self.CPU_ERR_MSG)
        if cpu <= 0:
            raise ValueError(self.CPU_ERR_MSG)

    def _validate_memory(self, memory):
        if not isinstance(memory, six.string_types):
            raise TypeError(self.MEMORY_ERR_MSG)
        if not re.match(r'^[0-9]+[e]?[0-9]*[E|P|T|G|M|K]?[i]?$', memory):
            raise ValueError(self.MEMORY_ERR_MSG)

    def _as_dict(self):
        d = dict()
        if self.cpu is not None:
            d['cpu_millis'] = int(self.cpu*1000)
        if self.memory is not None:
            d['memory'] = self.memory

        return d

    @classmethod
    def _from_dict(cls, rule_dict):
        return cls(**rule_dict)
