#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types

class SyncMetricEvent:
    def __init__(self, df, model,labelCol, predictionCol, metricType, metricValue, experimentRunId):
        self.df = df
        self.model = model
        self.metricType = metricType
        self.metricValue = metricValue
        self.labelCol = labelCol
        self.predictionCol = predictionCol
        self.experimentRunId = experimentRunId

    def makeMetricEvent(self):
        syncer = ModelDbSyncer.Syncer.instance
        self.syncableTransformer = syncer.convertModeltoThrift(self.model)
        self.syncableDataFrame = syncer.convertDftoThrift(self.df)
        me = modeldb_types.MetricEvent(self.syncableDataFrame, self.syncableTransformer, self.metricType,
                                self.metricValue, self.labelCol, self.predictionCol, self.experimentRunId)
        return me

    def associate(self, res):
        syncer = ModelDbSyncer.Syncer.instance
        #generate identity for storing in dictionary
        dfImm = id(self.df)

        syncer.storeObject(dfImm,res.dfId)
        syncer.storeObject(self.model, res.modelId)
        syncer.storeObject(self, res.eventId)

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance
        me = self.makeMetricEvent()

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeMetricEvent(me)
        self.associate(res)