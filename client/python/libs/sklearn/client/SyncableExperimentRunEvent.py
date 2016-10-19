#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

class SyncExperimentRunEvent:
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeExperimentRunEvent(ExperimentRunEvent(self.experimentRun))
        syncer.experimentRun.id = res.experimentRunId