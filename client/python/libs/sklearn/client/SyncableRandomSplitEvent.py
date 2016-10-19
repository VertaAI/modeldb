#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

class SyncRandomSplitEvent:
    def __init__(self, df, weights, seed, result, experimentRunId):
        self.df = df
        self.weights = weights
        self.seed = seed
        self.result = result
        self.experimentRunId = experimentRunId

    def makeRandomSplitEvent(self):
        syncer = ModelDbSyncer.Syncer.instance
        self.syncableDataFrame = syncer.convertDftoThrift(self.df)

        allSyncableFrames = []
        for dataFrame in self.result:
            allSyncableFrames.append(syncer.convertDftoThrift(dataFrame))
        re = RandomSplitEvent(self.syncableDataFrame, self.weights, self.seed, allSyncableFrames, syncer.project.id, self.experimentRunId)
        return re

    def associate(self,res):
        syncer = ModelDbSyncer.Syncer.instance

         #generate identity for storing in dictionary
        dfImmOld = id(self.df)

        syncer.storeObject(dfImmOld,res.oldDataFrameId)
        for i in range(0, len(self.result)):
            syncer.storeObject(id(self.result[i]), res.splitIds[i])
        syncer.storeObject(self, res.splitEventId)

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance
        re = self.makeRandomSplitEvent()

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeRandomSplitEvent(re)
        self.associate(res)