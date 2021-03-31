from profilers import (
    MissingValuesProfiler,
    BinaryHistogramProfiler,
    ContinuousHistogramProfiler,
)

from verta._internal_utils._utils import generate_default_name
from verta.environment import Python

from client import Client

client = Client("https://dev.verta.ai")
monitored_entity = client.get_or_create_monitored_entity("example-live-profiling-demo")
python_env = Python(requirements=["numpy", "scipy", "pandas"])
profiler = client.profilers.upload("age_missing", MissingValuesProfiler(columns=["age"]), environment=python_env)

status = profiler.enable(monitored_entity, wait=True)
print("Status: {}".format(status))
status = profiler.disable(monitored_entity)
print("Status: {}".format(status))
