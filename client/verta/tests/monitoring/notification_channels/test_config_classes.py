# -*- coding: utf-8 -*-

import hypothesis
import hypothesis.strategies as st

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta.monitoring.notification_channel import (
    SlackNotificationChannel,
)


class TestSlack:
    @hypothesis.given(webhook_url=st.text())
    def test_create(self, webhook_url):
        slack_channel = SlackNotificationChannel(webhook_url)
        assert slack_channel._url == webhook_url

        msg = _AlertService.NotificationChannelSlackWebhook(
            url=webhook_url,
        )
        assert slack_channel._as_proto() == msg

    @hypothesis.given(webhook_url=st.text())
    def test_repr(self, webhook_url):
        """__repr__() does not raise exceptions"""
        slack_channel = SlackNotificationChannel(webhook_url)
        assert repr(slack_channel)
