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
# data may be specified in the config
data = {'train' : '/path/to/train', 'test' : '/path/to/test'}
metrics = {'accuracy' : 0.9, 'rce' : 0.5}
# this may be an object or path
model = '/path/to/model'

SyncerObj.syncData()
SyncerObj.syncModel()
SyncerObj.syncMetrics()

SyncerObj.syncAll(ExperimentRunInfo(data, config, model, metrics))

result = SyncerObj.syncModel('/path/to/train', '/path/to/model', \
    'LinearRegression', config)

SyncerObj.instance.sync()

# data paths 
# model path
# metrics
# hyperparams
# config