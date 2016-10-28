from modeldb.basic.ModelDBSyncerLight import *

# Creating a new project
name = "test1"
author = "srinidhi"
description = "pandas-logistic-regression"
SyncerObj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

print Syncer.instance.experiment
Syncer.instance = None

SyncerObj = Syncer(
    None,
    None,
    ExistingExperimentRun(60))

print Syncer.instance.experimentRun

Syncer.instance.sync()