# -*- coding: utf-8 -*-

import abc

from ....external import six

from ...._protos.public.monitoring import Alert_pb2 as _AlertService


# TODO: move into separate files
@six.add_metaclass(abc.ABCMeta)
class _NotificationChannel(object):
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
    _TYPE = _AlertService.NotificationChannelTypeEnum.SLACK

    def __init__(self, url):
        self._url = url

    def _as_proto(self):
        return _AlertService.NotificationChannelSlackWebhook(
            url=self._url,
        )
