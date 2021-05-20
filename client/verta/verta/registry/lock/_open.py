from ..._protos.public.registry import RegistryService_pb2

from . import _LockLevel


class Open(_LockLevel):
    """
    Changes to the model version are allowed.

    Examples
    --------
    .. code-block:: python

        from verta.registry import lock
        reg_model = client.create_registered_model("My Model", workspace="my-org")
        model_ver = reg_model.create_version("My Model v0", lock_level=lock.Open())
        # equivalently:
        # reg_model.create_version("My Model v0", lock_level=lock.open)

    """

    _LOCK_LEVEL = RegistryService_pb2.ModelVersionLockLevelEnum.ModelVersionLockLevel.OPEN
