#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types

class SyncExperimentEvent:
    def __init__(self, experiment):
        self.experiment = experiment

    def sync(self, syncer):
        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeExperimentEvent(modeldb_types.ExperimentEvent(self.experiment))
        syncer.experiment.id = res.experimentId
