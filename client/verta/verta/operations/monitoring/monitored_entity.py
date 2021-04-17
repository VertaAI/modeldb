# -*- coding: utf-8 -*-

from __future__ import print_function

import warnings

from verta._protos.public.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)

from verta._tracking import (entity, _Context)
from verta._internal_utils import (
    _utils,
)
from .alert._entities import Alerts


class MonitoredEntity(entity._ModelDBEntity):
    """A monitored entity persisted to Verta.

    A monitored entity provides a named context to gather together data
    summaries and alert configurations.

    Attributes
    ----------
    name
    workspace
    alerts
    """

    def __init__(self, conn, conf, msg):
        """Intended for internal use only.

        Users should obtain a monitored entity through the
        :meth:`~verta.opertaions.monitoring.client.Client.get_or_create_monitored_entity`
        method of the operations sub-client.

        Parameters
        ----------
        conn
            A connection object to the backend service.
        conf
            A configuration object used by conn methods.
        msg
            A protobuf message ai.verta.monitoring.MonitoredEntity
        Returns
        -------
        MonitoredEntity
            A monitored entity which is persisted by Verta.
        """
        super(MonitoredEntity, self).__init__(
            conn, conf, _DataMonitoringService, "monitored_entity", msg
        )

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
        """The name of this monitored entity.

        Returns
        -------
        string
        """
        self._refresh_cache()
        return self._msg.name

    @property
    def workspace(self):
        """The name of the workspace which this monitored entity belongs to.

        Returns
        -------
        string
        """
        self._refresh_cache()

        if self._msg.workspace_id:
            return self._conn._get_workspace_name_by_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @property
    def alerts(self):
        """The sub-client for managing alerts defined for this monitored entity.

        Returns
        -------
        verta.operations.monitoring.alert._entities.Alerts
        """
        return Alerts(self._conn, self._conf, self.id)

    @classmethod
    def _generate_default_name(cls):
        return "MonitoredEntity {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _DataMonitoringService.GetMonitoredEntityRequest
        msg = Message(id=id)
        endpoint = "/api/v1/monitored_entity/getMonitoredEntity"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, Message.Response).monitored_entity

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace):
        # NOTE: workspace is currently unsupported until https://vertaai.atlassian.net/browse/VR-9792
        Message = _DataMonitoringService.GetMonitoredEntityByNameRequest
        msg = Message(name=name)
        endpoint = "/api/v1/monitored_entity/getMonitoredEntityByName"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, Message.Response).monitored_entity

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name):
        Message = _DataMonitoringService.CreateMonitoredEntityRequest
        msg = Message(name=name)

        endpoint = "/api/v1/monitored_entity/createMonitoredEntity"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        obj = conn.must_proto_response(response, Message.Response).monitored_entity

        if ctx.workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(ctx.workspace_name)
        else:
            WORKSPACE_PRINT_MSG = "personal workspace"

        print(
            "created new MonitoredEntity: {} in {}".format(
                obj.name, WORKSPACE_PRINT_MSG
            )
        )
        return obj

    def _set_client(self, client):
        self._client = client

    def _update(self, msg, response_proto, endpoint, method):
        raise NotImplementedError()


    def delete(self):
        """Delete this monitored entity.

        Instructs Verta's services to delete this monitored entity.

        Returns
        -------
        bool
            True if the delete was successful.

        Raises
        ------
        Error
            Raises an error if the delete failed for any reason.
        """
        msg = _DataMonitoringService.DeleteMonitoredEntityRequest(id=self.id)
        endpoint = "/api/v1/monitored_entity/deleteMonitoredEntity"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
