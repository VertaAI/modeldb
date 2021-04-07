# -*- coding: utf-8 -*-

from collections import namedtuple
import time

from verta._tracking import entity, _Context
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
    ValueTypeEnum,
    DeployStatusEnum as DeployStatus,
    BuildStatusEnum as BuildStatus,
)
from verta._internal_utils import _utils
from verta.environment import Python

class ProfilerReference(entity._ModelDBEntity):
    def __init__(self, conn, conf, msg):
        super(ProfilerReference, self).__init__(
            conn, conf, _DataMonitoringService, "profiler", msg
        )
        self.id = msg.id
        self.name = msg.name
        self.reference = msg.profiler_reference
        self._conn = conn

    def __repr__(self):
        id = self._msg.id
        name = self._msg.name
        reference = self._msg.profiler_reference
        return "ProfilerReference ({}, {}, {})".format(id, name, reference)

    def enable(self, monitored_entity, environment=None, wait=False):
        if wait:
            return self._blocking_enable(monitored_entity, environment)

        profiler_id = self.id
        try:
            monitored_entity_id = monitored_entity.id
        except:
            monitored_entity_id = monitored_entity
        environment = environment.copy() if environment else dict()
        environment["PROFILER_ID"] = self.id
        key_values = [KeyValue(key=k, value=_utils.python_to_val_proto(v, allow_collection=True)) for k, v in environment.items()]

        msg = EnableProfilerRequest(
            profiler_id=profiler_id, monitored_entity_id=monitored_entity_id, environment=key_values
        )
        endpoint = "/api/v1/monitored_entity/enableProfiler"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        status = self._conn.must_proto_response(
                response, EnableProfilerRequest.Response
            ).status
        return status


    def _blocking_enable(self, monitored_entity, environment, duration_seconds=0.5):
        status = self._blocking_build(monitored_entity, environment, duration_seconds=duration_seconds)
        status = self._blocking_deploy(monitored_entity, environment, duration_seconds=duration_seconds)
        return status


    def _blocking_build(self, monitored_entity, environment, duration_seconds=0.5):
        status = self.enable(monitored_entity=monitored_entity, environment=environment, wait=False)
        print("Status: {}".format(status))
        print("Waiting for build...")
        while True:
            time.sleep(duration_seconds)
            if status.build_status in (BuildStatus.UNDEFINED, BuildStatus.BUILDING):
                status = self.get_status(monitored_entity)
            elif status.build_status in (BuildStatus.DELETING, BuildStatus.ERROR, BuildStatus.FINISHED):
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
        status = self.enable(monitored_entity=monitored_entity, environment=environment, wait=False)
        while True:
            time.sleep(duration_seconds)
            if status.deploy_status in (DeployStatus.UNDEFINED, DeployStatus.INACTIVE, DeployStatus.UPDATING, DeployStatus.CREATING):
                status = self.get_status(monitored_entity)
            elif status.deploy_status in (DeployStatus.ACTIVE, DeployStatus.ERROR):
                print("Status: {}".format(status))
                return status
            else:
                print("Deploy status is not recognized")
                return status


    def disable(self, monitored_entity):
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
        profiler = conn.must_proto_response(
            response, msg.Response
        ).profiler
        return profiler


class Profilers:

    DEFAULT_ENVIRONMENT = Python(requirements=["numpy", "scipy", "pandas"])

    def __init__(self, conn, conf, client):
        self._conn = conn
        self._conf = conf
        self._client = client

    def list(self):
        endpoint = "/api/v1/monitored_entity/listProfilers"
        response = self._conn.make_proto_request("GET", endpoint)
        profilers = self._conn.must_proto_response(
            response, ListProfilersRequest.Response
        ).profilers
        return profilers

    def get(self, profiler_id):
        ref = ProfilerReference._get_by_id(self._conn, self._conf, profiler_id)
        ref._set_client(self._client)
        return ref

    def upload(self, name, profiler, attrs={}, environment=DEFAULT_ENVIRONMENT):
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

    def create(self, name, model_version):
        ctx = _Context(self._conn, self._conf)
        reference = str(model_version.id)
        reference = ProfilerReference._create(
            self._conn, self._conf, ctx, name=name, reference=reference
        )
        reference._set_version(model_version)
        reference._set_client(self._client)
        return reference


    @staticmethod
    def enable(profiler_reference, monitored_entity):
        return profiler_reference.enable(monitored_entity)

    @staticmethod
    def disable(profiler_reference, monitored_entity):
        return profiler_reference.disable(monitored_entity)

    @staticmethod
    def get_status(profiler_reference, monitored_entity):
        return profiler_reference.get_status(monitored_entity)

    def delete(self, profiler_reference):
        id = profiler_reference.id
        msg = DeleteProfilerRequest(id=id)
        endpoint = "/api/v1/monitored_entity/deleteProfile"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True
