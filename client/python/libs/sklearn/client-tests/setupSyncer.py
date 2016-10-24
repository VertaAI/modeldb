import numpy as np
import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model
import client.SyncableRandomSplit as SyncableRandomSplit
import client.SyncableMetrics as SyncableMetrics
from client.ModelDbSyncer import *
from client.ModelDbSyncerTest import *

name = "logistic-test"
author = "srinidhi"
description = "income-level logistic regression"
SyncerObj = SyncerTest(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))
