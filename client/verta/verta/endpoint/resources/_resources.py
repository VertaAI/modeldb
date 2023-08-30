# -*- coding: utf-8 -*-
"""Resource configuration for endpoints."""

import abc
import re

from .nvidia_gpu import NvidiaGPU
from verta._vendored import six


class Resources(object):
    """
    `Kubernetes computational resources
    <https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#resource-units-in-kubernetes>`__
    allowed for an endpoint's model, to be passed to :meth:`Endpoint.update() <verta.endpoint.Endpoint.update>`.

    .. versionadded:: 0.24.1
        The `nvidia_gpu` parameter.

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
        Memory allowed for an endpoint's model. Expects the same representation as Kubernetes:

            You can express memory as a plain integer or as a fixed-point integer
            using one of these suffixes: **E, P, T, G, M, K**. You can also use the
            power-of-two equivalents: **Ei, Pi, Ti, Gi, Mi, Ki**. For example, the
            following represent roughly the same value: 128974848, 129e6, 129M, 123Mi.
    nvidia_gpu: :class:`~verta.endpoint.resources.NvidiaGPU`, optional
        Nvidia GPU resources allowed for an endpoint's model.

    Examples
    --------
    .. code-block:: python

        from verta.endpoint.resources import Resources, NvidiaGPU, NvidiaGPUModel
        resources = Resources(cpu=.25, memory="512Mi", nvidia_gpu=NvidiaGPU(1, NvidiaGPUModel.V100)

    """

    CPU_ERR_MSG = "`cpu` must be a number greater than 0"
    MEMORY_ERR_MSG = " ".join(
        [
            "`memory` must be a string representing a plain integer",
            "or a fixed-point integer with suffixes",
            "E, P, T, G, M, K, Ei, Pi, Ti, Gi, Mi, Ki;",
            "for example: 128974848, 129e6, 129M, 123Mi",
        ]
    )

    def __init__(self, cpu=None, memory=None, nvidia_gpu=None):
        if cpu is not None:
            self._validate_cpu(cpu)
        if memory is not None:
            self._validate_memory(memory)
        if nvidia_gpu is not None:
            self._validate_nvidia_gpu(nvidia_gpu)

        self.cpu = cpu
        self.memory = memory
        self.nvidia_gpu = nvidia_gpu

    def _validate_cpu(self, cpu):
        if not isinstance(cpu, (six.integer_types, float)):
            raise TypeError(self.CPU_ERR_MSG)
        if cpu <= 0:
            raise ValueError(self.CPU_ERR_MSG)

    def _validate_memory(self, memory):
        if not isinstance(memory, six.string_types):
            raise TypeError(self.MEMORY_ERR_MSG)
        if not re.match(r"^[0-9]+[e]?[0-9]*[E|P|T|G|M|K]?[i]?$", memory):
            raise ValueError(self.MEMORY_ERR_MSG)

    def _validate_nvidia_gpu(self, nvidia_gpu):
        if not isinstance(nvidia_gpu, NvidiaGPU):
            raise TypeError(
                "`nvidia_gpu` must be an instance of `verta.endpoint.NvidiaGpu`"
            )

    def _as_dict(self):
        d = dict()
        if self.cpu is not None:
            d["cpu_millis"] = int(self.cpu * 1000)
        if self.memory is not None:
            d["memory"] = self.memory
        if self.nvidia_gpu is not None:
            d["nvidia_gpu"] = self.nvidia_gpu._as_dict()

        return d

    @classmethod
    def _from_dict(cls, resources_dict):
        resources_dict = resources_dict.copy()
        if "nvidia_gpu" in resources_dict:
            resources_dict["nvidia_gpu"] = NvidiaGPU._from_dict(
                resources_dict["nvidia_gpu"],
            )
        return cls(**resources_dict)
