# -*- coding: utf-8 -*-

import abc
import re

from ..external import six


class Resources(object):
    """
    Computational resources allowed for an endpoint's model, to be passed to
    :meth:`Endpoint.update() <verta._deployment.endpoint.Endpoint.update>`.

    Verta uses the same representation for memory `as Kubernetes
    <https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#meaning-of-memory>`__:

        You can express memory as a plain integer or as a fixed-point integer
        using one of these suffixes: **E, P, T, G, M, K**. You can also use the
        power-of-two equivalents: **Ei, Pi, Ti, Gi, Mi, Ki**. For example, the
        following represent roughly the same value: 128974848, 129e6, 129M, 123Mi.

    The JSON equivalent for this is:

    .. code-block:: json

        {
            "resources": {"cpu_millis": 250, "memory": "512Mi"}
        }

    Parameters
    ----------
    cpu_millis : int > 0
        CPU allowed for an endpoint's model. 1000 CPU millis is equivalent to one CPU core.
    memory : str
        Memory allows for an endpoint's model.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.resources import Resources
        resources = Resources(cpu_millis=250, memory="512Mi")

    """
    CPU_ERR_MSG = "`cpu_millis` must be int greater than 0"
    MEMORY_ERR_MSG = ' '.join([
        "`memory` must be a string representing a plain integer",
        "or a fixed-point integer with suffixes",
        "E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki;",
        "for example: 128974848, 129e6, 129M, 123Mi",
    ])

    def __init__(self, cpu_millis=None, memory=None):
        if cpu_millis is not None:
            self._validate_cpu(cpu_millis)
        if memory is not None:
            self._validate_memory(memory)

        self.cpu_millis = cpu_millis
        self.memory = memory

    def _validate_cpu(self, cpu_millis):
        if not isinstance(cpu_millis, int):
            raise TypeError(self.CPU_ERR_MSG)
        if cpu_millis <= 0:
            raise ValueError(self.CPU_ERR_MSG)

    def _validate_memory(self, memory):
        if not isinstance(memory, six.string_types):
            raise TypeError(self.MEMORY_ERR_MSG)
        if not re.match(r'^[0-9]+[e]?[0-9]*[E|P|T|G|M|K]?[i]?$', memory):
            raise ValueError(self.MEMORY_ERR_MSG)

    def _as_dict(self):
        return {
            'cpu_millis': self.cpu_millis,
            'memory': self.memory,
        }

    @classmethod
    def _from_dict(cls, rule_dict):
        return cls(**rule_dict)
