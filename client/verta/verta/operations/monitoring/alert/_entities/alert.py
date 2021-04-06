# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._tracking import entity, _Context
from ...notification_channel import _NotificationChannel


class Alert(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(Alert, self).__init__(
            conn,
            conf,
            _AlertService,
            "alerts",
            msg,
        )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindAlertRequest(
            ids=[int(id)],
        )
        endpoint = "/api/v1/alerts/findAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alerts = conn.must_proto_response(response, msg.Response).alerts
        if len(alerts) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return alerts[0]

    @classmethod
    def _get_proto_by_name(cls, conn, name, monitored_entity_id):
        msg = _AlertService.FindAlertRequest(
            names=[name],
            monitored_entity_ids=[int(monitored_entity_id)],
        )
        endpoint = "/api/v1/alerts/findAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alerts = conn.must_proto_response(response, msg.Response).alerts
        if len(alerts) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return alerts[0]

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        monitored_entity_id,
        alert,
        summary_sample_query,
        notification_channels,
        created_at_millis,
        updated_at_millis,
        last_evaluated_at_millis,
        # TODO: should we allow `status` and `violating samples` on create?
    ):
        msg = _AlertService.CreateAlertRequest(
            alert=_AlertService.Alert(
                name=name,
                monitored_entity_id=monitored_entity_id,
                created_at_millis=created_at_millis,
                updated_at_millis=updated_at_millis,
                last_evaluated_at_millis=last_evaluated_at_millis,
                notification_channels={
                    notification_channel.id: True
                    for notification_channel in notification_channels
                },
                sample_find_base=summary_sample_query._to_proto_request(),
                alerter_type=alert._TYPE,
            ),
        )
        if msg.alert.alerter_type == _AlertService.AlerterTypeEnum.FIXED:
            msg.alert.alerter_fixed.CopyFrom(alert._as_proto())
        elif msg.alert.alerter_type == _AlertService.AlerterTypeEnum.REFERENCE:
            msg.alert.alerter_reference.CopyFrom(alert._as_proto())
        else:
            raise ValueError(
                "unrecognized alert type enum value {}".format(msg.alert.alerter_type)
            )

        endpoint = "/api/v1/alerts/createAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alert_msg = conn.must_proto_response(response, _AlertService.Alert)
        return alert_msg

    def _update(self):
        raise NotImplementedError

    def _update_status(self):
        raise NotImplementedError

    def add_notification_channel(self, notification_channel):
        raise NotImplementedError

    # TODO: alternatively, fire() & resolve()?
    def set_status(self, status, summary_sample=None, event_time_millis=None):
        msg = _AlertService.UpdateAlertStatusRequest(
            alert_id=self.id,
            event_time_millis=event_time_millis,
            status=status._ALERT_STATUS,
            violating_summary_sample_id=summary_sample.id,
        )
        endpoint = "/api/v1/alerts/updateAlertStatus"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class Alerts(object):
    def __init__(self, conn, conf, monitored_entity_id):
        self._conn = conn
        self._conf = conf
        self._monitored_entity_id = int(monitored_entity_id)

    def create(
        self,
        name,
        alert,
        summary_sample_query=None,
        notification_channels=None,
        created_at_millis=None,
        updated_at_millis=None,
        last_evaluated_at_millis=None,
    ):
        for channel in notification_channels:
            if isinstance(channel, _NotificationChannel):
                raise TypeError(
                    "a notification channel must be created in Verta before"
                    " it can be used; please pass the object returned from"
                    " client.notification_channels.create() instead"
                )

        ctx = _Context(self._conn, self._conf)
        return Alert._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            monitored_entity_id=self._monitored_entity_id,
            alert=alert,
            summary_sample_query=summary_sample_query,
            notification_channels=notification_channels,
            created_at_millis=created_at_millis,
            updated_at_millis=updated_at_millis,
            last_evaluated_at_millis=last_evaluated_at_millis,
        )

    def get(self, name=None, id=None):
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        elif name:
            return Alert._get_by_name(
                self._conn,
                self._conf,
                name,
                self._monitored_entity_id,
            )
        elif id:
            return Alert._get_by_id(self._conn, self._conf, id)
        else:
            raise ValueError("must specify either `name` or `id`")

    # TODO: use lazy list and pagination
    # TODO: a proper find
    def list(self):
        msg = _AlertService.FindAlertRequest(
            monitored_entity_ids=[self._monitored_entity_id],
        )
        endpoint = "/api/v1/alerts/findAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        alerts = self._conn.must_proto_response(response, msg.Response).alerts
        return alerts


class AlertHistoryItem(object):
    pass


class AlertHistory(object):
    pass
