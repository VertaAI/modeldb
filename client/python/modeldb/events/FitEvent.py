"""
Event indicating estimator was used to fit model.
"""
from modeldb.events.Event import *

class FitEvent(Event):
    """
    Class for creating and storing FitEvents
    """
    def __init__(self, model, spec, df):
        self.model = model
        self.spec = spec
        self.df = df

    def makeEvent(self, syncer):
        """
        Constructs a thrift FitEvent object with appropriate fields.
        """
        syncable_transformer = syncer.convertModeltoThrift(self.model)
        model_spec = syncer.convertSpectoThrift(self.spec)
        syncable_dataframe = syncer.convertDftoThrift(self.df)
        columns = syncer.setColumns(self.df)
        fe = modeldb_types.FitEvent(syncable_dataframe, model_spec,
                                    syncable_transformer, columns,
                                    [], [], syncer.experimentRun.id)
        return fe

    def associate(self, res, syncer):
        """
        Stores the generated ids into dictionary.
        """
        df_id = id(self.df)
        syncer.storeObject(df_id, res.dfId)
        syncer.storeObject(self.spec, res.specId)
        syncer.storeObject(self.model, res.modelId)
        syncer.storeObject(self, res.eventId)

    def sync(self, syncer):
        """
        Stores FitEvent on the server.
        """
        fe = self.makeEvent(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeFitEvent(fe)
        self.associate(res, syncer)
