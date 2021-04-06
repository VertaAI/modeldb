# -*- coding: utf-8 -*-

import abc

from .....external import six

from ....._protos.public.monitoring import Alert_pb2 as _AlertService


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _AlertStatus(object):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.UNKNOWN

    def __repr__(self):
        return "<{} alert status>".format(
            _AlertService.AlertStatusEnum.AlertStatus.Name(self._ALERT_STATUS).lower()
        )

    @classmethod
    def _from_proto(cls, msg):
        """
        Returns an alert status object.

        Parameters
        ----------
        msg : int
            Variant of ``AlertStatusEnum``.

        Returns
        -------
        :class:`_AlertStatus` subclass

        """
        SUBCLASSES = [
            Alerting,
            Ok,
        ]

        for Subclass in SUBCLASSES:
            if msg == Subclass._ALERT_STATUS:
                return Subclass()

        raise ValueError("alert status {} not recognized".format(msg))


class Alerting(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.ALERTING


class Ok(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.OK
