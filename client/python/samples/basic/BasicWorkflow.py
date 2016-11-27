from modeldb.basic.ModelDbSyncerBase import *

# syncer_obj = Syncer.create_syncer("gensim test", "test_user", \
#     "using modeldb light logging")

# syncer_obj = Syncer.create_syncer_from_config(
#     "/Users/mvartak/Projects/modeldb_test_dir/dir/.mdb_config")

syncer_obj = Syncer(
    NewOrExistingProject("gensim test", "test_user",
    "using modeldb light logging"),
    DefaultExperiment(),
    NewExperimentRun("", "A1B2C3"))

print syncer_obj.experiment

print "I'm training some model"

datasets = {
    "train" : Dataset("/path/to/train", {}),
    "test" : Dataset("/path/to/test", {})
}
model ="model_obj"
mdb_model1 = Model("NN", model, "/path/to/model1")
model_config1 = ModelConfig("NN", {"l1" : 10})
model_metrics1 = ModelMetrics({"accuracy" : 0.8})

mdb_model2 = Model("NN", model, "/path/to/model2")
model_config2 = ModelConfig("NN", {"l1" : 20})
model_metrics2 = ModelMetrics({"accuracy" : 0.9})

syncer_obj.sync_datasets(datasets)

syncer_obj.sync_model("train", model_config1, mdb_model1)
syncer_obj.sync_metrics("test", mdb_model1, model_metrics1)

syncer_obj.sync_model("train", model_config2, mdb_model2)
syncer_obj.sync_metrics("test", mdb_model2, model_metrics2)

syncer_obj.sync()

