# -*- coding: utf-8 -*-

import datetime

import pytest

from verta._internal_utils import _utils, time_utils
from verta.operations.monitoring.notification_channel import (
    SlackNotificationChannel,
)
from verta.operations.monitoring.notification_channel import _entities


class TestNotificationChannel:
    """Tests that aren't specific to a channel type."""

    def test_creation_datetime(self, client, strs, created_entities):
        strs = iter(strs)
        notification_channels = client.operations.notification_channels

        created_at = time_utils.now() - datetime.timedelta(weeks=1)
        updated_at = time_utils.now() - datetime.timedelta(days=1)
        created_at_millis = time_utils.epoch_millis(created_at)
        updated_at_millis = time_utils.epoch_millis(updated_at)

        # as datetime
        channel = notification_channels.create(
            next(strs),
            SlackNotificationChannel(next(strs)),
            created_at=created_at,
            updated_at=updated_at,
        )
        created_entities.append(channel)
        assert channel._msg.created_at_millis == created_at_millis
        assert channel._msg.updated_at_millis == updated_at_millis

        # as millis
        channel = notification_channels.create(
            next(strs),
            SlackNotificationChannel(next(strs)),
            created_at=created_at_millis,
            updated_at=updated_at_millis,
        )
        created_entities.append(channel)
        assert channel._msg.created_at_millis == created_at_millis
        assert channel._msg.updated_at_millis == updated_at_millis

    def test_crud_workspace(self, client, organization, strs, created_entities):
        strs = iter(strs)
        name = _utils.generate_default_name()
        workspace = organization.name
        notification_channels = client.operations.notification_channels

        personal_channel = notification_channels.create(
            name,
            SlackNotificationChannel(next(strs)),
        )
        created_entities.append(personal_channel)
        assert personal_channel.workspace == client.get_workspace()
        assert personal_channel.id == notification_channels.get(name).id
        listed_channels = notification_channels.list()
        assert personal_channel.id in [c.id for c in listed_channels]

        # same name, different workspace
        org_channel = notification_channels.create(
            name,
            SlackNotificationChannel(next(strs)),
            workspace=workspace,
        )
        created_entities.append(org_channel)
        assert org_channel.workspace == workspace
        with pytest.warns(None) as record:
            assert org_channel.id == notification_channels.get(name, workspace=workspace).id
        assert not record  # no warning of multiple channels found
        listed_channels = notification_channels.list(workspace=workspace)
        assert len(listed_channels) == 1
        assert org_channel.id == listed_channels[0].id


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

    def test_repr(self, client, strs, created_entities):
        """__repr__() does not raise exceptions"""
        notification_channels = client.operations.notification_channels
        name, webhook_url = strs[:2]
        slack_channel = SlackNotificationChannel(webhook_url)

        created_channel = notification_channels.create(name, slack_channel)
        created_entities.append(created_channel)
        assert repr(created_channel)

        retrieved_channel = notification_channels.get(id=created_channel.id)
        assert repr(retrieved_channel)
