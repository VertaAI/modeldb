# -*- coding: utf-8 -*-
from typing import Any, Dict, List, Optional, Union

from google.protobuf.internal.containers import RepeatedCompositeFieldContainer

from verta._internal_utils import _monitoring_utils
from verta._protos.public.monitoring import MonitoredEntity_pb2 as _Monitor
from verta._internal_utils._utils import Connection

class Monitor(object):
    """
    Object representing the configuration for monitoring a given model/endpoint.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.create_monitor() <verta.Client.create_monitor>`

    To fetch a list of existing monitors, use
    :meth:`Client.find_monitors() <verta.Client.find_monitors>

    .. versionadded:: 0.22.2
        Client interface added for monitoring endpoints.

    Attributes
    ----------
    workspace : str
        Name of the relevant workspace.
    name : str
        Name of the monitor.
    id : int
        ID of the monitor.
    details : Dict[str, Any]
        All available details related to the monitor.
    """
    def __init__(
            self,
            conn: Connection,
            workspace: Union[str, int],
            id: int,
            details: _Monitor.MonitoredEntity,
            ):
        self._conn  = conn
        self._workspace = workspace
        self._id = id
        self._details = details

    def __repr__(self):
        # self.refresh_details()
        return f"Monitor object representing \"{self._details.name}\" in workspace \"{self._workspace}\"\n" \
               f"Details >>>\n" \
               f"{self._details}"

    @property
    def workspace(self):
        return self._workspace

    @property
    def name(self):
        return self._details.name

    @property
    def id(self):
        return self._id

    def refresh_details(self) -> None:
        #TODO remove print
        print("----Details refreshed")
        self._details = self._find_proto(
            conn=self._conn,
            workspace=self._workspace,
            ids=[self._id],
            )[0]

    @classmethod
    def from_response(
            cls,
            conn: Connection,
            workspace: str,
            monitored_entity: _Monitor.MonitoredEntity,
            ):
        """
        Turn the proto response for a monitored_entity into an object of the Monitor class.
        Parameters
        ----------
        monitored_entity : verta._protos.public.monitoring.MonitoredEntity_pb2.MonitoredEntity
            The

        Returns
        -------
        object : Monitor
        """
        return Monitor(
            conn=conn,
            workspace=workspace,
            id=monitored_entity.id,
            details=monitored_entity
        )

    @classmethod
    def _find_proto(
            cls,
            conn: Connection,
            workspace: Union[int, str],
            names: Optional[List[str]] = None,
            ids: Optional[List[int]] = None,
            endpoint_ids: Optional[List[int]] = None,
            model_version_ids: Optional[List[str]] = None,
            page_limit: int = 1000,
            page_number: int = 0,
            ) -> RepeatedCompositeFieldContainer:
        """
        Make a request to the back end to search for any existing monitors with the provided
        parameters.
        See doc string for client.find_monitors().
        """
        #TODO remove print
        print(">>> _find_proto")
        func_args: Dict[str, Any] = locals()  # Get all function args as a dict.
        request_body = _monitoring_utils._body_from_args(
            args=func_args,
            exclude_args=['cls', 'conn', 'silent']  # should not be added to request body
            )

        Message = _Monitor.FindMonitoredEntityRequest
        endpoint = "/api/v1/monitoring/monitored_entity/findMonitoredEntity"
        response = conn.make_proto_request(
            method='POST',
            path=endpoint,
            body=Message(**request_body),
            )
        monitors = conn.maybe_proto_response(
            response=response,
            response_type=Message.Response,
            ).monitored_entities
        return monitors

    @classmethod
    def _create_proto(
            cls,
            conn: Connection,
            workspace: Optional[Union[str, int]],
            name: str,
            endpoint_id: int,
            attributes: Optional[Dict[str, str]],
            resource_visibility: Optional[str] = None,
            custom_permission: Optional[str] = None,
            ) -> 'Monitor':
        """
        Make a request to the back end to create a new monitor with the provided parameters.
        See doc string for client.create_monitor()
        """
        #TODO remove print
        print(">>> _create_proto")
        func_args: Dict[str, Any] = locals()  # Get all function args as a dict.

        if resource_visibility:
            func_args.update(_monitoring_utils.validate_resource_visibility(resource_visibility))

        if custom_permission:
            func_args.update(_monitoring_utils.validate_custom_permission(custom_permission))

        request_body = _monitoring_utils._body_from_args(
            args=func_args,
            exclude_args=['cls', 'conn']  # should not be added to request body
        )
        Message = _Monitor.CreateMonitoredEntityRequest
        endpoint = "/api/v1/monitoring/monitored_entity/createMonitoredEntity"
        response = conn.make_proto_request(
            method='POST',
            path=endpoint,
            body=Message(**request_body),
            )
        monitor = conn.must_proto_response(
            response=response,
            response_type=Message.Response,
            ).monitored_entity
        print(f"\nNew monitor \"{monitor.name}\" created in workspace \"{workspace}\".")
        return Monitor(
            conn=conn,
            workspace=workspace,
            id=monitor.id,
            details=monitor,
        )

    @classmethod
    def _update_proto(
            cls,
            conn: Connection,
            workspace: Optional[Union[str, int]],
            id: int,
            name: str,
            attributes: Optional[Dict[str, str]],
            resource_visibility: Optional[str] = None,
            custom_permission: Optional[str] = None,
            ) -> 'Monitor':
        """
        Make a request to the backend to update a monitor with the provided parameters.
        See doc string for client.update_monitor().
        """
        #TODO remove print
        print(">>> _update_proto")
        func_args: Dict[str, Any] = locals()  # Get all function args as a dict.
        if resource_visibility:
            func_args.update(_monitoring_utils.validate_resource_visibility(resource_visibility))

        if custom_permission:
            func_args.update(_monitoring_utils.validate_custom_permission(custom_permission))

        request_body = _monitoring_utils._body_from_args(
            args=func_args,
            exclude_args=['cls', 'conn']  # should not be added to request body
        )
        Message = _Monitor.UpdateMonitoredEntityRequest
        endpoint = "/api/v1/monitoring/monitored_entity/updateMonitoredEntity"
        response = conn.make_proto_request(
            method='PATCH',
            path=endpoint,
            body=Message(**request_body),
        )
        monitor = conn.must_proto_response(
            response=response,
            response_type=Message.Response,
        ).monitored_entity
        print(f"\nMonitor \"{monitor.name}\" updated in workspace \"{workspace}\".")
        return Monitor(
            conn=conn,
            workspace=workspace,
            id=monitor.id,
            details=monitor,
        )

    @classmethod
    def _delete_proto(
            cls,
            conn: Connection,
            id: int,
            ):
        """
        Make a request to the backend to delete the provided monitor.
        See doc string for client.delete_monitor()
        """
        #TODO remove print
        print(">>> _delete_proto")
        Message = _Monitor.DeleteMonitoredEntityRequest
        endpoint = "/api/v1/monitoring/monitored_entity/deleteMonitoredEntity"
        response = conn.make_proto_request(
            method='DELETE',
            path=endpoint,
            body=Message(id=id),
            )
        if response.ok:
            print(f"deleted monitor id: {id}")
