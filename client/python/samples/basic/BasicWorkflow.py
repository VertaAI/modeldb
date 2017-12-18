from modeldb.basic.Structs import (
    Model, ModelConfig, ModelMetrics, Dataset)
from modeldb.basic.ModelDbSyncerBase import Syncer


# Create a syncer using a convenience API
# syncer_obj = Syncer.create_syncer("gensim test", "test_user", \
#     "using modeldb light logging")

# Example: Create a syncer from a config file
syncer_obj = Syncer.create_syncer_from_config(
    "[MODELDB_ROOT]/client/syncer.json")

# Example: Create a syncer explicitly
# syncer_obj = Syncer(
#     NewOrExistingProject("gensim test", "test_user",
#     "using modeldb light logging"),
#     DefaultExperiment(),
#     NewExperimentRun("", "sha_A1B2C3D4"))

# Example: Create a syncer from an existing experiment run
# experiment_run_id = int(sys.argv[len(sys.argv) - 1])
# syncer_obj = Syncer.create_syncer_for_experiment_run(experiment_run_id)

print("I'm training some model")

datasets = {
    "train": Dataset("/path/to/train", {"num_cols": 15, "dist": "random"}),
    "test": Dataset("/path/to/test", {"num_cols": 15, "dist": "gaussian"})
}
model = "model_obj"
mdb_model1 = Model("NN", model, "/path/to/model1")
model_config1 = ModelConfig("NN", {"l1": 10})
model_metrics1 = ModelMetrics({"accuracy": 0.8})

mdb_model2 = Model("NN", model, "/path/to/model2")
model_config2 = ModelConfig("NN", {"l1": 20})
model_metrics2 = ModelMetrics({"accuracy": 0.9})

syncer_obj.sync_datasets(datasets)

syncer_obj.sync_model("train", model_config1, mdb_model1)
syncer_obj.sync_metrics("test", mdb_model1, model_metrics1)

syncer_obj.sync_model("train", model_config2, mdb_model2)
syncer_obj.sync_metrics("test", mdb_model2, model_metrics2)

syncer_obj.sync()
