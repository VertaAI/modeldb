from client import Client
from verta.environment import Python
from clients.profilers import ProfilerReference
from verta._internal_utils._utils import generate_default_name
from profilers import ContinuousHistogramProfiler
from alerter import Alerter

client = Client("https://dev.verta.ai")
client.set_project("example alerts")

# 0. Set up contextually needed entities
profiler_name = "age_column_profiler_{}".format(generate_default_name())
profiler_ref = client.profilers.upload(profiler_name, ContinuousHistogramProfiler(columns=["age"]))

monitored_name = "monitored_entity:{}".format(generate_default_name())
monitored_entity = client.get_or_create_monitored_entity(name=monitored_name)

print("monitored entity created...")

reference_data_source = monitored_entity.get_or_create_data_source("data_source")

print("setup complete...")

# 1. Create alert definition
alert_definition = reference_data_source.alerts.upload(
    "age alert evaluator",
    profiler=profiler_ref,
    alerter=Alerter(),
    threshold=float(0.5)
)

assert alert_definition
print("created test alert definition: {}".format(alert_definition))

# 2. Create alert
alert = client.alerts.create("test alert", alert_definition)

assert alert
print("created test alert: {}".format(alert))
