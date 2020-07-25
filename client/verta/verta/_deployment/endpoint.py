# -*- coding: utf-8 -*-

from __future__ import print_function

from ..deployment.strategies import _UpdateStrategy
from .._internal_utils import _utils
from .._tracking import experimentrun


class Endpoint(object):
    def __init__(self, conn, conf, workspace, id):
        self._conn = conn
        self._conf = conf

        self.workspace = workspace
        self.id = id

    def __repr__(self):
        # TODO: print full info
        return "<Endpoint \"{}\">".format(self.path)

    @property
    def path(self):
        raise NotImplementedError

    @classmethod
    def _create(cls, conn, conf, workspace, path, description=None):
        endpoint_json = cls._create_json(conn, workspace, path, description)
        if endpoint_json:
            return cls(conn, conf, workspace, endpoint_json['id'])
        else:
            return None

    @classmethod
    def _create_json(cls, conn, workspace, path, description=None):
        data = {}
        if description:
            data["description"] = description
        if not path.startswith('/'):
            path = '/' + path
        data["path"] = path
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints".format(conn.scheme, conn.socket, workspace)
        response = _utils.make_request("POST", url, conn, json=data)
        _utils.raise_for_http_error(response)
        return response.json()

    @classmethod
    def _get_by_id(cls, conn, conf, workspace, id):
        endpoint_json = cls._get_json_by_id(conn, workspace, id)
        if endpoint_json:
            return cls(conn, conf, workspace, endpoint_json['id'])
        else:
            return None

    @classmethod
    def _get_json_by_id(cls, conn, workspace, id):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}".format(conn.scheme, conn.socket, workspace, id)
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)
        return response.json()

    @classmethod
    def _get_or_create_by_name(cls, conn, name, getter, creator):
        obj = getter(name)
        if obj is None:
            obj = creator(name)
        else:
            print("got existing {}: {}".format(cls.__name__, name))
        return obj

    @classmethod
    def _get_by_path(cls, conn, conf, workspace, path):
        endpoint_json = cls._get_json_by_path(conn, workspace, path)
        if endpoint_json:
            return cls(conn, conf, workspace, endpoint_json['id'])
        else:
            return None

    @classmethod
    def _get_json_by_path(cls, conn, workspace, path):
        if not path.startswith('/'):
            path = '/' + path

        raise NotImplementedError
        # TODO: GET "{}://{}/api/v1/deployment/workspace/{}/endpoints".format(scheme, socket, workspace)
        # TODO: iterate through response.json().get('endpoints', []) to find matching creator_request.path

    def update(self, run, strategy):
        if not isinstance(run, experimentrun.ExperimentRun):
            raise TypeError("run must be an ExperimentRun")

        if not isinstance(strategy, _UpdateStrategy):
            raise TypeError("strategy must be an object from verta.deployment.strategies")

        stage_id = self._get_or_create_stage()

        # Create new build:
        url = "{}://{}/api/v1/deployment/workspace/{}/builds".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace
        )
        response = _utils.make_request("POST", url, self._conn, json={"run_id": run.id})
        _utils.raise_for_http_error(response)
        build_id = response.json()["id"]

        # Update stages with new build
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages/{}/update".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
            stage_id
        )
        response = _utils.make_request("PUT", url, self._conn, json=strategy._as_build_update_req_body(build_id))
        _utils.raise_for_http_error(response)

    def _get_or_create_stage(self, name="production"):
        if name == "production":
            # Check if a stage exists:
            url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages".format(
                self._conn.scheme,
                self._conn.socket,
                self.workspace,
                self.id
            )
            response = _utils.make_request("GET", url, self._conn, params={})

            _utils.raise_for_http_error(response)
            response_json = response.json()

            if response_json["stages"]:
                return response_json["stages"][0]["id"]

            # no stage found:
            return self._create_stage("production")
        else:
            raise NotImplementedError("currently not supported other stages")

    def _create_stage(self, name="production"):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id
        )
        response = _utils.make_request("POST", url, self._conn, json={"name": name})
        _utils.raise_for_http_error(response)
        return response.json()["id"]

    def update_from_config(self, filepath):
        raise NotImplementedError

    def get_status(self):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages/{}".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
            self._get_or_create_stage()
        )
        response = _utils.make_request("GET", url, self._conn)
        _utils.raise_for_http_error(response)
        return response.json()

