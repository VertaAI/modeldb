# -*- coding: utf-8 -*-

from __future__ import print_function

from .._internal_utils import _utils

from .. import deployment
from .._tracking import experimentrun


class Endpoint(object):
    def __init__(self, conn, conf, workspace, id, path):
        self.workspace = workspace
        self.id = id
        self._path = path

    def __repr__(self):
        # TODO: print full info
        return "<Endpoint \"{}\">".format(self.path)

    @property
    def path(self):
        return self._path

    @classmethod
    def _create(cls, conn, conf, workspace, path, description=None):
        endpoint_json = cls._create_json(conn, workspace, path, description)
        if endpoint_json:
            return Endpoint._new(conf, conn, workspace, endpoint_json)
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
            return Endpoint._new(conf, conn, workspace, endpoint_json)
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
            return Endpoint._new(conf, conn, workspace, endpoint_json)
        else:
            return None

    @classmethod
    def _new(cls, conf, conn, workspace, endpoint_json):
        return cls(conn, conf, workspace, endpoint_json['id'], endpoint_json['creator_request']['path'])

    @classmethod
    def _get_json_by_path(cls, conn, workspace, path):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints".format(conn.scheme, conn.socket, workspace)
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)
        if not path.startswith('/'):
            path = '/' + path
        data_response = response.json()
        for endpoint in data_response['endpoints']:
            creator_request = endpoint['creator_request']
            if creator_request['path'] == path:
                return endpoint
        return None

    def update(self, run, strategy):
        raise NotImplementedError
        # TODO: check if isinstance(run, experimentrun.ExperimentRun)
        # TODO: check if isinstance(strategy, deployment._UpdateStrategy)

    def update_from_config(self, filepath):
        raise NotImplementedError

    def get_status(self):
        raise NotImplementedError
