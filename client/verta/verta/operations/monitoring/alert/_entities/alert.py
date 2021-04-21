# -*- coding: utf-8 -*-

import warnings

from ....._protos.public.monitoring import Alert_pb2 as _AlertService
from ....._internal_utils import _utils, time_utils
from ....._tracking import entity, _Context
from ... import notification_channel
from ... import summaries
from ... import utils
from .. import _alerter
from .. import status as status_module


class Alert(entity._ModelDBEntity):
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
    history : list of :class:`AlertHistoryItem`
        History of this alert's status changes.
    monitored_entity_id : int
        ID of the monitored entity this alert is associated with.
    status : :class:`~verta.operations.monitoring.alert.status._AlertStatus`
        Current status of this alert.
    summary_sample_query : :class:`~verta.operations.monitoring.summary.SummarySampleQuery`
        The summary samples this alert monitors.

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
                "created: {}".format(_utils.timestamp_to_str(msg.created_at_millis)),
                "updated: {}".format(_utils.timestamp_to_str(msg.updated_at_millis)),
                "last evaluated: {}".format(
                    _utils.timestamp_to_str(msg.last_evaluated_at_millis)
                ),
                "alerter: {}".format(
                    # TODO: use an `alerter` property that returns the actual class
                    _AlertService.AlerterTypeEnum.AlerterType.Name(msg.alerter_type)
                ),
                "violating summary sample ids: {}".format(
                    msg.violating_summary_sample_ids
                ),
                "summary sample query: {}".format(self.summary_sample_query),
                "notification channel ids: {}".format(
                    list(msg.notification_channels.keys())
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
    def history(self):
        # TODO: implement lazy list and pagination
        msg = _AlertService.ListAlertHistoryRequest(id=self.id)
        endpoint = "/api/v1/alerts/listAlertHistory"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        history = self._conn.must_proto_response(response, msg.Response).history
        return list(map(AlertHistoryItem, history))

    @property
    def _last_evaluated_or_created_millis(self):
        """For the alerter to filter for new summary samples."""
        self._refresh_cache()

        return self._msg.last_evaluated_at_millis or self._msg.created_at_millis

    @property
    def monitored_entity_id(self):
        self._refresh_cache()

        return self._msg.monitored_entity_id

    @property
    def name(self):
        self._refresh_cache()

        return self._msg.name

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

        return summaries.SummarySampleQuery._from_proto_request(
            self._msg.sample_find_base,
        )

    @staticmethod
    def _validate_notification_channel(channel):
        if not isinstance(channel, notification_channel._entities.NotificationChannel):
            raise TypeError(
                "notification channel must be an entity object returned"
                " from client.notification_channels.create(),"
                " not {}".format(type(channel))
            )

    @classmethod
    def _get_proto_by_id(cls, conn, id):
        msg = _AlertService.FindAlertRequest(
            ids=[int(id)], page_number=1, page_limit=-1,
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
            page_number=1, page_limit=-1,
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
        if msg.alert.alerter_type == _AlertService.AlerterTypeEnum.FIXED:
            msg.alert.alerter_fixed.CopyFrom(alerter._as_proto())
        elif msg.alert.alerter_type == _AlertService.AlerterTypeEnum.REFERENCE:
            msg.alert.alerter_reference.CopyFrom(alerter._as_proto())
        else:
            raise ValueError(
                "unrecognized alert type enum value {}".format(msg.alert.alerter_type)
            )

        endpoint = "/api/v1/alerts/createAlert"
        response = conn.make_proto_request("POST", endpoint, body=msg)
        alert_msg = conn.must_proto_response(response, _AlertService.Alert)
        return alert_msg

    def _update(self, alert_msg):
        msg = _AlertService.UpdateAlertRequest(alert=alert_msg)
        endpoint = "/api/v1/alerts/updateAlert"
        response = self._conn.make_proto_request("POST", endpoint, body=msg)
        self._conn.must_response(response)
        self._clear_cache()
        return True

    def _update_last_evaluated_at(self, last_evaluated_at=None):
        if last_evaluated_at is None:
            last_evaluated_at = time_utils.now()

        alert_msg = _AlertService.Alert()
        self._fetch_with_no_cache()
        alert_msg.CopyFrom(self._msg)
        alert_msg.last_evaluated_at_millis = time_utils.epoch_millis(last_evaluated_at)

        self._update(alert_msg)

    def add_notification_channels(self, notification_channels):
        """
        Add notification channels to this alert.

        Parameters
        ----------
        notification_channels : list of :class:`~verta.operations.monitoring.notification_channel._entities.NotificationChannel`
            Notification channels.

        Examples
        --------
        .. code-block:: python

            from verta.operations.monitoring.notification_channel import SlackNotificationChannel

            channels = Client().operations.notification_channels
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
        status : :class:`~verta.operations.monitoring.alert.status._AlertStatus`
            Alert status.
        event_time : datetime.datetime or int, optional
            An override event time to assign to this alert status update.
            Either a timezone aware datetime object or unix epoch milliseconds.

        Examples
        --------
        .. code-block:: python

            from verta.operations.monitoring.alert.status import Ok
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

    Examples
    --------
    .. code-block:: python

        alerts = monitored_entity.alerts

    """

    def __init__(self, conn, conf, monitored_entity_id=None):
        self._conn = conn
        self._conf = conf
        self._monitored_entity_id = (
            int(monitored_entity_id) if monitored_entity_id else None
        )

    def create(
        self,
        name,
        alerter,
        summary_sample_query,
        notification_channels=None,
        created_at=None,
        updated_at=None,
        last_evaluated_at=None,
    ):
        """
        Create a new alert.

        Parameters
        ----------
        name : str
            A unique name for this alert.
        alerter : :class:`~verta.operations.monitoring.alert.Alerter`
            The configuration for this alert.
        summary_sample_query : :class:`~verta.operations.monitoring.summaries.SummarySampleQuery`
            Summary samples for this alert to monitor for threshold violations.
        notification_channels : list of :class:`~verta.operations.monitoring.notification_channel._entities.NotificationChannel`, optional
        created_at : datetime.datetime or int, optional
            An override creation time to assign to this alert. Either a
            timezone aware datetime object or unix epoch milliseconds.
        updated_at : datetime.datetime or int, optional
            An override update time to assign to this alert. Either a
            timezone aware datetime object or unix epoch milliseconds.
        last_evaluated_at : datetime.datetime or int, optional
            An override evaluation time to assign to this alert. Either a
            timezone aware datetime object or unix epoch milliseconds.

        Returns
        -------
        :class:`Alert`
            Alert.

        Examples
        --------
        .. code-block:: python

            alert = monitored_entity.alerts.create(
                name="MSE",
                alerter=alerter,
                summary_sample_query=sample_query,
                notification_channels=[channel],
            )

        """
        if self._monitored_entity_id is None:
            raise RuntimeError(
                "this Alert cannot be used to create because it was not"
                " obtained via monitored_entity.alerts"
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
            monitored_entity_id=self._monitored_entity_id,
            alerter=alerter,
            summary_sample_query=summary_sample_query,
            notification_channels=notification_channels,
            created_at_millis=time_utils.epoch_millis(created_at),
            updated_at_millis=time_utils.epoch_millis(updated_at),
            last_evaluated_at_millis=time_utils.epoch_millis(last_evaluated_at),
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
            page_number=1, page_limit=-1,
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
        alert_ids = utils.extract_ids(alerts)
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
