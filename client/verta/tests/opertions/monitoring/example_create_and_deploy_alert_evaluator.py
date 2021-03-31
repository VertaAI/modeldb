from client import Client
from alerter import Alerter
from data_formats import DiscreteHistogram
from verta.environment import Python
import time
import os
from profilers import MissingValuesProfiler
import random
from _protos.private.monitoring.DataMonitoringService_pb2 import SeverityEnum

client = Client("https://dev.verta.ai")
monitored_entity = client.get_or_create_monitored_entity()
reference_data_source = monitored_entity.get_or_create_data_source()

source_profiler = client.profilers.upload("age missing values", MissingValuesProfiler(columns=["age"]))
source_profiler.enable(monitored_entity, wait=True)

def generate_summary(time_window_start_at_millis=None, time_window_end_at_millis=None):
  summary_name="age_missing"
  age_present = random.randint(0, 1000)
  age_missing = random.random() < 0.5
  labels = ["not_age"] if age_missing else ["age"]
  content = DiscreteHistogram(values=[age_present], labels=labels)

  try:
    return client.get_or_create_summary(
        name=summary_name, content=content, data_source=reference_data_source, monitored_entity=monitored_entity, time_window_start_at_millis=time_window_start_at_millis, time_window_end_at_millis=time_window_end_at_millis
    )
  except:
    # duplicate summary, skip it
    print("Summary exists.")


# create the reference summary
reference_summary = generate_summary()

alert_evaluator = reference_data_source.alerts.upload(
    "age alert evaluator",
    alerter=Alerter(),
    threshold=float(0.5),
    severity=SeverityEnum.MEDIUM,
    webhook=""
)

# NB: These vars are defaulted to inside `enable`, but this shows how one can pass alternative values to override the defaults
#     If you just want to run the alert with your locally defined email and key, there is no need to pass an environment dictionary.
env_vars = {
  "VERTA_EMAIL": os.environ["VERTA_EMAIL"],
  "VERTA_DEV_KEY": os.environ["VERTA_DEV_KEY"]
}
alert_evaluator.enable(reference_summary, environment=env_vars, wait=True)
print("Alert definition ID {}".format(alert_evaluator.id))

now_seconds = int(time.time())

# Generate 5 summaries spanning the previous 5 minutes
for i in range(0, 5):
  generate_summary(time_window_start_at_millis=(now_seconds - ((i + 1) * 60)) * 1000, time_window_end_at_millis=(now_seconds - (i * 60)) * 1000)

print("Generating 1 summary per minute")

# Generate 1 summary per minute for the next 5 minutes
for i in range(0,5):
  time.sleep(60)
  generate_summary()
