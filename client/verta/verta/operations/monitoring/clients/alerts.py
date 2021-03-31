# -*- coding: utf-8 -*-

from verta._tracking import entity, _Context
from _protos.private.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)
from _protos.private.monitoring.DataMonitoringService_pb2 import (
    GetAlertRequest,
    CreateAlertRequest,
    ListAlertsRequest,
    UpdateAlertRequest,
    DeleteAlertRequest,
)
from utils import now_in_millis


class Alert(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        # NB: the provided `service_url_component` is a no-op and should be ignored
        super(Alert, self).__init__(
            conn, conf, _DataMonitoringService, "monitored_entity", msg
        )

    @classmethod
    def _generate_default_name(cls):
        return "Alert_{}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = GetAlertRequest(id=id)
        endpoint = "/api/v1/monitored_entity/getProfiler"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, GetAlertRequest.Response).alert

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        definition_id,
        triggered_at_millis=None,
        acked_at_millis=None,
        closed_at_millis=None,
    ):
        msg = CreateAlertRequest(
            name=name,
            alert_definition_id=definition_id,
            triggered_at_millis=triggered_at_millis,
            acked_at_millis=acked_at_millis,
            closed_at_millis=closed_at_millis,
        )
        endpoint = "/api/v1/monitored_entity/createAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alert = conn.must_proto_response(response, CreateAlertRequest.Response).alert
        return alert


class Alerts:
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def get(self, alert_id):
        return Alert._get_by_id(self._conn, self._conf, alert_id)

    def list(self):
        endpoint = "/api/v1/monitored_entity/listAlerts"
        response = self._conn.make_proto_request("GET", endpoint)
        alerts = self._conn.must_proto_response(
            response, ListAlertsRequest.Response
        ).alert
        return alerts

    def create(
        self,
        name,
        alert_definition,
        triggered_at_millis=None,
        acked_at_millis=None,
        closed_at_millis=None,
    ):
        """
        defaults triggered_at_millis to 'now' if not supplied
        """
        ctx = _Context(self._conn, self._conf)
        alert_definition_id = alert_definition.id
        triggered_at = triggered_at_millis if triggered_at_millis else now_in_millis()
        return Alert._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            definition_id=alert_definition_id,
            triggered_at_millis=triggered_at_millis,
            acked_at_millis=acked_at_millis,
            closed_at_millis=closed_at_millis,
        )

    def update(
        self,
        alert_id,
        name,
        triggered_at_millis=None,
        acked_at_millis=None,
        closed_at_millis=None,
    ):
        msg = UpdateAlertRequest(
            id=alert_id,
            name=name,
            triggered_at_millis=triggered_at_millis,
            acked_at_millis=acked_at_millis,
            closed_at_millis=closed_at_millis,
        )
        endpoint = "/api/v1/monitored_entity/updateAlert"
        response = self._conn.make_proto_request("PATCH", endpoint, body=msg)
        updated_alert = self._conn.must_proto_response(
            response, UpdateAlertRequest.Response
        ).alert
        return updated_alert

    def delete(self, alert_id):
        msg = DeleteAlertRequest(id=alert_id)
        endpoint = "/api/v1/monitored_entity/deleteAlert"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        if response.ok:
            return True
        else:
            return False  # TODO: raise an exception instead?
