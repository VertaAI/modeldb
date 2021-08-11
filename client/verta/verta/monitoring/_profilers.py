# -*- coding: utf-8 -*-

import time

from verta.tracking import _Context
from verta.tracking.entities import _entity
from verta._protos.public.monitoring import (
    DataMonitoringService_pb2 as _DataMonitoringService,
)
from verta._protos.public.monitoring.DataMonitoringService_pb2 import (
    GetProfilerRequest,
    CreateProfilerRequest,
    ListProfilersRequest,
    UpdateProfilerRequest,
    DeleteProfilerRequest,
    EnableProfilerRequest,
    GetProfilerStatusRequest,
    DisableProfilerRequest,
    KeyValue,
    DeployStatusEnum as DeployStatus,
    BuildStatusEnum as BuildStatus,
)
from verta._internal_utils import _utils, arg_handler
from verta.environment import Python


class ProfilerReference(_entity._ModelDBEntity):
    """Represents an uploaded data profiler.

    A ProfilerReference represents an uploaded data profiler and provides
    methods to deploy, disable, and check the deployment status of profilers.

    Users should obtain ``ProfilerReference`` objects through the get and
    create methods available from :class:`Profilers`, accessible as the
    ``profilers`` attribute on the monitoring sub-:class:`~verta.monitoring.client.Client`.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.
    msg
        A protobuf message ai.verta.monitoring.Profiler

    Attributes
    ----------
    id : int
        The id of this profiler.
    name : str
        The name of this profiler.
    """

    def __init__(self, conn, conf, msg):
        super(ProfilerReference, self).__init__(
            conn, conf, _DataMonitoringService, "profiler", msg
        )
        self.id = msg.id
        self.name = msg.name
        self.reference = msg.profiler_reference  # TODO: hide this implementation detail
        self._conn = conn

    def __repr__(self):
        id = self._msg.id
        name = self._msg.name
        reference = self._msg.profiler_reference
        return "ProfilerReference ({}, {}, {})".format(id, name, reference)

    def enable(self, monitored_entity, environment=None, wait=False):
        """Build and deploy this uploaded profiler.

        This method instructs Verta Services to build and deploy a docker image
        for the profiler which was uploaded. Environment variables for this
        deployment can be specified in the `environment` keyword argument as
        a dictionary from strings to strings.

        By default this method will issue a command to Verta Services to build
        an image, or to deploy the built image if the build is ready. In order
        to fully build and deploy an image, and to block on the completion of
        this process, users should specify ``wait=True``.

        Parameters
        ----------
        monitored_entity : :class:`~verta.monitoring.monitored_entity.MonitoredEntity`
            The monitored entity for which this profiler should be enabled.
        environment : dict, optional
            Dictionary from strings to strings specifying environment variables.
        wait: bool, optional
            Whether to block on completion of the full build-and-deploy process.
            Defaults to False.

        Returns
        -------
        ai.verta.monitoring.ProfilerStatus
            Deployment and build status of this profiler
        """
        if wait:
            return self._blocking_enable(monitored_entity, environment)

        profiler_id = self.id
        monitored_entity_id = arg_handler.extract_id(monitored_entity)
        environment = environment.copy() if environment else dict()
        environment["PROFILER_ID"] = profiler_id
        key_values = [
            KeyValue(key=k, value=_utils.python_to_val_proto(v, allow_collection=True))
            for k, v in environment.items()
        ]

        msg = EnableProfilerRequest(
            profiler_id=profiler_id,
            monitored_entity_id=monitored_entity_id,
            environment=key_values,
        )
        endpoint = "/api/v1/monitored_entity/enableProfiler"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        status = self._conn.must_proto_response(
            response, EnableProfilerRequest.Response
        ).status
        return status  # TODO: wrap this in a nicer type

    def _blocking_enable(self, monitored_entity, environment, duration_seconds=0.5):
        status = self._blocking_build(
            monitored_entity, environment, duration_seconds=duration_seconds
        )
        status = self._blocking_deploy(
            monitored_entity, environment, duration_seconds=duration_seconds
        )
        return status

    def _blocking_build(self, monitored_entity, environment, duration_seconds=0.5):
        status = self.enable(
            monitored_entity=monitored_entity, environment=environment, wait=False
        )
        print("Status: {}".format(status))
        print("Waiting for build...")
        while True:
            time.sleep(duration_seconds)
            if status.build_status in (BuildStatus.UNDEFINED, BuildStatus.BUILDING):
                status = self.get_status(monitored_entity)
            elif status.build_status in (
                BuildStatus.DELETING,
                BuildStatus.ERROR,
                BuildStatus.FINISHED,
            ):
                print("Status: {}".format(status))
                return status
            else:
                print("Deploy status is not recognized")
                return status

    def _blocking_deploy(self, monitored_entity, environment, duration_seconds=0.5):
        status = self.get_status(monitored_entity)
        if status.build_status != BuildStatus.FINISHED:
            print("Build not complete. Deploy not possible.")
            return status
        status = self.enable(
            monitored_entity=monitored_entity, environment=environment, wait=False
        )
        while True:
            time.sleep(duration_seconds)
            if status.deploy_status in (
                DeployStatus.UNDEFINED,
                DeployStatus.INACTIVE,
                DeployStatus.UPDATING,
                DeployStatus.CREATING,
            ):
                status = self.get_status(monitored_entity)
            elif status.deploy_status in (DeployStatus.ACTIVE, DeployStatus.ERROR):
                print("Status: {}".format(status))
                return status
            else:
                print("Deploy status is not recognized")
                return status

    def disable(self, monitored_entity):
        """Disable this profiler for the monitored entity if it's enabled.

        Parameters
        ----------
        monitored_entity : :class:`~verta.monitoring.monitored_entity.MonitoredEntity`
            The monitored entity for which this profiler should be disabled.

        Returns
        -------
        ai.verta.monitoring.ProfilerStatus
            Deployment and build status of this profiler
        """
        profiler_id = self.id
        try:
            monitored_entity_id = monitored_entity.id
        except:
            monitored_entity_id = monitored_entity

        msg = DisableProfilerRequest(
            profiler_id=profiler_id, monitored_entity_id=monitored_entity_id
        )
        endpoint = "/api/v1/monitored_entity/disableProfiler"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        status = self._conn.must_proto_response(
            response, DisableProfilerRequest.Response
        ).status
        return status

    def get_status(self, monitored_entity):
        """Get the build and deploy status of this profiler.

        Parameters
        ----------
        monitored_entity : :class:`~verta.monitoring.monitored_entity.MonitoredEntity`
            The monitored entity for which the status should be returned.

        Returns
        -------
        ai.verta.monitoring.ProfilerStatus
            Deployment and build status of this profiler
        """
        profiler_id = self.id
        try:
            monitored_entity_id = monitored_entity.id
        except:
            monitored_entity_id = monitored_entity
        msg = GetProfilerStatusRequest(
            profiler_id=profiler_id, monitored_entity_id=monitored_entity_id
        )
        endpoint = "/api/v1/monitored_entity/getProfilerStatus"
        response = self._conn.make_proto_request("GET", endpoint, params=msg)
        status = self._conn.must_proto_response(
            response, GetProfilerStatusRequest.Response
        ).status
        return status

    def _set_version(self, model_version):
        self._version = model_version

    def _set_client(self, client):
        self._client = client

    def update(self, name):
        """Rename this profiler.

        Parameters
        ----------
        name : str
            A new name for this profiler.

        Returns
        -------
        self
            Returns this profiler reference.
        """
        msg = UpdateProfilerRequest(id=self.id, name=name)
        endpoint = "/api/v1/monitored_entity/updateProfiler"
        response = self._conn.make_proto_request("PATCH", endpoint, body=msg)
        updated_profiler = self._conn.must_proto_response(
            response, UpdateProfilerRequest.Response
        ).profiler
        self._set_version(updated_profiler.profiler_reference)
        self.name = updated_profiler.name
        return self

    @classmethod
    def _generate_default_name(cls):
        return "Profiler {}".format(_utils.generate_default_name())

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = GetProfilerRequest(id=id)
        endpoint = "/api/v1/monitored_entity/getProfiler"
        response = conn.make_proto_request("GET", endpoint, params=msg)
        return conn.maybe_proto_response(response, GetProfilerRequest.Response).profiler

    @classmethod
    def _get_proto_by_name(cls, conn, name, parent):
        raise NotImplementedError

    @classmethod
    def _create_proto_internal(cls, conn, ctx, name, reference):
        msg = CreateProfilerRequest(name=name, profiler_reference=reference)
        endpoint = "/api/v1/monitored_entity/createProfiler"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        profiler = conn.must_proto_response(response, msg.Response).profiler
        return profiler


