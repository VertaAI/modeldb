import abc

from ...external import six


@six.add_metaclass(abc.ABCMeta)
class _ActionType(object):
    """
    Base class for action type. Not for external use.

    """

    _ACTION_TYPE = None

    def __repr__(self):
        return "<{} action type>".format(self.__class__.__name__)

    def _as_proto(self):
        return self._ACTION_TYPE

    @staticmethod
    def _from_proto(action_type):
        """
        Parameters
        ----------
        action_type : ``RegistryService_pb2.ActionTypeEnum.ActionType``

        Returns
        -------
        :class:`_ActionType`

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
            Unknown,
        )

        for action_type_cls in (Other, Classification, Clustering, Detection, Regression, Transcription, Translation, Unknown):
            if action_type == action_type_cls._ACTION_TYPE:
                return action_type_cls()
        else:
            raise ValueError(
                "unrecognized action type {}".format(action_type)
            )
