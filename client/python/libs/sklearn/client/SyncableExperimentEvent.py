#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

class SyncExperimentEvent:
    def __init__(self, experiment):
        self.experiment = experiment

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeExperimentEvent(ExperimentEvent(self.experiment))
        syncer.experiment.id = res.experimentId