# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._internal_utils import _utils, time_utils
from ....._tracking import entity, _Context
from ... import notification_channel
from ... import summaries
from ... import utils
from .. import status


class Alert(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(Alert, self).__init__(
            conn,
            conf,
            _AlertService,
            "alerts",
            msg,
        )

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg
        return "\n\t".join((
            "Alert",
            "name: {}".format(msg.name),
            "id: {}".format(msg.id),
            "monitored entity id: {}".format(msg.monitored_entity_id),
            "status: {}".format(self.status),
            "created: {}".format(
                _utils.timestamp_to_str(msg.created_at_millis)),
            "updated: {}".format(
                _utils.timestamp_to_str(msg.updated_at_millis)),
            "last evaluated: {}".format(
                _utils.timestamp_to_str(msg.last_evaluated_at_millis)),
            "alerter: {}".format(
                # TODO: use an `alerter` property that returns the actual class
                _AlertService.AlerterTypeEnum.AlerterType.Name(msg.alerter_type)),
            "violating summary sample ids: {}".format(
                msg.violating_summary_sample_ids),
            "summary sample query: {}".format(
                self.summary_sample_query),
            "notification channel ids: {}".format(
                msg.notification_channels.keys()),
        ))

    @property
    def history(self):
        # TODO: implement lazy list and pagination
        msg = _AlertService.ListAlertHistoryRequest(id=self.id)
        endpoint = "/api/v1/alerts/listAlertHistory"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        history = self._conn.must_proto_response(response, msg.Response).history
        return list(map(AlertHistoryItem, history))

    @property
    def name(self):
        self._refresh_cache()

        return self._msg.name

    @property
    def status(self):
        self._refresh_cache()

        return status._AlertStatus._from_proto(self._msg.status)

    @property
    def summary_sample_query(self):
        self._refresh_cache()

        return summaries.SummarySampleQuery._from_proto_request(
            self._msg.sample_find_base,
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
        alerter,
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
                alerter_type=alerter._TYPE,
            ),
        )
        if msg.alert.alerter_type == _AlertService.AlerterTypeEnum.FIXED:
            msg.alert.alerter_fixed.CopyFrom(alerter._as_proto())
        elif msg.alert.alerter_type == _AlertService.AlerterTypeEnum.REFERENCE:
            msg.alert.alerter_reference.CopyFrom(alerter._as_proto())
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

    def delete(self):
        msg = _AlertService.DeleteAlertRequest(ids=[self.id])
        endpoint = "/api/v1/alerts/deleteAlert"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class Alerts(object):
    def __init__(self, conn, conf, monitored_entity_id=None):
        self._conn = conn
        self._conf = conf
        self._monitored_entity_id = int(monitored_entity_id) if monitored_entity_id else None

    def create(
        self,
        name,
        alerter,
        summary_sample_query,
        notification_channels=None,
        created_at_millis=None,
        updated_at_millis=None,
        last_evaluated_at_millis=None,
    ):
        if self._monitored_entity_id is None:
            raise RuntimeError(
                "this Alerts cannot be used to create because it was not"
                " obtained via monitored_entity.alerts"
            )

        if notification_channels is None:
            notification_channels = []
        for channel in notification_channels:
            # as opposed to notification_channel._entities.NotificationChannel
            if isinstance(channel, notification_channel._NotificationChannel):
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
            alerter=alerter,
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
        msg = _AlertService.FindAlertRequest()
        if self._monitored_entity_id is not None:
            msg.monitored_entity_ids = [self._monitored_entity_id]
        endpoint = "/api/v1/alerts/findAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        alerts = self._conn.must_proto_response(response, msg.Response).alerts
        return [Alert(self._conn, self._conf, alert) for alert in alerts]

    def delete(self, alerts):
        alert_ids = utils.extract_ids(alerts)
        msg = _AlertService.DeleteAlertRequest(ids=alert_ids)
        endpoint = "/api/v1/alerts/deleteAlert"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class AlertHistoryItem(object):
    def __init__(self, msg):
        self._event_time = time_utils.datetime_from_millis(msg.event_time_millis)
        self._status = status._AlertStatus._from_proto(msg.status)
        self._violating_summary_sample_ids = msg.violating_summary_sample_ids

    def __repr__(self):
        return "\n\t".join(
            (
                "AlertHistoryItem",
                "occurred at: {}".format(self._event_time),
                "status: {}".format(self._status),
                "associated summary sample IDs: {}".format(
                    self._violating_summary_sample_ids
                ),
            )
        )
