# -*- coding: utf-8 -*-
"""Monitoring sub-client."""

from __future__ import print_function


from .monitored_entity import MonitoredEntity
from .notification_channel.entities import NotificationChannels
from ._profilers import Profilers
from .summaries.summaries import Summaries
from .summaries.summary_samples import SummarySamples
from .labels import Labels
from .alert.entities import Alerts


class Client(object):
    """The sub-client aggregating repositories for monitoring features.

    This sub-client acts as a namespace for the collection objects used
    to interact with Verta's profiling and data monitoring features. Users
    should access instances of this client through the base Verta Client.

    Parameters
    ----------
    verta_client : :class:`verta.Client`
        An instance of the base Verta client.

    Attributes
    ----------
    summaries : :class:`~verta.monitoring.summaries.summaries.Summaries`
        Summaries collection.
    summary_samples : :class:`~verta.monitoring.summaries.summary_samples.SummarySamples`
        Summary samples collection.
    labels : :class:`~verta.monitoring.labels.Labels`
        Labels collection for finding label keys and values.
    alerts : :class:`~verta.monitoring.alert.entities.Alerts`
        Alerts collection for configuring and managing alert objects.
    notification_channels : :class:`~verta.monitoring.notification_channel.entities.NotificationChannels`
        Notification channel collection.

    Examples
    --------
    .. code-block:: python

        from verta import Client

        monitoring_client = Client().monitoring

        monitored_entity = monitoring_client.get_or_create_monitored_entity(
            "My Data Pipeline",
        )

    """

    def __init__(self, verta_client):
        self._client = verta_client

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
    def _profilers(self):
        return Profilers(self._conn, self._conf, self._client)

    @property
    def summaries(self):
        return Summaries(self._conn, self._conf)

    @property
    def summary_samples(self):
        return SummarySamples(self._conn, self._conf)

    @property
    def labels(self):
        return Labels(self._conn, self._conf)

    @property
    def alerts(self):
        return Alerts(self._conn, self._conf)

    @property
    def notification_channels(self):
        return NotificationChannels(self._client)

    def get_or_create_monitored_entity(self, name=None, workspace=None, id=None):
        """Get or create a monitored entity by name.

        Gets or creates a monitored entity. A name will be auto-generated if one
        is not provided. Either `name` or `id` can be provided but not both.
        If `id` is provided, this will act only as a get method and no object will
        be created.

        Parameters
        ----------
        name : str, optional
            A unique name for this monitored entity.
        workspace: str, optional
            A workspace for this entity. Defaults to the client's default workspace.
        id : int, optional
            This should not be provided if ``name`` is provided.

        Returns
        -------
        :class:`~verta.monitoring.monitored_entity.MonitoredEntity`
            A monitored entity object.
        """
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        if workspace and id:
            raise ValueError(
                "cannot specify both `workspace` and `id`;"
                " getting by ID does not require a workspace name"
            )

        name = self._client._set_from_config_if_none(name, "monitored_entity")
        if workspace is None:
            workspace = self._client.get_workspace()

        if id is not None:
            entity = MonitoredEntity._get_by_id(self._conn, self._conf, id)
        else:
            entity = MonitoredEntity._get_or_create_by_name(
                self._conn,
                name,
                lambda name: MonitoredEntity._get_by_name(
                    self._conn, self._conf, name=name, parent=workspace
                ),
                lambda name: MonitoredEntity._create(
                    self._conn,
                    self._conf,
                    self._ctx,
                    name=name,
                    workspace_name=workspace,
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
