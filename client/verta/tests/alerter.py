# -*- coding: utf-8 -*-

import logging

import requests

from verta.operations.monitoring.alert._entities import Alerts
from verta.operations.monitoring.alert import (
    FixedAlerter,
    ReferenceAlerter,
)
from verta.operations.monitoring.alert.status import (
    Alerting,
)
from verta.operations.monitoring.notification_channel import (
    SlackNotificationChannel,
)
from verta._internal_utils import _utils
from verta import data_types


logger = logging.getLogger(__name__)


class Alerter(object):
    def __init__(self, client):
        self.client = client
        self.channel_notifier = ChannelNotifier(client)
        self.threshold_checker = ThresholdChecker()

    def process_alerts(self):
        alerts = Alerts(
            self.client._conn, self.client._conf, monitored_entity_id,  # TODO
        ).list()
        for alert in alerts:
            self.evaluate_alert(alert)

    def evaluate_alert(self, alert):
        # TODO: use alert.sample_find_base to query for summary samples
        for comparison_sample in summary_samples:
            if self.threshold_checker.exceeds_threshold(comparison_sample, alert):
                alert.set_status(Alerting(comparison_sample.id))
                self.channel_notifier.notify_channels(alert)

    def find_summary_samples(self, alert):
        raise NotImplementedError


class ChannelNotifier(object):
    def __init__(self, client):
        self.client = client

    def notify_channels(self, alert):
        for channel_id in alert._msg.notification_channels.keys():
            channel = self.client.notification_channels.get(id=channel_id)

            if channel._msg.type == SlackNotificationChannel._TYPE:
                self.fire_slack_webhook(alert, channel)
            else:
                logger.error(" ".join((
                    "unrecognized channel type {}".format(channel._msg.type),
                    "for channel ID {}".format(channel.id),
                    "on alert ID {}".format(alert.id),
                )))

    def fire_slack_webhook(self, alert, notification_channel):
        response = requests.post(
            notification_channel._msg.slack_webhook.url,
            json=self.build_slack_webhook_body(alert),
        )

        try:
            self.client._conn.must_response(response)
        except requests.HTTPError as e:
            logger.error("Failed to fire webhook: {}".format(e))

    def build_slack_webhook_body(self, alert):
        header_text = "Verta Monitoring Alert"
        webapp_url = "{}://{}/{}/monitoring/{}/live-alerts".format(
            self.client._conn.scheme,
            self.client._conn.socket,
            "Personal",  # TODO: use workspace name when available
            alert._msg.monitored_entity_id,
        )
        alert_text = "<{}|*{}*> has exceeded threshold {}".format(
            webapp_url,
            alert._msg.name,
            "foo",
        )
        timestamp = _utils.now()//1000
        date_text = "*Fired*\n<!date^{}^{{date_num}} {{time_secs}}|{}>".format(
            timestamp,
            timestamp,
        )

        return {
            "blocks": [
                {
                    "type": "header",
                    "text": {
                        "type": "plain_text",
                        "text": header_text,
                    },
                }, {
                    "type": "section",
                    "fields": [
                        {
                            "type": "mrkdwn",
                            "text": alert_text,
                        }, {
                            "type": "mrkdwn",
                            "text": date_text,
                        },
                    ],
                },
            ],
        }


class ThresholdChecker(object):
    @classmethod
    def exceeds_threshold(cls, comparison_sample, alert):
        if alert._msg.alerter_type == FixedAlerter._TYPE:
            return cls.exceeds_fixed_threshold(
                comparison_sample,
                alert._msg.alerter_fixed.threshold,
            )
        elif alert._msg.alerter_type == ReferenceAlerter._TYPE:
            return cls.exceeds_reference_threshold(
                comparison_sample,
                reference_sample,  # TODO
                alert._msg.alerter_reference.threshold,
            )
        else:
            logger.error(" ".join((
                "unrecognized alerter type {}".format(alert.msg.alerter_type),
                "for alert ID {}".format(alert.id),
            )))


    @staticmethod
    def exceeds_fixed_threshold(comparison_sample, threshold):
        reference_sample = data_types.NumericValue(threshold)
        diff = comparison_sample.content.dist(reference_sample)
        return bool(diff)

    @staticmethod
    def exceeds_reference_threshold(comparison_sample, reference_sample, threshold):
        diff = comparison_sample.content.dist(reference_sample)
        return diff >= threshold
