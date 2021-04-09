# -*- coding: utf-8 -*-

from verta.operations.monitoring.notification_channel import (
    SlackNotificationChannel,
)
from verta.operations.monitoring.notification_channel import _entities


class TestSlack:
    def test_crud(self, client, strs):
        notification_channels = client.operations.notification_channels
        name, webhook_url = strs[:2]
        slack_channel = SlackNotificationChannel(webhook_url)

        created_channel = notification_channels.create(name, slack_channel)
        assert isinstance(created_channel, _entities.NotificationChannel)
        assert created_channel._msg.type == slack_channel._TYPE

        retrieved_channel = notification_channels.get(id=created_channel.id)
        assert isinstance(retrieved_channel, _entities.NotificationChannel)
        assert retrieved_channel._msg.type == slack_channel._TYPE

        listed_channels = notification_channels.list()
        assert created_channel.id in map(lambda c: c.id, listed_channels)

        assert notification_channels.delete([created_channel])

    def test_repr(self, client, strs):
        """__repr__() does not raise exceptions"""
        notification_channels = client.operations.notification_channels
        name, webhook_url = strs[:2]
        slack_channel = SlackNotificationChannel(webhook_url)

        created_channel = notification_channels.create(name, slack_channel)
        assert repr(created_channel)

        retrieved_channel = notification_channels.get(id=created_channel.id)
        assert repr(retrieved_channel)
