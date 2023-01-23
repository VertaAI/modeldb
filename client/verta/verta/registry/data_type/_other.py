from ..._protos.public.registry import RegistryService_pb2

from . import _DataType


class Other(_DataType):
    """
    Other data of the registered model.

    Examples
    --------
    .. code-block:: python

        from verta.registry import data_type
        reg_model = client.create_registered_model("My Model", workspace="my-org", data_type=data_type.Other())

    """

    _DATA_TYPE = RegistryService_pb2.DataTypeEnum.DataType.OTHER
