import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _LockLevel(object):
    """
    Base class for lock level. Not for external use.

    """

    _LOCK_LEVEL = None

    def __repr__(self):
        return "<{} lock level>".format(self.__class__.__name__)

    def _as_proto(self):
        return self._LOCK_LEVEL

    @staticmethod
    def _from_proto(lock_level):
        """
        Parameters
        ----------
        lock_level : ``RegistryService_pb2.ModelVersionLockLevelEnum.ModelVersionLockLevel``

        Returns
        -------
        :class:`_LockLevel`

        """
        # imports here to avoid circular import in Python 2
        from . import (
            Closed,
            Open,
            Redact,
        )

        for lock_level_cls in (Closed, Open, Redact):
            if lock_level == lock_level_cls._LOCK_LEVEL:
                return lock_level_cls()
        else:
            raise ValueError(
                "unrecognized lock level {}".format(lock_level)
            )
