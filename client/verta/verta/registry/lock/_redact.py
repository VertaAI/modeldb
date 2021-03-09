from ..._protos.public.registry import RegistryService_pb2

from . import _LockLevel


class Redact(_LockLevel):
    """
    Special permissions are required to make changes to the model version.

    Examples
    --------
    .. code-block:: python

        from verta.registry import lock
        reg_model = client.create_registered_model("My Model", workspace="my-org")
        model_ver = reg_model.create_version("My Model v0", lock_level=lock.Redact())
        # equivalently:
        # reg_model.create_version("My Model v0", lock_level=lock.redact)

    """

    _LOCK_LEVEL = RegistryService_pb2.ModelVersionLockLevelEnum.ModelVersionLockLevel.REDACT
