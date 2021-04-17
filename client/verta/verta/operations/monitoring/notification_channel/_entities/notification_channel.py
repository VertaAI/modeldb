# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._internal_utils import _utils, time_utils
from ....._tracking import entity, _Context
from ... import utils


class NotificationChannel(entity._ModelDBEntity):
    """
    A notification channel persisted to Verta.

    A notification channel directs a triggered alert to propagate a message to
    some destination to notify interested parties.

    Examples
    --------
    .. code-block:: python

        from verta.operations.monitoring.notification_channel import SlackNotificationChannel

        channels = Client().operations.notification_channels
        channel = notification_channels.create(
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

    def __init__(self, conn, conf, msg):
        super(NotificationChannel, self).__init__(
            conn,
            conf,
            _AlertService,
            "alerts",
            msg,
        )

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg
        return "\n\t".join(
            (
                "Notification Channel",
                "name: {}".format(msg.name),
                "id: {}".format(msg.id),
                "created: {}".format(_utils.timestamp_to_str(msg.created_at_millis)),
                "updated: {}".format(_utils.timestamp_to_str(msg.updated_at_millis)),
                "channel: {}".format(
                    # TODO: use a `channel` property that returns the actual class
                    _AlertService.NotificationChannelTypeEnum.NotificationChannelType.Name(
                        msg.type
                    )
                ),
            )
        )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindNotificationChannelRequest(
            ids=[int(id)], page_number=1, page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        channels = conn.must_proto_response(response, msg.Response).channels
        if len(channels) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return channels[0]

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        # NOTE: workspace is currently unsupported until https://vertaai.atlassian.net/browse/VR-9792
        msg = _AlertService.FindNotificationChannelRequest(
            names=[name], page_number=1, page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        channels = conn.must_proto_response(response, msg.Response).channels
        if len(channels) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return channels[0]

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        channel,
        created_at_millis,
        updated_at_millis,
    ):
        msg = _AlertService.CreateNotificationChannelRequest(
            channel=_AlertService.NotificationChannel(
                name=name,
                created_at_millis=created_at_millis,
                updated_at_millis=updated_at_millis,
                type=channel._TYPE,
            )
        )
        if msg.channel.type == _AlertService.NotificationChannelTypeEnum.SLACK:
            msg.channel.slack_webhook.CopyFrom(channel._as_proto())
        else:
            raise ValueError(
                "unrecognized notification channel type enum value {}".format(
                    msg.alert.alerter_type
                )
            )

        endpoint = "/api/v1/alerts/createNotificationChannel"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        notification_channel_msg = conn.must_proto_response(
            response,
            _AlertService.NotificationChannel,
        )
        return notification_channel_msg

    def _update(self):
        raise NotImplementedError

    def delete(self):
        """
        Delete this notification channel.

        Returns
        -------
        bool
            ``True`` if the delete was successful.

        Raises
        ------
        :class:`requests.HTTPError`
            If the delete failed.

        """
        msg = _AlertService.DeleteNotificationChannelRequest(ids=[self.id])
        endpoint = "/api/v1/alerts/deleteNotificationChannel"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class NotificationChannels(object):
    """
    Collection object for creating and funding notification channels.

    Examples
    --------
    .. code-block:: python

        channels = Client().operations.notification_channels

    """

    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def create(
        self,
        name,
        channel,
        created_at=None,
        updated_at=None,
    ):
        """
        Create a new notification channel.

        Parameters
        ----------
        name : str
            A unique name for this notification channel.
        channel : :class:`verta.operations.monitoring.notification_channel._NotificationChannel`
            The configuration for this notification channel.
        created_at : datetime.datetime or int, optional
            An override creation time to assign to this channel. Either a
            timezone aware datetime object or unix epoch milliseconds.
        updated_at : datetime.datetime or int, optional
            An override update time to assign to this channel. Either a
            timezoneaware datetime object or unix epoch milliseconds.

        Returns
        -------
        :class:`NotificationChannel`
            Notification channel.

        Examples
        --------
        .. code-block:: python

            from verta.operations.monitoring.notification_channel import SlackNotificationChannel

            channels = Client().operations.notification_channels

            channel = notification_channels.create(
                "Slack alerts",
                SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
            )

        """
        ctx = _Context(self._conn, self._conf)
        return NotificationChannel._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            channel=channel,
            created_at_millis=time_utils.epoch_millis(created_at),
            updated_at_millis=time_utils.epoch_millis(updated_at),
        )

    def get(self, name=None, id=None):
        """
        Get an existing notification channel.

        Either `name` or `id` can be provided but not both.

        Parameters
        ----------
        name : str, optional
            Notification channel name.
        id : int, optional
            Notification channel ID.

        Returns
        -------
        :class:`NotificationChannel`
            Notification channel.

        """
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        elif name:
            return NotificationChannel._get_by_name(
                self._conn,
                self._conf,
                name,
                None,  # TODO: pass workspace instead of None
            )
        elif id:
            return NotificationChannel._get_by_id(self._conn, self._conf, id)
        else:
            raise ValueError("must specify either `name` or `id`")

    # TODO: use lazy list and pagination
    # TODO: a proper find
    def list(self):
        """
        Return all accesible notification channels.

        Returns
        -------
        list of :class:`NotificationChannel`
            Notification channels.

        """
        msg = _AlertService.FindNotificationChannelRequest(
            page_number=1, page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        channels = self._conn.must_proto_response(response, msg.Response).channels
        return [
            NotificationChannel(self._conn, self._conf, channel) for channel in channels
        ]

    def delete(self, channels):
        """
        Delete the given notification channels in a single request.

        Parameters
        ----------
        list of :class:`NotificationChannel`
            Notification channels.

        Returns
        -------
        bool
            ``True`` if the delete was successful.

        Raises
        ------
        :class:`requests.HTTPError`
            If the delete failed.

        """
        channel_ids = utils.extract_ids(channels)
        msg = _AlertService.DeleteNotificationChannelRequest(ids=channel_ids)
        endpoint = "/api/v1/alerts/deleteNotificationChannel"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
