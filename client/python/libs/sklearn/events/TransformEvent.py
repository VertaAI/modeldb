#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types

class SyncTransformEvent:
    def __init__(self, oldDf, newDf, transformer, experimentRunId):
        self.oldDf = oldDf
        self.newDf = newDf
        self.transformer = transformer
        self.experimentRunId = experimentRunId

    def makeTransformEvent(self):
        syncer = ModelDbSyncer.Syncer.instance
        self.syncableTransformer = syncer.convertModeltoThrift(self.transformer)
        self.syncableDataFrameOld = syncer.convertDftoThrift(self.oldDf)
        self.syncableDataFrameNew = syncer.convertDftoThrift(self.newDf)
        te = modeldb_types.TransformEvent(self.syncableDataFrameOld, self.syncableDataFrameNew, 
                                    self.syncableTransformer, [], [], 
                                    self.experimentRunId)
        return te

    def associate(self, res):
        syncer = ModelDbSyncer.Syncer.instance

        #generate identity for storing in dictionary
        dfImmOld = id(self.oldDf)
        dfImmNew = id(self.newDf)

        syncer.storeObject(dfImmOld,res.oldDataFrameId)
        syncer.storeObject(dfImmNew,res.newDataFrameId)
        syncer.storeObject(self.transformer, res.transformerId)
        syncer.storeObject(self, res.eventId)

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance
        te = self.makeTransformEvent()

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeTransformEvent(te)
        self.associate(res)