from client import Client
from verta._internal_utils._utils import generate_default_name

client = Client("https://dev.verta.ai")
client.set_project("monitored entities")
client.set_experiment("monitored entity")
run = client.set_experiment_run()

name = "monitored_entity:{}".format(generate_default_name())
create_monitored = client.get_or_create_monitored_entity(name=name)
get_monitored = client.get_or_create_monitored_entity(name=name)

assert create_monitored
assert get_monitored
assert create_monitored.id == get_monitored.id
assert create_monitored.name == get_monitored.name
print("created and retrieved monitored entity\n{}".format(get_monitored))
