#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

class SyncFitEvent:
    def __init__(self, model, spec, df, experimentRunId):
        self.model = model
        self.spec = spec
        self.df = df
        self.experimentRunId = experimentRunId

    def makeFitEvent(self):
        syncer = ModelDbSyncer.Syncer.instance
        self.syncableTransformer = syncer.convertModeltoThrift(self.model)
        self.modelSpec = syncer.convertSpectoThrift(self.spec,self.df)
        self.syncableDataFrame = syncer.convertDftoThrift(self.df)
        fe = FitEvent(self.syncableDataFrame, self.modelSpec, self.syncableTransformer, 
                            [], [], [], self.experimentRunId)
        return fe

    def associate(self, res):
        syncer = ModelDbSyncer.Syncer.instance
        #generate identity for storing in dictionary
        dfImm = id(self.df)

        syncer.storeObject(dfImm,res.dfId)
        syncer.storeObject(self.spec,res.specId)
        syncer.storeObject(self.model, res.modelId)
        syncer.storeObject(self, res.eventId)

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance
        fe = self.makeFitEvent()

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeFitEvent(fe)
        self.associate(res)