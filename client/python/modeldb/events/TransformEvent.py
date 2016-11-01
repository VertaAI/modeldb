from Event import *

class TransformEvent(Event):
    def __init__(self, oldDf, newDf, transformer):
        self.oldDf = oldDf
        self.newDf = newDf
        self.transformer = transformer

    def makeEvent(self, syncer):
        self.syncableTransformer = syncer.convertModeltoThrift(self.transformer)
        self.syncableDataFrameOld = syncer.convertDftoThrift(self.oldDf)
        self.syncableDataFrameNew = syncer.convertDftoThrift(self.newDf)
        te = modeldb_types.TransformEvent(self.syncableDataFrameOld, self.syncableDataFrameNew, 
                                    self.syncableTransformer, [], [], 
                                    syncer.experimentRun.id)
        return te

    def associate(self, res, syncer):
        #generate identity for storing in dictionary
        dfImmOld = id(self.oldDf)
        dfImmNew = id(self.newDf)
        syncer.storeObject(dfImmOld,res.oldDataFrameId)
        syncer.storeObject(dfImmNew,res.newDataFrameId)
        syncer.storeObject(self.transformer, res.transformerId)
        syncer.storeObject(self, res.eventId)

    def sync(self, syncer):
        te = self.makeEvent(syncer)
        thriftClient = syncer.client
        res = thriftClient.storeTransformEvent(te)
        self.associate(res, syncer)
