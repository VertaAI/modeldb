from ..._protos.public.registry import RegistryService_pb2

from . import _ActionType


class _Unknown(_ActionType):
    """
    Unknown action of the registered model. Not for external use.

    """

    _ACTION_TYPE = RegistryService_pb2.ActionTypeEnum.ActionType.UNKNOWN
