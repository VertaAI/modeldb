# -*- coding: utf-8 -*-

from verta._internal_utils import _utils


class BuildHardwareCompatibilityNvidiaGPU:
    def __init__(self, T4=None, V100=None, all=None):
        self.T4 = T4
        self.V100 = V100
        self.all = all


class BuildHardwareCompatibility:
    """The hardware compatibility of a Verta model build.

    .. versionadded:: 0.24.1

    There should not be a need to instantiate this class directly; please use
    :meth:`Build.get_hardware_compatibility() <verta.endpoint.build.Build.get_hardware_compatibility>` instead.

    .. note::

        ``BuildHardwareCompatibility`` objects do not currently fetch live information from the
        backend; new objects must be obtained from
        :meth:`Build.get_hardware_compatibility() <verta.endpoint.build.Build.get_hardware_compatibility>` to get
        up-to-date hardware compatibility information.

    Attributes
    ----------
    nvidia_gpu: :class:`BuildHardwareCompatibilityNvidiaGPU`, Optional
    x86_64: bool, Optional

    """

    def __init__(self, json):
        self._json = json

    @classmethod
    def _get(cls, conn: _utils.Connection, build_id: int):
        url = f"{conn.scheme}://{conn.socket}/api/v1/deployment/builds/{build_id}"
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)

        hardware_compatibility = response.json()["creator_request"].get("hardware_compatibility")
        if hardware_compatibility is None:
            return None

        return cls(hardware_compatibility)

    def _as_dict(self):
        return self._json

    @property
    def nvidia_gpu(self):
        ngpu = self._json.get("nvidia_gpu")
        if ngpu is None:
            return None
        return BuildHardwareCompatibilityNvidiaGPU(**ngpu)

    @property
    def x86_64(self):
        return self._json.get("x86_64")
