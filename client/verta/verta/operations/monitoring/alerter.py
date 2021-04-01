import os
import time
from .client import Client
from .profilers import Profiler


class Alerter(Profiler):
    def __init__(self):
        super(Alerter, self).__init__([])


    def get_comparison_summary(self, client, monitored_entity_id, data_source_id, name=None, id=None):
        if name is not None and id is not None:
            raise ValueError("cannot specify both `name` and `id`")
        if name is not None:
            timestamp_millis = int((time.time() - 60) * 1000)
            print("Using timestamp {}".format(timestamp_millis))
            summary = client.summaries.find_summary(
                            monitored_entity_id,
                            data_source_id,
                            name,
                            int(0), # fetch the most recent summary saved
                        )
            return summary
        else:
            return client.get_or_create_summary(id=id)


    def profile(self, df):
        client = Client("https://dev.verta.ai")
        monitored_entity_id = int(os.environ["MONITORED_ENTITY_ID"])
        data_source_id = int(os.environ["DATA_SOURCE_ID"])
        summary_name = os.getenv("SUMMARY_NAME")
        summary_id = int(os.getenv("SUMMARY_ID")) if os.getenv("SUMMARY_ID") else None
        reference_summary_id = int(os.environ["REFERENCE_SUMMARY_ID"])
        alert_definition_id = int(os.environ["ALERT_DEFINITION_ID"])
        print("Alerter evaluating for monitored entity {}, data source {}, summary name {}, summary ID {}".format(monitored_entity_id, data_source_id, summary_name, summary_id))

        comparison_summary = self.get_comparison_summary(client=client, monitored_entity_id=monitored_entity_id, data_source_id=data_source_id, name=summary_name, id=summary_id)
        if comparison_summary is None:
            print("Comparison summary not found.")
            return
        print("Comparison summary: {}".format(comparison_summary))
        comparison_content = comparison_summary.content

        reference_summary = client.summaries.get_or_create(id=reference_summary_id)
        if reference_summary is None:
            print("Reference summary not found for ID {}".format(reference_summary_id))
            return
        print("Reference summary: {}".format(reference_summary))
        reference_content = reference_summary.content

        diff = comparison_content.diff(reference_content)
        print("Summary diff: {}".format(diff))
        alert_definition = client.alert_definitions.get(alert_definition_id)
        if alert_definition is None:
            print("Comparison summary not found for ID {}".format(alert_definition_id))
            return
        threshold = 0.7 if alert_definition._msg.threshold is None else alert_definition._msg.threshold
        print("Found alert threshold {}".format(threshold))
        if diff > threshold:
            print("Alert threshold exceeded: threshold is {}".format(threshold))
            # client.alerts.create("Threshold violation", alert_definition)
        else:
            print("No violation.")
