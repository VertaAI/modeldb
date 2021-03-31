import os
from client import Client
from verta.environment import Python
from alerter import Alerter
import pandas as pd

from profilers import (
  MissingValuesProfiler
)

client = Client("https://monitoring.dev.verta.ai")
profiler = MissingValuesProfiler(columns=["age"])
profiler_name = "age_missing"
monitored_entity = client.get_or_create_monitored_entity("example-alerter")
data_source = client.get_or_create_data_source(monitored_entity=monitored_entity)


def create_profiler(registered_model, profiler, name, type, data_source=None, alert_definition=None):
  version = registered_model.get_or_create_version(name=name)
  version.add_attribute(key="type", value=type, overwrite=True)
  if data_source is not None:
    version.add_attribute(key="data_source_id", value=data_source.id, overwrite=True)
  if alert_definition is not None:
    version.add_attribute(key="alert_definition_id", value=alert_definition.id, overwrite=True)
  version.log_model(profiler, overwrite=True)
  version.log_environment(Python(requirements=[]), overwrite=True)
  profiler = client.profilers.create(name=name, model_version=version)
  print(
      "{} {} has id {} and references model version with ID {}".format(
          type, name, profiler.id, version.id
      )
  )
  return profiler

model = client.get_or_create_registered_model("example-alerter")

# 1. Create alert definition

alert_definition = client.alert_definitions.create(
    "test alert definition",
    monitored_entity=monitored_entity,
    reference_data_source=data_source,
    threshold=0.7,
    webhook="https://hooks.slack.com/services/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",
)
print("alert definition id: {}".format(alert_definition.id))

# 2. Create some summaries

summary_train = profiler.profile(pd.read_csv("census-train.csv"))
print("Training data summary: {}".format(summary_train))
summary_content = summary_train[profiler_name]
summary_train_ref = client.get_or_create_summary(
    name=profiler_name, content=summary_content, data_source=data_source, monitored_entity=monitored_entity
)
print("train summary has ID {}".format(summary_train_ref.id))

summary_test = profiler.profile(pd.read_csv("census-test.csv"))
print("Test data summary: {}".format(summary_test))
summary_content = summary_test[profiler_name]
summary_test_ref = client.get_or_create_summary(
    name=profiler_name, content=summary_content, data_source=data_source, monitored_entity=monitored_entity
)
print("test summary has ID {}".format(summary_test_ref.id))

summary_no_age = profiler.profile(pd.read_csv("census-no-age.csv"))
print("No age data summary: {}".format(summary_no_age))
summary_content = summary_no_age[profiler_name]
summary_no_age_ref = client.get_or_create_summary(
    name=profiler_name, content=summary_content, data_source=data_source, monitored_entity=monitored_entity
)
print("No age summary has ID {}".format(summary_test_ref.id))

# 3. Evaluate the alerter

os.environ['MONITORED_ENTITY_ID'] = "{}".format(monitored_entity.id)
os.environ['DATA_SOURCE_ID'] = "{}".format(data_source.id)
os.environ['SUMMARY_NAME'] = "age_missing"
os.environ['REFERENCE_SUMMARY_ID'] = "{}".format(summary_train.id)
os.environ['ALERT_DEFINITION_ID'] = "{}".format(alert_definition.id)

alerter = Alerter()
alerter.profile(df=None)

os.environ['SUMMARY_ID'] = "{}".format(summary_no_age_ref.id)
alerter = Alerter()
alerter.profile(df=None)
