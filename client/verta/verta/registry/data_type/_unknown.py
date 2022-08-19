from ..._protos.public.registry import RegistryService_pb2

from . import _DataType


class _Unknown(_DataType):
    """
    Unknown data of the registered model. Not for external use.

    """

    _DATA_TYPE = RegistryService_pb2.DataTypeEnum.DataType.UNKNOWN
