# -*- coding: utf-8 -*-
"""Entities for defining notification channels in the Verta backend."""

from verta._internal_utils import documentation

from ._notification_channel import NotificationChannel, NotificationChannels

documentation.reassign_module(
    [
        NotificationChannel,
        NotificationChannels,
    ],
    module_name=__name__,
)
