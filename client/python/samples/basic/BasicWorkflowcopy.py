from modeldb.basic.ModelDbSyncerBase import *
import sys

# Create a syncer using a convenience API
syncer_obj = Syncer.create_syncer("gensim test", "test_user", \
    "using modeldb light logging")

# Example: Create a syncer from a config file
# syncer_obj = Syncer.create_syncer_from_config(
#     "/Users/mvartak/Projects/modeldb_test_dir/dir/.mdb_config")

# Example: Create a syncer explicitly
# syncer_obj = Syncer(
#     NewOrExistingProject("gensim test", "test_user",
#     "using modeldb light logging"),
#     DefaultExperiment(),
#     NewExperimentRun("", "sha_A1B2C3D4"))

# Example: Create a syncer from an existing experiment run
# experiment_run_id = int(sys.argv[len(sys.argv) - 1])
# syncer_obj = Syncer.create_syncer_for_experiment_run(experiment_run_id)

print "I'm training some model"
metadata = ""
syncer_obj.sync_all(metadata)

syncer_obj.sync()

