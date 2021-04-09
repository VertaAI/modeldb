# -*- coding: utf-8 -*-

from verta._internal_utils import _utils
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

    def test_add_notification_channels(self, client, monitored_entity):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(0.7)
        sample_query = SummarySampleQuery()

        channel1 = client.operations.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )
        channel2 = client.operations.notification_channels.create(
            _utils.generate_default_name(),
            SlackNotificationChannel(_utils.generate_default_name()),
        )

        alert = alerts.create(
            name,
            alerter,
            sample_query,
            notification_channels=[channel1],
        )
        assert alert._msg.notification_channels.keys() == {channel1.id}

        alert.add_notification_channels([channel2])
        alert._refresh_cache()
        assert alert._msg.notification_channels.keys() == {channel1.id, channel2.id}

    def test_set_status(self, monitored_entity, summary_sample):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(0.7)
        sample_query = SummarySampleQuery()

        alert = alerts.create(name, alerter, sample_query)
        assert alert.status == Ok()

        alert.set_status(Alerting(), summary_sample)
        assert alert.status == Alerting()

        alert.set_status(Ok())
        assert alert.status == Ok()


class TestFixed:
    def test_crud(self, client, monitored_entity):
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(0.7)
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert isinstance(created_alert, _entities.Alert)
        assert created_alert._msg.alerter_type == alerter._TYPE

        retrieved_alert = alerts.get(id=created_alert.id)
        client_retrieved_alert = client.operations.alerts.get(id=created_alert.id)
        assert retrieved_alert.id == client_retrieved_alert.id
        assert isinstance(retrieved_alert, _entities.Alert)
        assert retrieved_alert._msg.alerter_type == alerter._TYPE

        listed_alerts = alerts.list()
        assert created_alert.id in map(lambda a: a.id, listed_alerts)
        client_listed_alerts = client.operations.alerts.list()
        assert created_alert.id in map(lambda a: a.id, client_listed_alerts)

        assert alerts.delete([created_alert])

    def test_repr(self, monitored_entity):
        """__repr__() does not raise exceptions"""
        alerts = monitored_entity.alerts
        name = _utils.generate_default_name()
        alerter = FixedAlerter(0.7)
        sample_query = SummarySampleQuery()

        created_alert = alerts.create(name, alerter, sample_query)
        assert repr(created_alert)

        retrieved_alert = alerts.get(id=created_alert.id)
        assert repr(retrieved_alert)
