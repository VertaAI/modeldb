from modeldb.basic.ModelDbSyncerBase import *
import yaml

# Create a syncer from a config file
syncer_obj = Syncer.create_syncer_from_config("YamlSample.yaml", None, None)

# sync_all can use both yaml and json files
filename = "YamlSample.yaml"
# filename = "JsonSample.json"

print "Syncing all data from file..."
syncer_obj.sync_all(filename)

syncer_obj.sync()

