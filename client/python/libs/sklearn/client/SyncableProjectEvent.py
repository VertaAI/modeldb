#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

class SyncProjectEvent:
    def __init__(self, project):
        self.project = project

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeProjectEvent(ProjectEvent(self.project))
        syncer.project.id = res.projectId
        