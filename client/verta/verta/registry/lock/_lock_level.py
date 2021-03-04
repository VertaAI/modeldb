import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _LockLevel(object):
    """
    Base class for lock level. Not for external use.

    """

    def __repr__(self):
        return "<{} lock level>".format(self.__class__.__name__)

    @abc.abstractmethod
    def _as_proto(self):
        raise NotImplementedError
