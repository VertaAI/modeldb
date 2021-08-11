# -*- coding: utf-8 -*-

import abc

from verta.external import six

from verta._protos.public.monitoring import Alert_pb2 as _AlertService

from verta._internal_utils import arg_handler


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _AlertStatus(object):
    """Base class for an alert status. Not for external use."""

    _ALERT_STATUS = _AlertService.AlertStatusEnum.UNKNOWN

    def __init__(self, summary_samples):
        self._sample_ids = arg_handler.extract_ids(summary_samples) if summary_samples else []

    def __eq__(self, other):
        if not isinstance(other, _AlertStatus):
            return NotImplemented

        if self._ALERT_STATUS != other._ALERT_STATUS:
            return False
        if set(self._sample_ids) != set(other._sample_ids):
            return False

        return True

    def __repr__(self):
        return "<alert status \"{}\" (sample IDs {})>".format(
            _AlertService.AlertStatusEnum.AlertStatus.Name(self._ALERT_STATUS).lower(),
            self._sample_ids,
        )

    @classmethod
    def _from_proto(cls, msg, sample_ids=None):
        """
        Return an alert status object.

        Parameters
        ----------
        msg : int
            Variant of ``AlertStatusEnum``.
        sample_ids : list of int, optional
            Summary Sample IDs that triggered the status.

        Returns
        -------
        :class:`_AlertStatus` subclass

        """
        for subcls in cls.__subclasses__():
            if msg == subcls._ALERT_STATUS:
                return subcls(sample_ids)

        raise ValueError("alert status {} not recognized".format(msg))


class Alerting(_AlertStatus):
    """
    Alerting status for an alert.

    Parameters
    ----------
    summary_samples : list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`
        Summary samples that triggered the alert.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.alert.status import Alerting
        alert.set_status(Alerting([monitored_entity]))

    """

    _ALERT_STATUS = _AlertService.AlertStatusEnum.ALERTING

    def _to_proto_request(self):
        return _AlertService.UpdateAlertStatusRequest(
            status=self._ALERT_STATUS,
            alerting_sample_ids=self._sample_ids,
        )


class Ok(_AlertStatus):
    """
    Ok status for an alert.

    Parameters
    ----------
    summary_samples : list of :class:`~verta.monitoring.summaries.summary_sample.SummarySample`, optional
        Summary samples to be removed from the alert's list of violating
        summary samples. If not provided, all summary samples will be cleared
        from the list.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.alert.status import Ok
        alert.set_status(Ok())

    """

    _ALERT_STATUS = _AlertService.AlertStatusEnum.OK

    def __init__(self, summary_samples=None):
        super(Ok, self).__init__(summary_samples=summary_samples)

    def _to_proto_request(self):
        msg = _AlertService.UpdateAlertStatusRequest(
            status=self._ALERT_STATUS,
        )
        if self._sample_ids:
            msg.ok_sample_ids.extend(self._sample_ids)
        else:
            msg.clear_alerting_sample_ids = True

        return msg
