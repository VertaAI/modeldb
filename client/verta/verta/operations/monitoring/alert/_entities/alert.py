# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._internal_utils import _utils, time_utils
from ....._tracking import entity, _Context
from ... import notification_channel
from ... import summaries
from ... import utils
from .. import status as status_module


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
        return "\n\t".join(
            (
                "Alert",
                "name: {}".format(msg.name),
                "id: {}".format(msg.id),
                "monitored entity id: {}".format(msg.monitored_entity_id),
                "status: {}".format(self.status),
                "created: {}".format(_utils.timestamp_to_str(msg.created_at_millis)),
                "updated: {}".format(_utils.timestamp_to_str(msg.updated_at_millis)),
                "last evaluated: {}".format(
                    _utils.timestamp_to_str(msg.last_evaluated_at_millis)
                ),
                "alerter: {}".format(
                    # TODO: use an `alerter` property that returns the actual class
                    _AlertService.AlerterTypeEnum.AlerterType.Name(msg.alerter_type)
                ),
                "violating summary sample ids: {}".format(
                    msg.violating_summary_sample_ids
                ),
                "summary sample query: {}".format(self.summary_sample_query),
                "notification channel ids: {}".format(
                    list(msg.notification_channels.keys())
                ),
            )
        )

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

        if self._msg.status == status_module.Ok._ALERT_STATUS:
            # violating samples aren't relevant to a retrieved Ok status
            sample_ids = None
        else:
            sample_ids = self._msg.violating_summary_sample_ids


        return status_module._AlertStatus._from_proto(
            self._msg.status,
            sample_ids,
        )

    @property
    def summary_sample_query(self):
        self._refresh_cache()

        return summaries.SummarySampleQuery._from_proto_request(
            self._msg.sample_find_base,
        )

    @staticmethod
    def _validate_notification_channel(channel):
        if not isinstance(channel, notification_channel._entities.NotificationChannel):
            raise TypeError(
                "notification channel must be an entity object returned"
                " from client.notification_channels.create(),"
                " not {}".format(type(channel))
            )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindAlertRequest(
            ids=[int(id)], page_number=1, page_limit=-1,
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
            page_number=1, page_limit=-1,
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
                    channel.id: True for channel in notification_channels
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

    def _update(self, alert_msg):
        msg = _AlertService.UpdateAlertRequest(alert=alert_msg)
        endpoint = "/api/v1/alerts/updateAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()
        return True

    def _update_last_evaluated_at(self, last_evaluated_at=None):
        if last_evaluated_at is None:
            last_evaluated_at = time_utils.now()

        alert_msg = _AlertService.Alert()
        self._fetch_with_no_cache()
        alert_msg.CopyFrom(self._msg)
        alert_msg.last_evaluated_at_millis = time_utils.epoch_millis(last_evaluated_at)

        self._update(alert_msg)

    def add_notification_channels(self, notification_channels):
        for channel in notification_channels:
            self._validate_notification_channel(channel)

        alert_msg = _AlertService.Alert()
        self._fetch_with_no_cache()
        alert_msg.CopyFrom(self._msg)
        alert_msg.notification_channels.update(
            {channel.id: True for channel in notification_channels}
        )

        self._update(alert_msg)

    def set_status(self, status, event_time=None):
        msg = status._to_proto_request()
        msg.alert_id = self.id
        if event_time:
            msg.event_time_millis = time_utils.epoch_millis(event_time)

        endpoint = "/api/v1/alerts/updateAlertStatus"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()
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
        self._monitored_entity_id = (
            int(monitored_entity_id) if monitored_entity_id else None
        )

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
                "this Alert cannot be used to create because it was not"
                " obtained via monitored_entity.alerts"
            )

        if notification_channels is None:
            notification_channels = []
        for channel in notification_channels:
            Alert._validate_notification_channel(channel)

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
        msg = _AlertService.FindAlertRequest(
            page_number=1, page_limit=-1,
        )
        if self._monitored_entity_id is not None:
            msg.monitored_entity_ids.append(self._monitored_entity_id)
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
        self._status = status_module._AlertStatus._from_proto(
            msg.status,
            msg.violating_summary_sample_ids,
        )

    def __repr__(self):
        return "\n\t".join(
            (
                "AlertHistoryItem",
                "occurred at: {}".format(self._event_time),
                "status: {}".format(self._status),
            )
        )
