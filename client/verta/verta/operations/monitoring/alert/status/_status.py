# -*- coding: utf-8 -*-

import abc

from .....external import six

from ....._protos.public.monitoring import Alert_pb2 as _AlertService


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _AlertStatus(object):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.UNKNOWN

    def __eq__(self, other):
        if not isinstance(other, _AlertStatus):
            return NotImplemented

        return self._ALERT_STATUS == other._ALERT_STATUS

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
        for subcls in cls.__subclasses__():
            if msg == subcls._ALERT_STATUS:
                return subcls()

        raise ValueError("alert status {} not recognized".format(msg))


class Alerting(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.ALERTING


class Ok(_AlertStatus):
    _ALERT_STATUS = _AlertService.AlertStatusEnum.OK
