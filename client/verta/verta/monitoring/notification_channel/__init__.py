# -*- coding: utf-8 -*-
"""Notification channels to which to propagate alert messages."""

from verta._internal_utils import documentation

from ._notification_channel import (
    _NotificationChannel,
    SlackNotificationChannel,
)


documentation.reassign_module(
    [SlackNotificationChannel],
    module_name=__name__,
)
