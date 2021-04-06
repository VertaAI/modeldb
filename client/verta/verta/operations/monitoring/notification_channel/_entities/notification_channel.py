# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._tracking import entity, _Context


class NotificationChannel(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(NotificationChannel, self).__init__(
            conn,
            conf,
            _AlertService,
            "alerts",
            msg,
        )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindNotificationChannelRequest(
            ids=[int(id)],
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
            names=[name],
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


class NotificationChannels(object):
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def create(
        self,
        name,
        channel,
        created_at_millis=None,
        updated_at_millis=None,
    ):
        ctx = _Context(self._conn, self._conf)
        return NotificationChannel._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            channel=channel,
            created_at_millis=created_at_millis,
            updated_at_millis=updated_at_millis,
        )

    def get(self, name=None, id=None):
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
        msg = _AlertService.FindNotificationChannelsRequest()
        endpoint = "/api/v1/alerts/findNotificationChannel"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        channels = self._conn.must_proto_response(response, msg.Response).channels
        return channels
