"""
Event indicating a Transformer transformed a dataframe.
"""
from modeldb.events.Event import *

class TransformEvent(Event):
    """
    Class for creating and storing TransformEvents
    """
    def __init__(self, oldDf, newDf, transformer):
        self.old_df = oldDf
        self.new_df = newDf
        self.transformer = transformer

    def makeEvent(self, syncer):
        """
        Constructs a thrift TransformEvent object with appropriate fields.
        """
        syncable_transformer = syncer.convertModeltoThrift(self.transformer)
        syncable_dataframe_old = syncer.convertDftoThrift(self.old_df)
        syncable_dataframe_new = syncer.convertDftoThrift(self.new_df)
        te = modeldb_types.TransformEvent(syncable_dataframe_old, syncable_dataframe_new,
                                          syncable_transformer, [], [],
                                          syncer.experimentRun.id)
        return te

    def associate(self, res, syncer):
        """
        Stores the server response ids into dictionary.
        """
        df_old_id = id(self.old_df)
        df_new_id = id(self.new_df)
        syncer.storeObject(df_old_id, res.oldDataFrameId)
        syncer.storeObject(df_new_id, res.newDataFrameId)
        syncer.storeObject(self.transformer, res.transformerId)
        syncer.storeObject(self, res.eventId)

    def sync(self, syncer):
        """
        Stores TransformEvent on the server.
        """
        te = self.makeEvent(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeTransformEvent(te)
        self.associate(res, syncer)
