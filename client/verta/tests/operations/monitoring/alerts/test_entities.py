# -*- coding: utf-8 -*-

import datetime

from verta._internal_utils import (
    _utils,
    time_utils,
)
from verta.common import comparison
from verta.operations.monitoring.alert import (
    FixedAlerter,
    ReferenceAlerter,
)
from verta.operations.monitoring.alert.status import (
    Alerting,
    Ok,
)
from verta.operations.monitoring.alert import _entities
from verta.operations.monitoring.summaries import SummarySampleQuery
from verta.operations.monitoring.notification_channel import (
    SlackNotificationChannel,
)


class TestIntegration:
    """Alerts + related entities/objects."""

    def test_add_notification_channels(
        self,
        client,
        monitored_entity,
        created_entities,
    ):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        channel1 = client.operations.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )
        created_entities.append(channel1)
        channel2 = client.operations.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )
        created_entities.append(channel2)

        alert = alerts.create(
            name,
            alerter,
            sample_query,
            notification_channels=[channel1],
        )
        retrieved_channel_ids = alert._msg.notification_channels.keys()
        assert set(retrieved_channel_ids) == {channel1.id}

        alert.add_notification_channels([channel2])
        alert._refresh_cache()
        retrieved_channel_ids = alert._msg.notification_channels.keys()
        assert set(retrieved_channel_ids) == {channel1.id, channel2.id}

    def test_set_status(self, monitored_entity, summary_sample, created_entities):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        alert = alerts.create(name, alerter, sample_query)
        assert alert.status == Ok()
        assert alert._last_evaluated_or_created_millis == alert._msg.created_at_millis

        alert.set_status(Alerting([summary_sample]))
        assert alert.status == Alerting([summary_sample])
        assert alert._last_evaluated_or_created_millis == alert._msg.last_evaluated_at_millis

        alert.set_status(Ok())
        assert alert.status == Ok()

    def test_summary_sample_query(self, monitored_entity, created_entities):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        alert = alerts.create(name, alerter, sample_query)
        created_query_proto = sample_query._to_proto_request()
        retrieved_query_proto = alert.summary_sample_query._to_proto_request()
        assert created_query_proto == retrieved_query_proto


class TestAlert:
    """Tests that aren't specific to an alerter type."""

    def test_update_last_evaluated_at(self, monitored_entity, created_entities):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        alert = alerts.create(name, alerter, sample_query)
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

    def test_creation_datetime(self, monitored_entity, strs, created_entities):
        strs = iter(strs)
        alerts = monitored_entity.alerts
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        created_at = time_utils.now() - datetime.timedelta(weeks=1)
        updated_at = time_utils.now() - datetime.timedelta(days=1)
        last_evaluated_at = time_utils.now() - datetime.timedelta(hours=1)
        created_at_millis = time_utils.epoch_millis(created_at)
        updated_at_millis = time_utils.epoch_millis(updated_at)
        last_evaluated_at_millis = time_utils.epoch_millis(last_evaluated_at)

        # as datetime
        alert = alerts.create(
            next(strs),
            alerter,
            sample_query,
            created_at=created_at,
            updated_at=updated_at,
            last_evaluated_at=last_evaluated_at,
        )
        assert alert._msg.created_at_millis == created_at_millis
        assert alert._msg.updated_at_millis == updated_at_millis
        assert alert._msg.last_evaluated_at_millis == last_evaluated_at_millis

        # as millis
        alert = alerts.create(
            next(strs),
            alerter,
            sample_query,
            created_at=created_at_millis,
            updated_at=updated_at_millis,
            last_evaluated_at=last_evaluated_at_millis,
        )
        assert alert._msg.created_at_millis == created_at_millis
        assert alert._msg.updated_at_millis == updated_at_millis
        assert alert._msg.last_evaluated_at_millis == last_evaluated_at_millis


class TestFixed:
    def test_crud(self, client, monitored_entity):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert isinstance(created_alert, _entities.Alert)
        assert created_alert._msg.alerter_type == alerter._TYPE
        assert created_alert.monitored_entity_id == monitored_entity.id

        retrieved_alert = alerts.get(id=created_alert.id)
        client_retrieved_alert = client.operations.alerts.get(id=created_alert.id)
        assert retrieved_alert.id == client_retrieved_alert.id
        assert isinstance(retrieved_alert, _entities.Alert)
        assert retrieved_alert._msg.alerter_type == alerter._TYPE
        assert retrieved_alert.alerter._as_proto() == alerter._as_proto()

        listed_alerts = alerts.list()
        assert created_alert.id in map(lambda a: a.id, listed_alerts)
        client_listed_alerts = client.operations.alerts.list()
        assert created_alert.id in map(lambda a: a.id, client_listed_alerts)

        assert alerts.delete([created_alert])

    def test_repr(self, monitored_entity, created_entities):
        """__repr__() does not raise exceptions"""
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(comparison.GreaterThan(0.7))
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert repr(created_alert)

        retrieved_alert = alerts.get(id=created_alert.id)
        assert repr(retrieved_alert)


class TestReference:
    def test_crud(self, client, monitored_entity, summary_sample):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = ReferenceAlerter(comparison.GreaterThan(0.7), summary_sample)
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert isinstance(created_alert, _entities.Alert)
        assert created_alert._msg.alerter_type == alerter._TYPE
        assert created_alert.monitored_entity_id == monitored_entity.id

        retrieved_alert = alerts.get(id=created_alert.id)
        client_retrieved_alert = client.operations.alerts.get(id=created_alert.id)
        assert retrieved_alert.id == client_retrieved_alert.id
        assert isinstance(retrieved_alert, _entities.Alert)
        assert retrieved_alert._msg.alerter_type == alerter._TYPE
        assert retrieved_alert.alerter._as_proto() == alerter._as_proto()
        assert retrieved_alert.alerter._reference_sample_id == summary_sample.id

        listed_alerts = alerts.list()
        assert created_alert.id in map(lambda a: a.id, listed_alerts)
        client_listed_alerts = client.operations.alerts.list()
        assert created_alert.id in map(lambda a: a.id, client_listed_alerts)

        assert alerts.delete([created_alert])

    def test_repr(self, monitored_entity, created_entities, summary_sample):
        """__repr__() does not raise exceptions"""
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = ReferenceAlerter(comparison.GreaterThan(0.7), summary_sample)
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert repr(created_alert)

        retrieved_alert = alerts.get(id=created_alert.id)
        assert repr(retrieved_alert)
