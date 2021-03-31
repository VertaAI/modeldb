# -*- coding: utf-8 -*-

from __future__ import print_function

import warnings

from _protos.private.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)

from verta._tracking import entity
from verta._internal_utils import (
    _utils,
)
from clients.alert_definitions import DataSourceAlerts


class DataSource(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(DataSource, self).__init__(
            conn, conf, _DataMonitoringService, "data_source", msg
        )
        self.monitored_entity_id = msg.monitored_entity_id

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return "\n".join(
            (
                "name: {}".format(msg.name),
                "id: {}".format(msg.id),
            )
        )

    @property
    def name(self):
        self._refresh_cache()
        return self._msg.name


    @classmethod
    def _generate_default_name(cls):
        return "DataSource {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _DataMonitoringService.GetDataSourceRequest
        msg = Message(id=id)
        endpoint = "/api/v1/monitored_entity/getDataSource"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, Message.Response).data_source

    @classmethod
    def _get_by_id(cls, conn, conf, id, monitored_entity_id, client):
        by_id = super(DataSource, cls)._get_by_id(conn, conf, id)
        by_id._hydrate(monitored_entity_id, client)
        return by_id


    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        return None

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, monitored_entity_id):
        Message = _DataMonitoringService.CreateDataSourceRequest
        msg = Message(
            name=name,
            data_source_reference="foo",
            monitored_entity_id=monitored_entity_id,
        )

        endpoint = "/api/v1/monitored_entity/createDataSource"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        obj = conn.must_proto_response(response, Message.Response).data_source

        print("created new DataSource: {}".format(obj.name))
        return obj

    @classmethod
    def _create(cls, conn, conf, ctx, client, monitored_entity, *args, **kwargs):
        created = super(DataSource, cls)._create(conn, conf, ctx, *args, monitored_entity_id=monitored_entity.id, **kwargs)
        created._hydrate(monitored_entity, client)
        return created

    def _hydrate(self, monitored_entity, client):
        self._monitored_entity = monitored_entity
        self._client = client
        self.alerts = DataSourceAlerts(client, self)


    def _update(self, msg, response_proto, endpoint, method):
        raise NotImplementedError()

    @classmethod
    def list(self, conn, conf):
        Message = _DataMonitoringService.ListDataSourcesRequest
        msg = Message()
        endpoint = "/api/v1/monitored_entity/listDataSources"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        sources = conn.maybe_proto_response(response, Message.Response).dataSource
        return [DataSource(conn, conf, src) for src in sources]

    def delete(self):
        msg = _DataMonitoringService.DeleteDataSourceRequest(id=self.id)
        endpoint = "/api/v1/monitored_entity/deleteDataSource"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