class Profilers(object):
    """Collection object for creating and finding profilers.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        client = Client()
        profilers = client.monitoring.profilers

    """

    def __init__(self, conn, conf, client):
        self._conn = conn
        self._conf = conf
        self._client = client

    @classmethod
    def default_environment(cls):
        return Python(requirements=["numpy", "scipy", "pandas"])

    def list(self):
        endpoint = "/api/v1/monitored_entity/listProfilers"
        response = self._conn.make_proto_request("GET", endpoint)
        profilers = self._conn.must_proto_response(
            response, ListProfilersRequest.Response
        ).profilers
        return profilers

    # TODO: Hide this as an internal method
    def get(self, profiler_id):
        ref = ProfilerReference._get_by_id(self._conn, self._conf, profiler_id)
        ref._set_client(self._client)
        return ref

    def upload(self, name, profiler, attrs={}, environment=None):
        environment = environment if environment else self.default_environment()
        model = self._client.get_or_create_registered_model()
        version = model.get_or_create_version()
        version.add_attribute(key="type", value="profiler", overwrite=True)
        reference = self.create(name=name, model_version=version)
        for attr, value in attrs.items():
            version.add_attribute(key=attr, value=value, overwrite=True)

        version.log_model(profiler, overwrite=True)
        version.log_environment(environment, overwrite=True)
        reference._set_version(version)
        reference._set_client(self._client)
        return reference

    # TODO: hide this an internal method
    def create(self, name, model_version):
        ctx = _Context(self._conn, self._conf)
        reference = str(model_version.id)
        reference = ProfilerReference._create(
            self._conn, self._conf, ctx, name=name, reference=reference
        )
        reference._set_version(model_version)
        reference._set_client(self._client)
        return reference

    def delete(self, profiler_reference):
        """Delete the provided profiler reference.

        Parameters
        ----------
        profiler_reference : ProfilerReference
            The profiler to be deleted.

        Returns
        -------
        bool
            True if the deletion was successful.
        """
        id = profiler_reference.id
        msg = DeleteProfilerRequest(id=id)
        endpoint = "/api/v1/monitored_entity/deleteProfile"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
