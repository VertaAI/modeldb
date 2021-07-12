# -*- coding: utf-8 -*-

import warnings

from verta._protos.public.monitoring import Alert_pb2 as _AlertService
from verta._internal_utils import arg_handler, time_utils
from verta.tracking import _Context
from verta.tracking.entities import _entity
from verta.monitoring import notification_channel
from verta.monitoring.summaries.queries import (
    SummaryQuery,
    SummarySampleQuery,
)
from .. import _alerter
from .. import status as status_module


class Alert(_entity._ModelDBEntity):
    """
    An alert persisted to Verta.

    An alert periodically queries matching summary samples to determine if
    a threshold has been exceeded and if so, propagates a notification to
    configured channels.

    Attributes
    ----------
    id : int
        ID of this alert.
    name : str
        Name of this alert.
    created_at : datetime.datetime
        When this alert was created.
    history : list of :class:`AlertHistoryItem`
        History of this alert's status changes.
    labels : dict of str to list of str
        Same as parameter from :meth:`Alerts.create`.
    last_evaluated_at : datetime.datetime or None
        When this alert was last evaluated.
    monitored_entity_id : int
        ID of the monitored entity this alert is associated with.
    starting_from : datetime.datetime or None
        Same as parameter from :meth:`Alerts.create`.
    status : :mod:`~verta.monitoring.alert.status`
        Current status of this alert.
    summary_sample_query : :class:`~verta.monitoring.summaries.queries.SummarySampleQuery`
        Query for the summary samples this alert monitors.
    updated_at : datetime.datetime
        When this alert's configuration was last updated.

    Examples
    --------
    .. code-block:: python

        alert = summary.alerts.create(
            name="MSE",
            alerter=alerter,
            notification_channels=[channel],
        )

    """

    def __init__(self, conn, conf, msg):
        super(Alert, self).__init__(
            conn,
            conf,
            _AlertService,
            "alerts",
            msg,
        )

    def __repr__(self):
        self._refresh_cache()
        msg = self._msg

        return "\n\t".join(
            (
                "Alert",
                "name: {}".format(msg.name),
                "id: {}".format(msg.id),
                "monitored entity id: {}".format(msg.monitored_entity_id),
                "status: {}".format(self.status),
                "created: {}".format(self.created_at),
                "updated: {}".format(self.updated_at),
                "last evaluated: {}".format(self.last_evaluated_at),
                "alerter: {}".format(self.alerter),
                "notification channel ids: {}".format(
                    list(msg.notification_channels.keys())
                ),
                "labels: {}".format(self.labels),
                "starting from: {}".format(self.starting_from),
                "violating summary sample ids: {}".format(
                    msg.violating_summary_sample_ids
                ),
            )
        )

    @property
    def alerter(self):
        self._refresh_cache()
        alerter_field = self._msg.WhichOneof("alerter")
        alerter_msg = getattr(self._msg, alerter_field)

        return _alerter._Alerter._from_proto(alerter_msg)

    @property
    def created_at(self):
        self._refresh_cache()

        return time_utils.datetime_from_millis(self._msg.created_at_millis)

    @property
    def history(self):
        # TODO: implement lazy list and pagination
        msg = _AlertService.ListAlertHistoryRequest(id=self.id)
        endpoint = "/api/v1/alerts/listAlertHistory"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        history = self._conn.must_proto_response(response, msg.Response).history
        return list(map(AlertHistoryItem, history))

    @property
    def labels(self):
        self._refresh_cache()

        return {
            key: values.label_value
            for key, values
            in self._msg.sample_find_base.filter.labels.items()
        }

    @property
    def last_evaluated_at(self):
        self._refresh_cache()

        millis = self._msg.last_evaluated_at_millis
        if not millis:
            return None
        return time_utils.datetime_from_millis(millis)

    @property
    def monitored_entity_id(self):
        self._refresh_cache()

        return self._msg.monitored_entity_id

    @property
    def name(self):
        self._refresh_cache()

        return self._msg.name

    @property
    def starting_from(self):
        self._refresh_cache()

        millis = self._msg.sample_find_base.filter.time_window_start_at_millis
        if not millis:
            return None
        return time_utils.datetime_from_millis(millis)

    @property
    def status(self):
        self._refresh_cache()

        if self._msg.status == status_module.Ok._ALERT_STATUS:
            # violating samples aren't relevant to a retrieved Ok status
            sample_ids = None
        else:
            sample_ids = self._msg.violating_summary_sample_ids

        return status_module._AlertStatus._from_proto(
            self._msg.status,
            sample_ids,
        )

    @property
    def summary_sample_query(self):
        self._refresh_cache()
        sample_query_msg = type(self._msg.sample_find_base)()
        sample_query_msg.CopyFrom(self._msg.sample_find_base)

        # if this alert hasn't been evaluated yet, refer to creation time
        last_evaluated_at = (
            self._msg.last_evaluated_at_millis
            or self._msg.created_at_millis
        )

        # only fetch samples logged after this alert was last evaluated
        # so as to not re-alert on previously-seen samples
        sample_query_msg.filter.created_at_after_millis = last_evaluated_at

        return SummarySampleQuery._from_proto_request(sample_query_msg)

    @property
    def updated_at(self):
        self._refresh_cache()

        return time_utils.datetime_from_millis(self._msg.updated_at_millis)

    @staticmethod
    def _validate_notification_channel(channel):
        if not isinstance(channel, notification_channel.entities.NotificationChannel):
            raise TypeError(
                "notification channel must be an entity object returned"
                " from client.notification_channels.create(),"
                " not {}".format(type(channel))
            )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindAlertRequest(
            ids=[int(id)],
            page_number=1,
            page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alerts = conn.must_proto_response(response, msg.Response).alerts
        if len(alerts) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return alerts[0]

    @classmethod
    def _get_proto_by_name(cls, conn, name, monitored_entity_id):
        msg = _AlertService.FindAlertRequest(
            names=[name],
            monitored_entity_ids=[int(monitored_entity_id)],
            page_number=1,
            page_limit=-1,
        )
        endpoint = "/api/v1/alerts/findAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alerts = conn.must_proto_response(response, msg.Response).alerts
        if len(alerts) > 1:
            warnings.warn(
                "unexpectedly found multiple alerts with the same name and"
                " monitored entity ID"
            )
        return alerts[0]

    @classmethod
    def _create_proto_internal(
        cls,
        conn,
        ctx,
        name,
        monitored_entity_id,
        alerter,
        summary_sample_query,
        notification_channels,
        created_at_millis,
        updated_at_millis,
        last_evaluated_at_millis,
        # TODO: should we allow `status` and `violating samples` on create?
    ):
        msg = _AlertService.CreateAlertRequest(
            alert=_AlertService.Alert(
                name=name,
                monitored_entity_id=monitored_entity_id,
                created_at_millis=created_at_millis,
                updated_at_millis=updated_at_millis,
                last_evaluated_at_millis=last_evaluated_at_millis,
                notification_channels={
                    channel.id: True for channel in notification_channels
                },
                sample_find_base=summary_sample_query._to_proto_request(),
                alerter_type=alerter._TYPE,
            ),
        )

        field = getattr(msg.alert, alerter._get_alert_field())
        field.CopyFrom(alerter._as_proto())

        endpoint = "/api/v1/alerts/createAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alert_msg = conn.must_proto_response(response, _AlertService.Alert)
        return alert_msg

    def _update(self, alert_msg):
        alert_msg.id = self.id
        msg = _AlertService.UpdateAlertRequest(alert=alert_msg)
        endpoint = "/api/v1/alerts/updateAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()
        return True

    def _update_last_evaluated_at(self, last_evaluated_at=None):
        if last_evaluated_at is None:
            last_evaluated_at = time_utils.now()

        millis = time_utils.epoch_millis(last_evaluated_at)
        alert_msg = _AlertService.Alert(
            last_evaluated_at_millis=millis,
        )

        self._update(alert_msg)

    def add_notification_channels(self, notification_channels):
        """
        Add notification channels to this alert.

        Parameters
        ----------
        notification_channels : list of :class:`~verta.monitoring.notification_channel.entities.NotificationChannel`
            Notification channels.

        Examples
        --------
        .. code-block:: python

            from verta.monitoring.notification_channel import SlackNotificationChannel

            channels = Client().monitoring.notification_channels
            channel = notification_channels.create(
                "Slack alerts",
                SlackNotificationChannel("https://hooks.slack.com/services/.../.../......"),
            )

            alert.add_notification_channels([channel])

        """
        for channel in notification_channels:
            self._validate_notification_channel(channel)

        alert_msg = _AlertService.Alert()
        self._fetch_with_no_cache()
        alert_msg.CopyFrom(self._msg)
        alert_msg.notification_channels.update(
            {channel.id: True for channel in notification_channels}
        )

        self._update(alert_msg)

    def set_status(self, status, event_time=None):
        """
        Set the status of this alert.

        .. note::

            There should usually be no need to manually set an alert to
            alerting, as the Verta platform monitors alerts and their summary
            samples.

        Parameters
        ----------
        status : :mod:`~verta.monitoring.alert.status`
            Alert status.
        event_time : datetime.datetime or int, optional
            An override event time to assign to this alert status update.
            Either a timezone aware datetime object or unix epoch milliseconds.

        Examples
        --------
        .. code-block:: python

            from verta.monitoring.alert.status import Ok
            alert.set_status(Ok())

        """
        msg = status._to_proto_request()
        msg.alert_id = self.id
        if event_time:
            msg.event_time_millis = time_utils.epoch_millis(event_time)

        endpoint = "/api/v1/alerts/updateAlertStatus"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()
        return True

    def delete(self):
        """
        Delete this alert.

        Returns
        -------
        bool
            ``True`` if the delete was successful.

        Raises
        ------
        :class:`requests.HTTPError`
            If the delete failed.

        """
        msg = _AlertService.DeleteAlertRequest(ids=[self.id])
        endpoint = "/api/v1/alerts/deleteAlert"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class Alerts(object):
    """
    Collection object for creating and finding alerts.

    Parameters
    ----------
    conn
        A connection object to the backend service.
    conf
        A configuration object used by conn methods.
    monitored_entity_id : int, optional
        A monitored entity id to use for all alerts in this collection
    summary : :class:`~verta.monitoring.summaries.summary.Summary`, optional
        A summary for creating and finding alerts in this collection, and finding samples to alert on.

    Examples
    --------
    .. code-block:: python

        alerts = summary.alerts

    """

    def __init__(self, conn, conf, monitored_entity_id=None, summary=None):
        self._conn = conn
        self._conf = conf
        self._validate_summary_and_monitor(monitored_entity_id, summary)
        self._monitored_entity_id = (
            int(monitored_entity_id) if monitored_entity_id else None
        )
        self._summary = summary

    def create(
        self,
        name,
        alerter,
        notification_channels=None,
        labels=None,
        starting_from=None,
        _created_at=None,
        _updated_at=None,
        _last_evaluated_at=None,
    ):
        """
        Create a new alert.

        Parameters
        ----------
        name : str
            A unique name for this alert.
        alerter : :mod:`~verta.monitoring.alert`
            The configuration for this alert.
        notification_channels : list of :class:`~verta.monitoring.notification_channel.entities.NotificationChannel`, optional
            Channels for this alert to propagate notifications to.
        labels : dict of str to list of str, optional
            Alert on samples that have at least one of these labels. A mapping
            between label keys and lists of corresponding label values.
        starting_from : datetime.datetime or int, optional
            Alert on samples associated with periods after this time; useful
            for monitoring samples representing past data. Either a timezone
            aware datetime object or unix epoch milliseconds.

        Returns
        -------
        :class:`Alert`
            Alert.

        Examples
        --------
        .. code-block:: python

            alert = summary.alerts.create(
                name="MSE",
                alerter=alerter,
                notification_channels=[channel],
            )

        """
        if self._summary is None:
            raise RuntimeError(
                "this Alert cannot be used to create because it was not"
                " obtained via summary.alerts"
            )

        summary_sample_query = SummarySampleQuery(
            summary_query=self._build_summary_query(),
            labels=labels,
            time_window_start=time_utils.epoch_millis(starting_from),
        )

        if notification_channels is None:
            notification_channels = []
        for channel in notification_channels:
            Alert._validate_notification_channel(channel)

        ctx = _Context(self._conn, self._conf)
        return Alert._create(
            self._conn,
            self._conf,
            ctx,
            name=name,
            monitored_entity_id=(
                self._monitored_entity_id or self._summary.monitored_entity_id
            ),
            alerter=alerter,
            summary_sample_query=summary_sample_query,
            notification_channels=notification_channels,
            created_at_millis=time_utils.epoch_millis(_created_at),
            updated_at_millis=time_utils.epoch_millis(_updated_at),
            last_evaluated_at_millis=time_utils.epoch_millis(_last_evaluated_at),
        )

    def _build_summary_query(self):
        if self._summary:
            return SummaryQuery(
                ids=[self._summary.id],
                names=[self._summary.name],
                monitored_entities=[self._summary.monitored_entity_id],
            )
        elif self._monitored_entity_id:
            return SummaryQuery(
                monitored_entities=[self._monitored_entity_id],
            )
        else:
            return None

    @classmethod
    def _has_valid_summary_and_monitor(cls, monitored_entity_id, summary):
        if summary and monitored_entity_id:
            return summary.monitored_entity_id == monitored_entity_id
        else:
            return True

    @classmethod
    def _validate_summary_and_monitor(cls, monitored_entity_id, summary):
        if not cls._has_valid_summary_and_monitor(monitored_entity_id, summary):
            raise RuntimeError(
                "this Alerts object cannot be used because its summary is"
                " inconsistent with the provided monitored entity id"
            )

    def get(self, name=None, id=None):
        """
        Get an existing alert.

        Either `name` or `id` can be provided but not both.

        Parameters
        ----------
        name : str, optional
            Alert name.
        id : int, optional
            Alert ID.

        Returns
        -------
        :class:`Alert`
            Alert.

        """
        if name and id:
            raise ValueError("cannot specify both `name` and `id`")
        elif name:
            return Alert._get_by_name(
                self._conn,
                self._conf,
                name,
                self._monitored_entity_id,
            )
        elif id:
            return Alert._get_by_id(self._conn, self._conf, id)
        else:
            raise ValueError("must specify either `name` or `id`")

    # TODO: use lazy list and pagination
    # TODO: a proper find
    def list(self):
        """
        Return all accesible alerts.

        Returns
        -------
        list of :class:`Alert`
            Alerts.

        """
        msg = _AlertService.FindAlertRequest(
            page_number=1,
            page_limit=-1,
        )
        if self._monitored_entity_id is not None:
            msg.monitored_entity_ids.append(self._monitored_entity_id)
        endpoint = "/api/v1/alerts/findAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        alerts = self._conn.must_proto_response(response, msg.Response).alerts
        return [Alert(self._conn, self._conf, alert) for alert in alerts]

    def delete(self, alerts):
        """
        Delete the given alerts in a single request.

        Parameters
        ----------
        list of :class:`Alert`
            Alerts.

        Returns
        -------
        bool
            ``True`` if the delete was successful.

        Raises
        ------
        :class:`requests.HTTPError`
            If the delete failed.

        """
        alert_ids = arg_handler.extract_ids(alerts)
        msg = _AlertService.DeleteAlertRequest(ids=alert_ids)
        endpoint = "/api/v1/alerts/deleteAlert"
        response = self._conn.make_proto_request("DELETE", endpoint, body=msg)
        self._conn.must_response(response)
        return True


class AlertHistoryItem(object):
    """
    The history of an alert's status changes.

    Examples
    --------
    .. code-block:: python

        alert.history

    """

    def __init__(self, msg):
        self._event_time = time_utils.datetime_from_millis(msg.event_time_millis)
        self._status = status_module._AlertStatus._from_proto(
            msg.status,
            msg.violating_summary_sample_ids,
        )

    def __repr__(self):
        return "\n\t".join(
            (
                "AlertHistoryItem",
                "occurred at: {}".format(self._event_time),
                "status: {}".format(self._status),
            )
        )
