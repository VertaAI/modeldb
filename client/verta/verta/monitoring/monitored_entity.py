# -*- coding: utf-8 -*-
"""A entity with associated data summaries and alerts."""

from __future__ import print_function

import warnings

from verta._protos.public.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)

from verta.tracking.entities import _entity
from verta._internal_utils import _utils
from .summaries.summaries import Summaries


class MonitoredEntity(_entity._ModelDBEntity):
    """A monitored entity persisted to Verta.

    A monitored entity provides a named context to gather together data
    summaries and alert configurations. Users should obtain a monitored entity
    through the :meth:`~verta.monitoring.client.Client.get_or_create_monitored_entity`
    method of the monitoring sub-client.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.
    msg
        A protobuf message ai.verta.monitoring.MonitoredEntity

    Attributes
    ----------
    name : str
        The name of this monitored entity.
    workspace : str
        The name of the workspace which this monitored entity belongs to.
    alerts : :class:`~verta.monitoring.alert.entities.Alerts`
        The sub-client for managing alerts defined for this monitored entity.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        client = Client()
        monitored_entity = client.monitoring.get_or_create_monitored_entity()

    """

    def __init__(self, conn, conf, msg):
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
        self._refresh_cache()
        return self._msg.name

    @property
    def workspace(self):
        self._fetch_with_no_cache()

        if self._msg.workspace_id:
            return self._conn.get_workspace_name_from_id(self._msg.workspace_id)
        else:
            return self._conn._OSS_DEFAULT_WORKSPACE

    @property
    def summaries(self):
        return Summaries(self._conn, self._conf)

    @classmethod
    def _generate_default_name(cls):
        return "MonitoredEntity {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        Message = _DataMonitoringService.FindMonitoredEntityRequest
        msg = Message(ids=[id], page_number=1, page_limit=-1)
        endpoint = "/api/v1/monitored_entity/findMonitoredEntity"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        results = conn.maybe_proto_response(response, Message.Response)
        count = results.total_records if results else 0
        if count > 1:
            warnings.warn("found more than one monitored entity with id {}".format(id))
        if results.monitored_entities:
            return results.monitored_entities[0]
        else:
            return None

    @classmethod
    def _get_proto_by_name(cls, conn, name, workspace=None):
        Message = _DataMonitoringService.FindMonitoredEntityRequest
        msg = Message(
            names=[name], workspace_name=workspace, page_number=1, page_limit=-1
        )
        endpoint = "/api/v1/monitored_entity/findMonitoredEntity"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        results = conn.maybe_proto_response(response, Message.Response)
        count = results.total_records if results else 0
        if count > 1:
            warnings.warn(
                "found more than one monitored entity with name {}".format(name)
            )
        if results.monitored_entities:
            return results.monitored_entities[0]
        else:
            return None

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, workspace_name):
        Message = _DataMonitoringService.CreateMonitoredEntityRequest
        msg = Message(name=name, workspace_name=workspace_name)
        endpoint = "/api/v1/monitored_entity/createMonitoredEntity"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        obj = conn.must_proto_response(response, Message.Response).monitored_entity

        if workspace_name is not None:
            WORKSPACE_PRINT_MSG = "workspace: {}".format(workspace_name)
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
