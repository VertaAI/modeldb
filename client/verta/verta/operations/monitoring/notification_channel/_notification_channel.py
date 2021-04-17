# -*- coding: utf-8 -*-

import abc

from ....external import six

from ...._protos.public.monitoring import Alert_pb2 as _AlertService


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _NotificationChannel(object):
    """Base class for a notification channel. Not for external use."""

    _TYPE = _AlertService.NotificationChannelTypeEnum.UNKNOWN

    def __repr__(self):
        return "<{} notification channel>".format(
            _AlertService.NotificationChannelTypeEnum.NotificationChannelType.Name(
                self._TYPE
            ).lower()
        )

    @abc.abstractmethod
    def _as_proto(self):
        raise NotImplementedError


class SlackNotificationChannel(_NotificationChannel):
    """
    A Slack notification channel.

    Parameters
    ----------
    url : str
        Slack webhook URL.

    Examples
    --------
    .. code-block:: python

        from verta.operations.monitoring.notification_channel import SlackNotificationChannel

        channels = Client().operations.notification_channels
        channel = channels.create(
            "Slack alerts",
            SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
        )

        alert = monitored_entity.alerts.create(
            name="MSE",
            alerter=alerter,
            summary_sample_query=sample_query,
            notification_channels=[channel],
        )

    """

    _TYPE = _AlertService.NotificationChannelTypeEnum.SLACK

    def __init__(self, url):
        self._url = url

    def _as_proto(self):
        return _AlertService.NotificationChannelSlackWebhook(
            url=self._url,
        )
