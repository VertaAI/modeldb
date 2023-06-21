import abc

from ..._vendored import six


@six.add_metaclass(abc.ABCMeta)
class _TaskType(object):
    """
    Base class for task type. Not for external use.

    """

    _TASK_TYPE = None

    def __eq__(self, other):
        if type(self) is not type(other):
            return NotImplemented
        return self._as_proto() == other._as_proto()

    def __repr__(self):
        return "<{} task type>".format(self.__class__.__name__)

    def _as_proto(self):
        return self._TASK_TYPE

    @staticmethod
    def _from_proto(task_type):
        """
        Parameters
        ----------
        task_type : ``RegistryService_pb2.TaskTypeEnum.TaskType``

        Returns
        -------
        :class:`_TaskType`

        """
        # imports here to avoid circular import in Python 2
        from . import (
            Other,
            Classification,
            Clustering,
            Detection,
            Regression,
            Transcription,
            Translation,
            _Unknown,
        )

        for task_type_cls in (
            Other,
            Classification,
            Clustering,
            Detection,
            Regression,
            Transcription,
            Translation,
            _Unknown,
        ):
            if task_type == task_type_cls._TASK_TYPE:
                return task_type_cls()
        else:
            raise ValueError("unrecognized task type {}".format(task_type))

    @staticmethod
    def _from_str(task_type_str):
        """
        Parameters
        ----------
        task_type : Task type name.

        Returns
        -------
        :class:`_TaskType`

        """
        # imports here to avoid circular import in Python 2
        from . import (
            Other,
            Classification,
            Clustering,
            Detection,
            Regression,
            Transcription,
            Translation,
            _Unknown,
        )

        for task_type_cls in (
            Other,
            Classification,
            Clustering,
            Detection,
            Regression,
            Transcription,
            Translation,
            _Unknown,
        ):
            if task_type_str.lower() == task_type_cls.__name__.lower():
                return task_type_cls()
        else:
            raise ValueError("unrecognized task type {}".format(task_type_str))
