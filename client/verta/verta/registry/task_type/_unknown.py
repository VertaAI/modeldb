from ..._protos.public.registry import RegistryService_pb2

from . import _TaskType


class _Unknown(_TaskType):
    """
    Unknown task of the registered model. Not for external use.

    """

    _TASK_TYPE = RegistryService_pb2.TaskTypeEnum.TaskType.UNKNOWN
