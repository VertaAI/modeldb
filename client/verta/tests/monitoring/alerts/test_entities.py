# -*- coding: utf-8 -*-

import datetime
from collections import namedtuple
import pytest

from verta._internal_utils import (
    _utils,
    time_utils,
)
from verta.monitoring import comparison
from verta.monitoring.alert import (
    FixedAlerter,
    RangeAlerter,
    ReferenceAlerter,
)
from verta.monitoring.alert.status import (
    Alerting,
    Ok,
)
from verta.monitoring.alert.entities import Alert, Alerts
from verta.monitoring.summaries.queries import SummarySampleQuery
from verta.monitoring.notification_channel import (
    SlackNotificationChannel,
)
from verta import data_types


class TestIntegration:
    """Alerts + related entities/objects."""

    def test_add_notification_channels(
        self,
        client,
        summary,
        created_entities,
    ):
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))

        channel1 = client.monitoring.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )
        created_entities.append(channel1)
        channel2 = client.monitoring.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )
        created_entities.append(channel2)

        alert = summary.alerts.create(
            name,
            alerter,
            notification_channels=[channel1],
        )
        retrieved_channel_ids = alert._msg.notification_channels.keys()
        assert set(retrieved_channel_ids) == {channel1.id}

        alert.add_notification_channels([channel2])
        alert._refresh_cache()
        retrieved_channel_ids = alert._msg.notification_channels.keys()
        assert set(retrieved_channel_ids) == {channel1.id, channel2.id}

    def test_set_status(self, summary, summary_sample):
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))

        alert = summary.alerts.create(name, alerter)
        assert alert.status == Ok()
        assert alert.last_evaluated_at is None

        alert.set_status(Alerting([summary_sample]))
        assert alert.status == Alerting([summary_sample])
        assert alert.last_evaluated_at == time_utils.datetime_from_millis(
            alert._msg.last_evaluated_at_millis
        )

        alert.set_status(Ok())
        assert alert.status == Ok()


class TestAlert:
    """Tests that aren't specific to an alerter type."""

    def test_update_last_evaluated_at(self, summary):
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))

        alert = summary.alerts.create(name, alerter)
        alert._fetch_with_no_cache()
        initial = alert._msg.last_evaluated_at_millis

        alert._update_last_evaluated_at()
        alert._fetch_with_no_cache()
        assert alert._msg.last_evaluated_at_millis > initial

        yesterday = time_utils.now() - datetime.timedelta(days=1)
        yesterday_millis = time_utils.epoch_millis(yesterday)
        # TODO: remove following line when backend stops round to nearest sec
        yesterday_millis = round(yesterday_millis, -3)
        alert._update_last_evaluated_at(yesterday)
        alert._fetch_with_no_cache()
        assert alert._msg.last_evaluated_at_millis == yesterday_millis

    def test_creation_query_params(self, summary):
        """`labels` and `starting_from`"""
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        labels = {"datasource": ["census2010", "census2020"]}
        starting_from = datetime.datetime(year=2021, month=5, day=10, tzinfo=time_utils.utc)

        # none passed
        alert = summary.alerts.create(
            name,
            alerter,
        )
        expected_sample_query = SummarySampleQuery(
            summary_query=summary.alerts._build_summary_query(),
            created_after=alert.created_at,
        )
        assert alert.summary_sample_query == expected_sample_query
        assert alert.labels == {}
        assert alert.starting_from is None

        # just labels
        alert = summary.alerts.create(
            name,
            alerter,
            labels=labels,
        )
        expected_sample_query = SummarySampleQuery(
            summary_query=summary.alerts._build_summary_query(),
            labels=labels,
            created_after=alert.created_at,
        )
        assert alert.summary_sample_query == expected_sample_query
        assert alert.labels == labels
        assert alert.starting_from is None

        # starting_from
        alert = summary.alerts.create(
            name,
            alerter,
            labels=labels,
            starting_from=starting_from,
        )
        expected_sample_query = SummarySampleQuery(
            summary_query=summary.alerts._build_summary_query(),
            labels=labels,
            time_window_start=starting_from,
            created_after=alert.created_at,
        )
        assert alert.summary_sample_query == expected_sample_query
        assert alert.labels == labels
        assert alert.starting_from == starting_from

    def test_creation_override_datetimes(self, summary, strs):
        strs = iter(strs)
        alerter = FixedAlerter(comparison.GreaterThan(0.7))

        created_at = time_utils.now() - datetime.timedelta(weeks=1)
        updated_at = time_utils.now() - datetime.timedelta(days=1)
        last_evaluated_at = time_utils.now() - datetime.timedelta(hours=1)
        created_at_millis = time_utils.epoch_millis(created_at)
        updated_at_millis = time_utils.epoch_millis(updated_at)
        last_evaluated_at_millis = time_utils.epoch_millis(last_evaluated_at)

        # as datetime
        alert = summary.alerts.create(
            next(strs),
            alerter,
            _created_at=created_at,
            _updated_at=updated_at,
            _last_evaluated_at=last_evaluated_at,
        )
        assert alert._msg.created_at_millis == created_at_millis
        assert alert._msg.updated_at_millis == updated_at_millis
        assert alert._msg.last_evaluated_at_millis == last_evaluated_at_millis

        # as millis
        alert = summary.alerts.create(
            next(strs),
            alerter,
            _created_at=created_at_millis,
            _updated_at=updated_at_millis,
            _last_evaluated_at=last_evaluated_at_millis,
        )
        assert alert._msg.created_at_millis == created_at_millis
        assert alert._msg.updated_at_millis == updated_at_millis
        assert alert._msg.last_evaluated_at_millis == last_evaluated_at_millis

    def test_alerts_summary(self):
        MockSummary = namedtuple("Summary", ["id", "name", "monitored_entity_id"])

        monitored_entity_id = 5
        summary = MockSummary(123, "my_test_summary", monitored_entity_id)
        offline_alerts = Alerts(
            None,
            None,
            monitored_entity_id=monitored_entity_id,
            summary=summary,
        )
        query = offline_alerts._build_summary_query()
        assert query
        assert summary.id in query._ids
        assert summary.name in query._names
        assert summary.monitored_entity_id in query._monitored_entity_ids


