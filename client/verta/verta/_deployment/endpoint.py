# -*- coding: utf-8 -*-

from __future__ import print_function
import sys
import time
import json
import yaml

from ..deployment.update.rules import _UpdateRule
from ..deployment import DeployedModel
from ..deployment.update._strategies import _UpdateStrategy, DirectUpdateStrategy, CanaryUpdateStrategy
from .._internal_utils import _utils
from .._tracking import experimentrun


class Endpoint(object):
    def __init__(self, conn, conf, workspace, id):
        self.workspace = workspace
        self._conn = conn
        self._conf = conf
        self.id = id

    def __repr__(self):
        # TODO: print full info
        return "<Endpoint \"{}\">".format(self.path)

    @property
    def path(self):
        return self._path(Endpoint._get_json_by_id(self._conn, self.workspace, self.id))

    def _path(self, data):
        return data['creator_request']['path']

    def _date_updated(self, data):
        return data['date_updated']

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

    def _get_info_list_by_id(self):
        data = Endpoint._get_json_by_id(self._conn, self.workspace, self.id)
        return [self._path(data), str(self.id), self._date_updated(data)]

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
    def _get_endpoints(cls, conn, workspace):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints".format(conn.scheme, conn.socket, workspace)
        response = _utils.make_request("GET", url, conn)
        _utils.raise_for_http_error(response)
        data_response = response.json()
        return data_response['endpoints']

    @classmethod
    def _get_json_by_path(cls, conn, workspace, path):
        endpoints = cls._get_endpoints(conn, workspace)
        if not path.startswith('/'):
            path = '/' + path
        for endpoint in endpoints:
            creator_request = endpoint['creator_request']
            if creator_request['path'] == path:
                return endpoint
        return None

    def update(self, run, strategy, wait=False, resources=None, autoscaling=None, env_vars=None):
        if not isinstance(run, experimentrun.ExperimentRun):
            raise TypeError("run must be an ExperimentRun")

        if not isinstance(strategy, _UpdateStrategy):
            raise TypeError("strategy must be an object from verta.deployment.strategies")

        # Create new build:
        url = "{}://{}/api/v1/deployment/workspace/{}/builds".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
        )
        response = _utils.make_request("POST", url, self._conn, json={"run_id": run.id})
        _utils.raise_for_http_error(response)
        build_id = response.json()["id"]

        # prepare body for update request
        update_body = strategy._as_build_update_req_body(build_id)

        # Update stages with new build
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages/{}/update".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
            self._get_or_create_stage(),
        )
        response = _utils.make_request("PUT", url, self._conn, json=update_body)
        _utils.raise_for_http_error(response)

        if wait:
            print("waiting for update...", end='')
            sys.stdout.flush()
            while self.get_status()['status'] not in ("active", "error"):
                print(".", end='')
                sys.stdout.flush()
                time.sleep(5)
            print()
            if self.get_status()['status'] == "error":
                raise RuntimeError("endpoint update failed")

        return self.get_status()

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
        with open(filepath, 'r') as f:
            config = f.read()

        is_json = True
        is_yaml = True

        try:
            update_dict = json.loads(config)
        except ValueError:
            is_json = False

        if not is_json:
            try:
                update_dict = yaml.full_load(config)
            except yaml.YAMLError:
                is_yaml = False

        if not (is_json or is_yaml):
            raise ValueError("input must be a json or yaml")

        return self._update_from_dict(update_dict)

    def _update_from_dict(self, update_dict):
        if update_dict["strategy"] == "direct":
            strategy = DirectUpdateStrategy()
        elif update_dict["strategy"] == "canary":
            strategy = CanaryUpdateStrategy(
                interval=int(update_dict["canary_strategy"]["progress_interval_seconds"]),
                step=float(update_dict["canary_strategy"]["progress_step"])
            )

            for rule in update_dict["canary_strategy"]["rules"]:
                strategy.add_rule(_UpdateRule._from_dict(rule))
        else:
            raise ValueError("update strategy must be \"direct\" or \"canary\"")

        run = experimentrun.ExperimentRun._get_by_id(self._conn, self._conf, id=update_dict["run_id"])
        return self.update(run, strategy)

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

    def get_access_token(self):
        url = "{}://{}/api/v1/deployment/workspace/{}/endpoints/{}/stages/{}/accesstokens".format(
            self._conn.scheme,
            self._conn.socket,
            self.workspace,
            self.id,
            self._get_or_create_stage(),
        )
        response = _utils.make_request("GET", url, self._conn)
        _utils.raise_for_http_error(response)
        data = response.json()
        tokens = data["tokens"]
        if len(tokens) == 0:
            return None
        return tokens[0]['creator_request']['value']

    def get_deployed_model(self):
        status = self.get_status()
        if status['status'] != "active":
            raise RuntimeError("model is not currently deployed (status: {})".format(status))

        access_token = self.get_access_token()
        url = "{}://{}/api/v1/predict{}".format(self._conn.scheme, self._conn.socket, self.path)
        return DeployedModel.from_url(url, access_token)
