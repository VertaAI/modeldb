# -*- coding: utf-8 -*-

from __future__ import print_function
import itertools

import verta

from verta._tracking import entity
from verta._tracking import (
    _Context,
)

from monitored_entity import MonitoredEntity
from data_source import DataSource
from clients.profilers import Profilers
from clients.alert_definitions import AlertDefinitions
from clients.alerts import Alerts
from clients.summaries import Summaries
from clients.labels import Labels


class Client(verta.Client):
    def __init__(self, *args, **kwargs):
        super(Client, self).__init__(*args, **kwargs)
        self.profilers = Profilers(self._conn, self._conf, self)
        self.alerts = Alerts(self._conn, self._conf)
        self.alert_definitions = AlertDefinitions(self._conn, self._conf)
        self.summaries = Summaries(self._conn, self._conf)
        self.labels = Labels(self._conn, self._conf)

    def get_or_create_monitored_entity(self, name=None, workspace=None, id=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._set_from_config_if_none(name, "monitored_entity")
        workspace = self._set_from_config_if_none(workspace, "workspace")

        ctx = _Context(self._conn, self._conf)
        ctx.workspace_name = workspace

        resource_name = "MonitoredEntity"
        # param_names = "`desc`, `tags`, `attrs`, `time_created`, or `public_within_org`"
        # params = (desc, tags, attrs, time_created, public_within_org)
        if id is not None:
            entity = MonitoredEntity._get_by_id(self._conn, self._conf, id)
            # check_unnecessary_params_warning(resource_name, "id {}".format(id),
            #                                       param_names, params)
        else:
            entity = MonitoredEntity._get_or_create_by_name(
                self._conn,
                name,
                lambda name: MonitoredEntity._get_by_name(
                    self._conn, self._conf, name=name, parent=workspace
                ),
                lambda name: MonitoredEntity._create(
                    self._conn, self._conf, ctx, name=name
                ),
                lambda: self.__noop_checker(),
            )

        entity._set_client(self)
        return entity


    @staticmethod
    def __error():
        raise NotImplementedError()

    @staticmethod
    def __noop_checker():
        pass
