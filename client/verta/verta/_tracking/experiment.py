# -*- coding: utf-8 -*-

from __future__ import print_function

import requests
import warnings

from .entity import _ModelDBEntity
from .experimentruns import ExperimentRuns

from .._protos.public.common import CommonService_pb2 as _CommonCommonService
from .._protos.public.modeldb import ExperimentService_pb2 as _ExperimentService

from ..external import six

from .._internal_utils import (
    _utils,
)


class Experiment(_ModelDBEntity):
    """
    Object representing a machine learning Experiment.

    This class provides read/write functionality for Experiment metadata and access to its Experiment
    Runs.

    There should not be a need to instantiate this class directly; please use
    :meth:`Client.set_experiment`.

    Attributes
    ----------
    id : str
        ID of this Experiment.
    name : str
        Name of this Experiment.
    expt_runs : :class:`ExperimentRuns`
        Experiment Runs under this Experiment.

    """
    def __init__(self, conn, conf,
                 proj_id=None, expt_name=None,
                 desc=None, tags=None, attrs=None,
                 _expt_id=None):
        if expt_name is not None and _expt_id is not None:
            raise ValueError("cannot specify both `expt_name` and `_expt_id`")

        if _expt_id is not None:
            expt = Experiment._get(conn, _expt_id=_expt_id)
            if expt is not None:
                print("set existing Experiment: {}".format(expt.name))
            else:
                raise ValueError("Experiment with ID {} not found".format(_expt_id))
        elif proj_id is not None:
            if expt_name is None:
                expt_name = Experiment._generate_default_name()
            try:
                expt = Experiment._create(conn, proj_id, expt_name, desc, tags, attrs)
            except requests.HTTPError as e:
                if e.response.status_code == 409:  # already exists
                    if any(param is not None for param in (desc, tags, attrs)):
                        warnings.warn("Experiment with name {} already exists;"
                                      " cannot initialize `desc`, `tags`, or `attrs`".format(expt_name))
                    expt = Experiment._get(conn, proj_id, expt_name)
                    if expt is not None:
                        print("set existing Experiment: {}".format(expt.name))
                    else:
                        raise RuntimeError("unable to retrieve Experiment {};"
                                           " please notify the Verta development team".format(expt_name))
                else:
                    raise e
            else:
                print("created new Experiment: {}".format(expt.name))
        else:
            raise ValueError("insufficient arguments")

        super(Experiment, self).__init__(conn, conf, _ExperimentService, "experiment", expt.id)

    def __repr__(self):
        return "<Experiment \"{}\">".format(self.name)

    @property
    def name(self):
        Message = _ExperimentService.GetExperimentById
        msg = Message(id=self.id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                       "{}://{}/api/v1/modeldb/experiment/getExperimentById".format(self._conn.scheme, self._conn.socket),
                                       self._conn, params=data)
        _utils.raise_for_http_error(response)

        response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
        return response_msg.experiment.name

    @property
    def expt_runs(self):
        # get runs in this Experiment
        runs = ExperimentRuns(self._conn, self._conf)
        runs._msg.experiment_id = self.id
        return runs

    @staticmethod
    def _generate_default_name():
        return "Expt {}".format(_utils.generate_default_name())

    @classmethod
    def _get_by_id(cls, conn, id):
        Message = _ExperimentService.GetExperimentById
        msg = Message(id=id)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                        "{}://{}/api/v1/modeldb/experiment/getExperimentById".format(conn.scheme, conn.socket),
                                        conn, params=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            expt = response_msg.experiment

            if not expt.id:  # 200, but empty message
                raise RuntimeError("unable to retrieve Experiment {};"
                                   " please notify the Verta development team".format(id))

            return expt
        else:
            if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                    or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                return None
            else:
                _utils.raise_for_http_error(response)

    @classmethod
    def _get_by_name(cls, conn, name, proj_id):
        Message = _ExperimentService.GetExperimentByName
        msg = Message(project_id=proj_id, name=name)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("GET",
                                        "{}://{}/api/v1/modeldb/experiment/getExperimentByName".format(conn.scheme, conn.socket),
                                        conn, params=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            expt = response_msg.experiment

            if not expt.id:  # 200, but empty message
                raise RuntimeError("unable to retrieve Experiment {};"
                                   " please notify the Verta development team".format(name))

            return expt
        else:
            if ((response.status_code == 403 and _utils.body_to_json(response)['code'] == 7)
                    or (response.status_code == 404 and _utils.body_to_json(response)['code'] == 5)):
                return None
            else:
                _utils.raise_for_http_error(response)

    @staticmethod
    def _get(conn, proj_id=None, expt_name=None, _expt_id=None):
        if _expt_id is not None:
            return Experiment._get_by_id(conn, _expt_id)
        elif None not in (proj_id, expt_name):
            return Experiment._get_by_name(conn, expt_name, proj_id)
        else:
            raise ValueError("insufficient arguments")

    @staticmethod
    def _create(conn, proj_id, expt_name, desc=None, tags=None, attrs=None):
        if tags is not None:
            tags = _utils.as_list_of_str(tags)
        if attrs is not None:
            attrs = [_CommonCommonService.KeyValue(key=key, value=_utils.python_to_val_proto(value, allow_collection=True))
                     for key, value in six.viewitems(attrs)]

        Message = _ExperimentService.CreateExperiment
        msg = Message(project_id=proj_id, name=expt_name,
                      description=desc, tags=tags, attributes=attrs)
        data = _utils.proto_to_json(msg)
        response = _utils.make_request("POST",
                                       "{}://{}/api/v1/modeldb/experiment/createExperiment".format(conn.scheme, conn.socket),
                                       conn, json=data)

        if response.ok:
            response_msg = _utils.json_to_proto(_utils.body_to_json(response), Message.Response)
            return response_msg.experiment
        else:
            _utils.raise_for_http_error(response)
