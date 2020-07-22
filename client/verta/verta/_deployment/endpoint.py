# -*- coding: utf-8 -*-

from __future__ import print_function

from .. import deployment
from .._tracking import experimentrun


class Endpoint(object):
    def __init__(self, conn, conf, workspace, id):
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
        if not path.startswith('/'):
            path = '/' + path

        raise NotImplementedError

    @classmethod
    def _get_by_id(cls, conn, conf, workspace, id):
        endpoint_json = cls._get_json_by_id(conn, workspace, id)
        if endpoint_json:
            return cls(conn, conf, workspace, endpoint_json['id'])
        else:
            return None

    @classmethod
    def _get_json_by_id(cls, conn, workspace, id):
        raise NotImplementedError

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
        raise NotImplementedError
        # TODO: check if isinstance(run, experimentrun.ExperimentRun)
        # TODO: check if isinstance(strategy, deployment._UpdateStrategy)

        # TODO: POST "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages".format(scheme, socket, workspace, self.id)
        #       to create a stage

        # TODO: POST "{}://{}/api/v1/deployment/workspace/{}/builds".format(scheme, socket, workspace)
        #       to create a build. _utils.make_request(..., `json={'run_id': run.id}`)

        # TODO: PUT

    def get_status(self):
        raise NotImplementedError
