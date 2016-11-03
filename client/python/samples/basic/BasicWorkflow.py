from modeldb.basic.ModelDbSyncerBase import *

# Creating a new project
name = "gensim test"
author = "test_user"
description = "using modeldb light logging"
SyncerObj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

print Syncer.instance.experiment

print "I'm training some model"

config = {'l1' : 0.3, 'l2' : 0.4}
result = SyncerObj.syncModel('/path/to/train', '/path/to/model', \
    'LinearRegression', config)

# syncModel(self, trainDf, model_path, model_type, config, features=[]):

# SyncerObj = Syncer(
#     None,
#     None,
#     ExistingExperimentRun(60))

# print Syncer.instance.experimentRun

SyncerObj.instance.sync()

# data paths 
# model path
# metrics
# hyperparams
# config