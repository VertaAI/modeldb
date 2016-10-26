#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('../thrift/gen-py')
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types

class SyncProjectEvent:
    def __init__(self, project):
        self.project = project

    def sync(self, syncer):
        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeProjectEvent(modeldb_types.ProjectEvent(self.project))
        syncer.project.id = res.projectId
        