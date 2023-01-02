# -*- coding: utf-8 -*-
from typing import Any, Dict, List, Optional, Union

from google.protobuf.internal.containers import RepeatedCompositeFieldContainer

from verta._protos.public.monitoring import MonitoredEntity_pb2 as _Monitor
from verta._protos.public.common import CommonService_pb2 as _CommonService
from verta._protos.public.uac import Collaborator_pb2 as _Collaborator
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
        return f"Monitor object representing \"{self.details.name}\" in workspace \"{self._workspace}\"\n" \
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

    @property
    def details(self):
        self.refresh_details()
        return self._details

    def refresh_details(self) -> None:
        self._details = self._find_proto(
            conn=self._conn,
            workspace=self._workspace,
            ids=[self._id],
            silent=True,
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
    def _body_from_args(
            cls,
            args: Dict[str, Any],
            exclude_args: List[str]
            ) -> Dict:
        """
        Shortcut for processing the args from other class functions into a dict to be used as the body of a request.
        Requires that parameter names be the same as the expected proto variables in the request.
        variables for the given call.

        Parameters
        ----------
        args: Dict[str, Any]
            Dict of functions args (locals()) to be packaged up as a clean dict.

        Returns
        -------
        Dict[str, Any]
        """
        body = dict()
        arg_keys = args.keys()
        if 'workspace' in arg_keys:
            if isinstance(args['workspace'], int):
                body['workspace_id'] = args['workspace']
            if isinstance(args['workspace'], str):
                body['workspace_name'] = args['workspace']
            del args['workspace']
        # Make sure we don't mistakenly include the classes "self" or the connection arg.
        for key in exclude_args:
            if key in arg_keys:
                del args[key]
        for k, v in args.items():
            if v:
                body[k] = v
        return body



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
            silent: bool = False
            ) -> RepeatedCompositeFieldContainer:
        """
        Make a request to the back end to search for any existing monitors with the provided
        parameters.
        """
        func_args: Dict[str, Any] = locals()  # Get all function args as a dict.
        request_body = cls._body_from_args(
            args=func_args,
            exclude_args=['cls', 'conn', 'silent']  # should not be added to request body
            )

        Message = _Monitor.FindMonitoredEntityRequest
        endpoint = "/api/v1/monitored_entity/findMonitoredEntity"
        response = conn.make_proto_request(
            method='POST',
            path=endpoint,
            body=Message(**request_body),
            )
        monitors = conn.maybe_proto_response(
            response=response,
            response_type=Message.Response,
            ).monitored_entities
        if monitors:
            if not silent:
                print("\nFound existing monitors:\n---------------------------------")
                monitor_names = [print(m.name) for m in monitors]
        else:
            print(f"\nNo monitors found matching {names}")
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
            ) -> _Monitor.MonitoredEntity:
        """
        Make a request to the back end to create a new monitor with the provided parameters.
        See doc string
        """
        func_args: Dict[str, Any] = locals()  # Get all function args as a dict.

        if resource_visibility:
            ok_rv_values = [x[0] for x in _Collaborator.ResourceVisibility.items()]
            if resource_visibility not in ok_rv_values:
                raise ValueError(f"value for \"resource_visibility\" must be one of {ok_rv_values}. "
                                 f"Received \"{resource_visibility}\" instead.")
            func_args['resource_visibility'] = (
                _Collaborator.ResourceVisibility.Value(resource_visibility)
                )

        if custom_permission:
            ok_cp_values = [x[0] for x in _CommonService.CollaboratorTypeEnum.CollaboratorType.items()]
            if custom_permission not in ok_cp_values:
                raise ValueError(f"value for \"custom_permission\" must be one of {ok_cp_values}. "
                                 f"Received \"{custom_permission}\" instead.")
            func_args['custom_permission'] = (
                _Collaborator.CollaboratorPermissions(
                    collaborator_type = _CommonService.CollaboratorTypeEnum.CollaboratorType.Value(custom_permission)
                    )
                )

        request_body = cls._body_from_args(
            args=func_args,
            exclude_args=['cls', 'conn']  # should not be added to request body
        )
        Message = _Monitor.CreateMonitoredEntityRequest
        endpoint = "/api/v1/monitored_entity/createMonitoredEntity"
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
        return monitor
