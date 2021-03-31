# -*- coding: utf-8 -*-

from verta._tracking import entity, _Context
from verta._internal_utils._utils import proto_to_json
from _protos.private.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)
from _protos.private.monitoring.DataMonitoringService_pb2 import (
    GetAlertDefinitionRequest,
    CreateAlertDefinitionRequest,
    ListAlertDefinitionsRequest,
    UpdateAlertDefinitionRequest,
    SeverityEnum,
)
from clients.profilers import Profilers

import os


class AlertDefinition(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(AlertDefinition, self).__init__(
            conn, conf, _DataMonitoringService, "monitored_entity", msg
        )
        self._hydrate(msg)

    def _hydrate(self, msg):
        self.name = msg.name
        self.source_profiler_id = msg.source_profiler_id
        self.threshold = msg.threshold
        self.severity = msg.severity
        self.webhook = msg.webhook
        self.reference_data_source_id = msg.reference_data_source_id
        self.source_profiler_id = msg.source_profiler_id
        self.monitored_entity_id = msg.monitored_entity_id

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = GetAlertDefinitionRequest(id=id)
        endpoint = "/api/v1/monitored_entity/getAlertDefinition"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(
            response, GetAlertDefinitionRequest.Response
        ).alert_definition

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        monitored_entity_id,
        reference_data_source_id,
        source_profiler_id,
        threshold,
        severity,
        webhook,
    ):
        msg = CreateAlertDefinitionRequest(
            name=name,
            monitored_entity_id=monitored_entity_id,
            reference_data_source_id=reference_data_source_id,
            source_profiler_id=source_profiler_id,
            threshold=threshold,
            severity=severity,
            webhook=webhook,
        )
        endpoint = "/api/v1/monitored_entity/createAlertDefinition"
        print("inspecting msg json: {}".format(proto_to_json(msg)))
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alert_definition = conn.must_proto_response(
            response, CreateAlertDefinitionRequest.Response
        ).alert_definition
        return alert_definition

    def _set_monitored_entity(self, monitored_entity):
        self._monitored_entity = monitored_entity

    def _set_internal_profiler(self, profiler):
        self._profiler = profiler

    def enable(self, reference_summary, environment={}, wait=False):
        default_environment = {
          "PROFILER_ID": str(self.source_profiler_id),
          "MONITORED_ENTITY_ID": str(self._monitored_entity.id),
          "DATA_SOURCE_ID": str(reference_summary.data_source_id),
          "REFERENCE_SUMMARY_ID": str(reference_summary.id),
          "SUMMARY_NAME": reference_summary.name,
          "ALERT_DEFINITION_ID": str(self.id),
          "VERTA_EMAIL": os.environ["VERTA_EMAIL"],
          "VERTA_DEV_KEY": os.environ["VERTA_DEV_KEY"]
        }

        # TODO: Make Python 2 compatible.
        alert_environment = {**default_environment, **environment}
        return self._profiler.enable(self._monitored_entity, alert_environment, wait=wait)

    def disable(self):
        return self._profiler.disable(self._monitored_entity)

    def status(self):
        return self._profiler.get_status(self._monitored_entity)

    def update(
        self,
        name=None,
        reference_data_source=None,
        source_profiler_id=None,
        threshold=None,
        severity=None,
        webhook=None,
    ):
        monitored_entity_id = self.monitored_entity_id

        name = name if name else self.name
        reference_data_source_id = reference_data_source.id if reference_data_source else self.reference_data_source_id
        threshold = threshold if threshold else self.threshold
        severity = severity if severity else self.severity
        webhook = webhook if webhook else self.webhook
        source_profiler_id = source_profiler_id if source_profiler_id else self.source_profiler_id

        msg = UpdateAlertDefinitionRequest(
            id=self.id,
            name=name,
            reference_data_source_id=reference_data_source_id,
            threshold=threshold,
            severity=severity,
            webhook=webhook,
            source_profiler_id=source_profiler_id,
        )
        endpoint = "/api/v1/monitored_entity/updateAlertDefinition"
        response = self._conn.make_proto_request("PATCH", endpoint, body=msg)
        updated_alert_definition = self._conn.must_proto_response(
            response, UpdateAlertDefinitionRequest.Response
        ).alert_definition
        self._hydrate(updated_alert_definition)
        return self


class DataSourceAlerts:

    def __init__(self, client, data_source):
        self._client = client
        self._data_source = data_source

    def upload(self, name, alerter, environment=Profilers.DEFAULT_ENVIRONMENT, threshold=float(0.5), severity=SeverityEnum.MEDIUM, webhook=""):
        monitored_entity = self._data_source._monitored_entity
        reference_data_source = self._data_source

        alert_definition = self._client.alert_definitions.create(name, monitored_entity, reference_data_source, threshold=threshold, severity=severity, webhook=webhook)

        attrs = {'data_source_id': reference_data_source.id, 'alert_definition_id': alert_definition.id, 'type':'alert evaluator'}
        alert_profiler = self._client.profilers.upload(name, alerter, attrs=attrs)

        alert_definition.update(source_profiler_id=alert_profiler.id)
        alert_definition._set_internal_profiler(alert_profiler)
        alert_definition._set_monitored_entity(monitored_entity)

        return alert_definition

    def _create_definition(
            self,
            name,
            monitored_entity_id,
            reference_data_source_id,
            source_profiler_id,
            threshold=float(0.5),
            severity=SeverityEnum.MEDIUM,
            webhook="",
        ):
        ctx = _Context(self._client._conn, self._client._conf)
        return AlertDefinition._create(
            self._client._conn,
            self._client._conf,
            ctx,
            name=name,
            monitored_entity=monitored_entity_id,
            reference_data_source=reference_data_source_id,
            source_profiler_id=source_profiler_id,
            threshold=threshold,
            severity=severity,
            webhook=webhook,
        )




class AlertDefinitions:
    def __init__(self, conn, conf):
        self._conn = conn
        self._conf = conf

    def get(self, alert_definition_id):
        return AlertDefinition._get_by_id(self._conn, self._conf, alert_definition_id)

    def list(self):
        endpoint = "/api/v1/monitored_entity/listAlertDefinitions"
        response = self._conn.make_proto_request("GET", endpoint)
        alert_definitions = self._conn.must_proto_response(
            response, ListAlertDefinitionsRequest.Response
        ).alert_definition
        return alert_definitions

    def create(
        self,
        name,
        monitored_entity,
        reference_data_source,
        threshold=float(0.5),
        severity=SeverityEnum.MEDIUM,
        webhook="",
    ):
        ctx = _Context(self._conn, self._conf)
        monitored_entity_id = monitored_entity.id

        reference_data_source_id = reference_data_source.id

        return AlertDefinition._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            monitored_entity_id=monitored_entity_id,
            reference_data_source_id=reference_data_source_id,
            source_profiler_id=0,
            threshold=threshold,
            severity=severity,
            webhook=webhook,
        )

    def delete(self, alert_definition):
        msg = DeleteAlertDefinitionRequest(id=alert_definition.id)
        endpoint = "/api/v1/monitored_entity/deleteAlertDefinition"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        if response.ok:
            return True
        else:
            return False  # TODO: raise an exception instead?
