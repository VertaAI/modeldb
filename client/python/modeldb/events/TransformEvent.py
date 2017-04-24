"""
Event indicating a Transformer transformed a dataframe.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types


class TransformEvent(Event):
    """
    Class for creating and storing TransformEvents
    """

    def __init__(self, old_df, new_df, transformer):
        self.old_df = old_df
        self.new_df = new_df
        self.transformer = transformer

    def make_event(self, syncer):
        """
        Constructs a thrift TransformEvent object with appropriate fields.
        """
        syncable_transformer = syncer.convert_model_to_thrift(self.transformer)
        syncable_dataframe_old = syncer.convert_df_to_thrift(self.old_df)
        syncable_dataframe_new = syncer.convert_df_to_thrift(self.new_df)
        te = modeldb_types.TransformEvent(
            syncable_dataframe_old, syncable_dataframe_new,
            syncable_transformer, [], [],
            syncer.experiment_run.id)
        return te

    def associate(self, res, syncer):
        """
        Stores the server response ids into dictionary.
        """
        syncer.store_object(self.old_df, res.oldDataFrameId)
        syncer.store_object(self.new_df, res.newDataFrameId)
        syncer.store_object(self.transformer, res.transformerId)
        syncer.store_object(self, res.eventId)

    def sync(self, syncer):
        """
        Stores TransformEvent on the server.
        """
        te = self.make_event(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeTransformEvent(te)
        self.associate(res, syncer)
