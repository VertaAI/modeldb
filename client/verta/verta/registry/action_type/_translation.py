from ..._protos.public.registry import RegistryService_pb2

from . import _ActionType


class Translation(_ActionType):
    """
    Translation action of the registered model.

    Examples
    --------
    .. code-block:: python

        from verta.registry import action_type
        reg_model = client.create_registered_model("My Model", workspace="my-org", action_type=action_type.Translation())

    """

    _ACTION_TYPE = RegistryService_pb2.ActionTypeEnum.ActionType.TRANSLATION
