import abc

from app.api import _six


@_six.add_metaclass(abc.ABCMeta)
class ABCDeserializer:
    @abc.abstractmethod
    def deserialize(self, filename):
        pass

    @staticmethod
    @abc.abstractmethod
    def deserializer_type():
        # type: (...) -> str
        pass
