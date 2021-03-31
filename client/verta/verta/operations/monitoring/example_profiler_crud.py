# -*- coding: utf-8 -*-

from client import Client
from verta.environment import Python
from clients.profilers import ProfilerReference
from verta._internal_utils._utils import generate_default_name
from profilers import ContinuousHistogramProfiler


# 0. Setup Context

client = Client("https://dev.verta.ai")
client.set_project("test profilers")

monitored_entity = client.get_or_create_monitored_entity(name="monitored_entity_{}".format(generate_default_name()))
assert monitored_entity
print("created monitored entity\n{}".format(monitored_entity))

# 1. Create/Upload Profiler and Update

profiler_name = "age_column_profiler_{}".format(generate_default_name())
python_env = Python(requirements=["numpy", "scipy", "pandas"])
profiler = client.profilers.upload(profiler_name, ContinuousHistogramProfiler(columns=["age"]), environment=python_env)

assert isinstance(profiler, ProfilerReference)

retrieved_profiler = client.profilers.get(profiler.id)
assert retrieved_profiler

listed_profilers = client.profilers.list()
assert len(listed_profilers) > 1

old_name = profiler.name
old_profiler_version = profiler.reference
new_name = "profiler2_{}".format(generate_default_name())


# Commented out due to bug in profiler reference update SQL

# profiler.update(new_name)
#
# assert profiler.name == new_name
# assert profiler.name != old_name
# assert old_profiler_version == profiler.reference # This should not have changed

# 2. Deploy Profiler

enabled = profiler.enable(monitored_entity, {}, wait=True)
print("enabled? {}".format(enabled))

status = profiler.get_status(monitored_entity)
print("status: {}".format(status))

disabled = profiler.disable(monitored_entity)
print("disabled? {}".format(disabled))

# 3. Delete the profiler

delete = client.profilers.delete(profiler)
assert delete

# End

print("completed test of profilers client interface")
