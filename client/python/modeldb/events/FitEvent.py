from Event import *

class FitEvent(Event):
    def __init__(self, model, spec, df):
        self.model = model
        self.spec = spec
        self.df = df

    def makeEvent(self, syncer):
        self.syncableTransformer = syncer.convertModeltoThrift(self.model)
        self.modelSpec = syncer.convertSpectoThrift(self.spec)
        self.syncableDataFrame = syncer.convertDftoThrift(self.df)
        self.columns = syncer.setColumns(self.df)
        fe = modeldb_types.FitEvent(self.syncableDataFrame, self.modelSpec, self.syncableTransformer, 
                            self.columns, [], [], syncer.experimentRun.id)
        return fe

    def associate(self, res, syncer):
        #generate identity for storing in dictionary
        dfImm = id(self.df)
        syncer.storeObject(dfImm,res.dfId)
        syncer.storeObject(self.spec, res.specId)
        syncer.storeObject(self.model, res.modelId)
        syncer.storeObject(self, res.eventId)

    def sync(self, syncer):
        fe = self.makeEvent(syncer)
        thriftClient = syncer.client
        res = thriftClient.storeFitEvent(fe)
        self.associate(res, syncer)