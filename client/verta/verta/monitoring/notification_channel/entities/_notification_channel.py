# -*- coding: utf-8 -*-

import warnings

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta._internal_utils import _utils, arg_handler, time_utils
from verta.tracking import _Context
from verta.tracking.entities import _entity


class NotificationChannel(_entity._ModelDBEntity):
    """
    A notification channel persisted to Verta.

    A notification channel directs a triggered alert to propagate a message to
    some destination to notify interested parties.

    Attributes
    ----------
    id : int
        ID of this notification channel.
    name : str
        Name of this notification channel.
    workspace : str
        Name of the workspace which this notification channel belongs to.

    Examples
    --------
    .. code-block:: python

        from verta.monitoring.notification_channel import SlackNotificationChannel

        channels = Client().monitoring.notification_channels
        channel = notification_channels.create(
            "Slack alerts",
            SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
        )

        alert = summary.alerts.create(
            name="MSE",
            alerter=alerter,
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

    @property
    def name(self):
        self._refresh_cache()

        return self._msg.name

    @property
    def workspace(self):
        # TODO: replace with _refresh_cache() when backend returns ID on /create
        self._fetch_with_no_cache()

        if self._msg.workspace_id:
            return self._conn.get_workspace_name_from_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindNotificationChannelRequest(
            ids=[int(id)],
            page_number=1,
            page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        channels = conn.must_proto_response(response, msg.Response).channels
        if len(channels) > 1:
            warnings.warn(
                "unexpectedly found multiple notification channels with ID"
                " {}".format(id)
            )
        if channels:
            return channels[0]
        else:
            return None

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        msg = _AlertService.FindNotificationChannelRequest(
            names=[name],
            page_number=1,
            page_limit=-1,
            workspace_name=workspace,
        )
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        channels = conn.must_proto_response(response, msg.Response).channels
        if len(channels) > 1:
            warnings.warn(
                "unexpectedly found multiple notification channels with name"
                " {} in workspace {}".format(name, workspace)
            )
        if channels:
            return channels[0]
        else:
            return None

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        channel,
        workspace,
        created_at_millis,
        updated_at_millis,
    ):
        msg = _AlertService.CreateNotificationChannelRequest(
            name=name,
            created_at_millis=created_at_millis,
            updated_at_millis=updated_at_millis,
            workspace_name=workspace,
            type=channel._TYPE,
        )
        if msg.type == _AlertService.NotificationChannelTypeEnum.SLACK:
            msg.slack_webhook.CopyFrom(channel._as_proto())
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
    Collection object for creating and finding notification channels.

    Examples
    --------
    .. code-block:: python

        channels = Client().monitoring.notification_channels

    """

    def __init__(self, client):
        self._client = client

    @property
    def _conn(self):
        return self._client._conn

    @property
    def _conf(self):
        return self._client._conf

    def create(
        self,
        name,
        channel,
        workspace=None,
        created_at=None,
        updated_at=None,
    ):
        """
        Create a new notification channel.

        Parameters
        ----------
        name : str
            A unique name for this notification channel.
        channel : :mod:`~verta.monitoring.notification_channel`
            The configuration for this notification channel.
        workspace : str, optional
            Workspace in which to create this notification channel. Defaults to
            the client's default workspace.
        created_at : datetime.datetime or int, optional
            An override creation time to assign to this channel. Either a
            timezone aware datetime object or unix epoch milliseconds.
        updated_at : datetime.datetime or int, optional
            An override update time to assign to this channel. Either a
            timezone aware datetime object or unix epoch milliseconds.

        Returns
        -------
        :class:`NotificationChannel`
            Notification channel.

        Examples
        --------
        .. code-block:: python

            from verta.monitoring.notification_channel import SlackNotificationChannel

            channels = Client().monitoring.notification_channels

            channel = notification_channels.create(
                "Slack alerts",
                SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
            )

        """
        if workspace is None:
            workspace = self._client.get_workspace()

        ctx = _Context(self._conn, self._conf)
        return NotificationChannel._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            channel=channel,
            workspace=workspace,
            created_at_millis=time_utils.epoch_millis(created_at),
            updated_at_millis=time_utils.epoch_millis(updated_at),
        )

    def get(self, name=None, workspace=None, id=None):
        """
        Get an existing notification channel.

        Either `name` or `id` can be provided but not both.

        Parameters
        ----------
        name : str, optional
            Notification channel name.
        workspace : str, optional
            Workspace in which the notification channel exists. Defaults to the
            client's default workspace.
        id : int, optional
            Notification channel ID.

        Returns
        -------
        :class:`NotificationChannel`
            Notification channel.

        """
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        if workspace and id:
            raise ValueError(
                "cannot specify both `workspace` and `id`;"
                " getting by ID does not require a workspace name"
            )
        elif name:
            if workspace is None:
                workspace = self._client.get_workspace()

            return NotificationChannel._get_by_name(
                self._conn,
                self._conf,
                name,
                workspace,
            )
        elif id:
            return NotificationChannel._get_by_id(self._conn, self._conf, id)
        else:
            raise ValueError("must specify either `name` or `id`")

    def get_or_create(
        self,
        name=None,
        channel=None,
        workspace=None,
        created_at=None,
        updated_at=None,
        id=None,
    ):
        """Get or create a notification channel by name.

        Either `name` or `id` can be provided but not both. If `id` is
        provided, this will act only as a get method and no object will be
        created.

        Parameters
        ----------
        name : str, optional
            A unique name for this notification channel.
        channel : :mod:`~verta.monitoring.notification_channel`, optional
            The configuration for this notification channel.
        workspace : str, optional
            Workspace in which to create this notification channel. Defaults to
            the client's default workspace.
        created_at : datetime.datetime or int, optional
            An override creation time to assign to this channel. Either a
            timezone aware datetime object or unix epoch milliseconds.
        updated_at : datetime.datetime or int, optional
            An override update time to assign to this channel. Either a
            timezone aware datetime object or unix epoch milliseconds.
        id : int, optional
            Notification channel ID. This should not be provided if `name`
            is provided.

        Returns
        -------
        :class:`NotificationChannel`
            Notification channel.

        Examples
        --------
        .. code-block:: python

            from verta.monitoring.notification_channel import SlackNotificationChannel

            channels = Client().monitoring.notification_channels

            channel = notification_channels.get_or_create(
                "Slack alerts",
                SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
            )

            # get it back later with the same method
            channel = notification_channels.get_or_create(
                "Slack alerts",
            )

        """
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        if workspace and id:
            raise ValueError(
                "cannot specify both `workspace` and `id`;"
                " getting by ID does not require a workspace name"
            )

        name = self._client._set_from_config_if_none(name, "notification_channel")
        if workspace is None:
            workspace = self._client.get_workspace()

        resource_name = "Notification Channel"
        param_names = "`channel`, `created_at`, or `updated_at`"
        params = (channel, created_at, updated_at)
        if id is not None:
            channel = NotificationChannel._get_by_id(self._conn, self._conf, id)
            _utils.check_unnecessary_params_warning(
                resource_name,
                "id {}".format(id),
                param_names,
                params,
            )
        else:
            channel = NotificationChannel._get_or_create_by_name(
                self._conn,
                name,
                lambda name: NotificationChannel._get_by_name(
                    self._conn,
                    self._conf,
                    name,
                    workspace,
                ),
                lambda name: NotificationChannel._create(
                    self._conn,
                    self._conf,
                    _Context(self._conn, self._conf),
                    name=name,
                    channel=channel,
                    workspace=workspace,
                    created_at_millis=time_utils.epoch_millis(created_at),
                    updated_at_millis=time_utils.epoch_millis(updated_at),
                ),
                lambda: _utils.check_unnecessary_params_warning(
                    resource_name,
                    "name {}".format(name),
                    param_names,
                    params,
                ),
            )

        return channel

    # TODO: use lazy list and pagination
    # TODO: a proper find
    def list(self, workspace=None):
        """
        Return accesible notification channels.

        Parameters
        ----------
        workspace : str, optional
            Workspace from which to list notification channels. Defaults to the
            client's default workspace.

        Returns
        -------
        list of :class:`NotificationChannel`
            Notification channels.

        """
        msg = _AlertService.FindNotificationChannelRequest(
            page_number=1,
            page_limit=-1,
            workspace_name=workspace,
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
        channels : list of :class:`NotificationChannel`
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
        channel_ids = arg_handler.extract_ids(channels)
        msg = _AlertService.DeleteNotificationChannelRequest(ids=channel_ids)
        endpoint = "/api/v1/alerts/deleteNotificationChannel"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
