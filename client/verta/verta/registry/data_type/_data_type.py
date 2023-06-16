import abc

from ..._vendored import six


@six.add_metaclass(abc.ABCMeta)
class _DataType(object):
    """
    Base class for data type. Not for external use.

    """

    _DATA_TYPE = None

    def __eq__(self, other):
        if type(self) is not type(other):
            return NotImplemented
        return self._as_proto() == other._as_proto()

    def __repr__(self):
        return "<{} data type>".format(self.__class__.__name__)

    def _as_proto(self):
        return self._DATA_TYPE

    @staticmethod
    def _from_proto(data_type):
        """
        Parameters
        ----------
        data_type : ``RegistryService_pb2.DataTypeEnum.DataType``

        Returns
        -------
        :class:`_DataType`

        """
        # imports here to avoid circular import in Python 2
        from . import (
            Other,
            Audio,
            Image,
            Tabular,
            Text,
            Video,
            _Unknown,
        )

        for data_type_cls in (Other, Audio, Image, Tabular, Text, Video, _Unknown):
            if data_type == data_type_cls._DATA_TYPE:
                return data_type_cls()
        else:
            raise ValueError("unrecognized data type {}".format(data_type))

    @staticmethod
    def _from_str(data_type_str):
        """
        Parameters
        ----------
        data_type : Data type name.

        Returns
        -------
        :class:`_DataType`

        """
        # imports here to avoid circular import in Python 2
        from . import Other, Audio, Image, Tabular, Text, Video, _Unknown

        for data_type_cls in (Other, Audio, Image, Tabular, Text, Video, _Unknown):
            if data_type_str.lower() == data_type_cls.__name__.lower():
                return data_type_cls()
        else:
            raise ValueError("unrecognized data type {}".format(data_type_str))
