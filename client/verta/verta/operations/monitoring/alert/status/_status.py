# -*- coding: utf-8 -*-

import abc

from verta.external import six

from verta._protos.public.monitoring import Alert_pb2 as _AlertService

from ... import utils


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _AlertStatus(object):
    """
    Base class for alert status. Not for external use.

    """

    _ALERT_STATUS = _AlertService.AlertStatusEnum.UNKNOWN

    def __init__(self, summary_samples):
        self._sample_ids = utils.extract_ids(summary_samples) if summary_samples else []

    def __eq__(self, other):
        if not isinstance(other, _AlertStatus):
            return NotImplemented

        if self._ALERT_STATUS != other._ALERT_STATUS:
            return False
        if set(self._sample_ids) != set(other._sample_ids):
            return False

        return True

    def __repr__(self):
        return "<{} alert status>".format(
            _AlertService.AlertStatusEnum.AlertStatus.Name(self._ALERT_STATUS).lower()
        )


class Alerting(_AlertStatus):
    """
    Alerting status for an alert.

    Parameters
    ----------
    summary_samples : list of :class:`~verta.operations.monitoring.summary.SummarySample`
        Summary samples that triggered the alert.

    Examples
    --------
    .. code-block:: python

        from verta.operations.monitoring.alert.status import Alerting
        alert.set_status(Alerting([monitored_entity]))

    """

    _ALERT_STATUS = _AlertService.AlertStatusEnum.ALERTING


class Ok(_AlertStatus):
    """
    Ok status for an alert.

    Parameters
    ----------
    summary_samples : list of :class:`~verta.operations.monitoring.summary.SummarySample`, optional
        Summary samples to be removed from the alert's list of violating
        summary samples. If not provided, all summary samples will be cleared
        from the list.

    Examples
    --------
    .. code-block:: python

        from verta.operations.monitoring.alert.status import Ok
        alert.set_status(Ok())

    """

    _ALERT_STATUS = _AlertService.AlertStatusEnum.OK

    def __init__(self, summary_samples=None):
        super(Ok, self).__init__(summary_samples=summary_samples)
