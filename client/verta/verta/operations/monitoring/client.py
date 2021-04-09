# -*- coding: utf-8 -*-

from __future__ import print_function
import itertools

from verta._tracking import _Context

from .monitored_entity import MonitoredEntity
from .notification_channel._entities import NotificationChannels
from .profilers import Profilers
from .summaries import Summaries, SummarySamples
from .labels import Labels
from .alert._entities import Alerts


class Client(object):
    def __init__ (self, verta_client):
        self._client = verta_client
        self.profilers = Profilers(self._conn, self._conf, self._client)
        self.summaries = Summaries(self._conn, self._conf)
        self.summary_samples = SummarySamples(self._conn, self._conf)
        self.labels = Labels(self._conn, self._conf)

    @property
    def _conn(self):
        return self._client._conn

    @property
    def _conf(self):
        return self._client._conf

    @property
    def _ctx(self):
        return self._client._ctx

    @property
    def alerts(self):
        return Alerts(self._conn, self._conf)

    @property
    def notification_channels(self):
        return NotificationChannels(self._conn, self._conf)

    def get_or_create_monitored_entity(self, name=None, workspace=None, id=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")

        name = self._client._set_from_config_if_none(name, "monitored_entity")
        if workspace is None:
            workspace = self._client.get_workspace()

        ctx = self._ctx
        # TODO: use the context workspace and do not modify it through parameters on this method
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