class TestNonReferenceAlerters:
    @pytest.mark.parametrize(
        "alerter", [FixedAlerter(comparison.GreaterThan(0.7)), RangeAlerter(-1.0, 1.0)]
    )
    def test_crud(self, client, summary, alerter):
        name = _utils.generate_default_name()

        created_alert = summary.alerts.create(name, alerter)
        assert isinstance(created_alert, Alert)
        assert created_alert._msg.alerter_type == alerter._TYPE
        assert created_alert.monitored_entity_id == summary.monitored_entity_id
        assert summary.id in created_alert.summary_sample_query.summary_query._ids

        retrieved_alert = summary.alerts.get(id=created_alert.id)
        client_retrieved_alert = client.monitoring.alerts.get(id=created_alert.id)
        assert retrieved_alert.id == client_retrieved_alert.id
        assert isinstance(retrieved_alert, Alert)
        assert retrieved_alert._msg.alerter_type == alerter._TYPE
        assert retrieved_alert.alerter._as_proto() == alerter._as_proto()

        listed_alerts = summary.alerts.list()
        assert created_alert.id in map(lambda a: a.id, listed_alerts)
        client_listed_alerts = client.monitoring.alerts.list()
        assert created_alert.id in map(lambda a: a.id, client_listed_alerts)

        assert summary.alerts.delete([created_alert])

    @pytest.mark.parametrize(
        "alerter", [FixedAlerter(comparison.GreaterThan(0.7)), RangeAlerter(-1.0, 1.0)]
    )
    def test_repr(self, summary, alerter):
        """__repr__() does not raise exceptions"""
        name = _utils.generate_default_name()
        created_alert = summary.alerts.create(name, alerter)
        assert repr(created_alert)

        retrieved_alert = summary.alerts.get(id=created_alert.id)
        assert repr(retrieved_alert)


class TestReference:
    def test_crud(self, client, summary, summary_sample):
        name = _utils.generate_default_name()
        alerter = ReferenceAlerter(comparison.GreaterThan(0.7), summary_sample)

        created_alert = summary.alerts.create(name, alerter)
        assert isinstance(created_alert, Alert)
        assert created_alert._msg.alerter_type == alerter._TYPE
        assert created_alert.monitored_entity_id == summary.monitored_entity_id
        assert summary.id in created_alert.summary_sample_query.summary_query._ids

        retrieved_alert = summary.alerts.get(id=created_alert.id)
        client_retrieved_alert = client.monitoring.alerts.get(id=created_alert.id)
        assert retrieved_alert.id == client_retrieved_alert.id
        assert isinstance(retrieved_alert, Alert)
        assert retrieved_alert._msg.alerter_type == alerter._TYPE
        assert retrieved_alert.alerter._as_proto() == alerter._as_proto()
        assert retrieved_alert.alerter._reference_sample_id == summary_sample.id

        listed_alerts = summary.alerts.list()
        assert created_alert.id in map(lambda a: a.id, listed_alerts)
        client_listed_alerts = client.monitoring.alerts.list()
        assert created_alert.id in map(lambda a: a.id, client_listed_alerts)

        assert summary.alerts.delete([created_alert])

    def test_repr(self, summary, summary_sample):
        """__repr__() does not raise exceptions"""
        name = _utils.generate_default_name()
        alerter = ReferenceAlerter(comparison.GreaterThan(0.7), summary_sample)

        created_alert = summary.alerts.create(name, alerter)
        assert repr(created_alert)

        retrieved_alert = summary.alerts.get(id=created_alert.id)
        assert repr(retrieved_alert)
