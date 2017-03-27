"""
Event indicating that a dataFrame was split into smaller dataFrames.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types


class RandomSplitEvent(Event):
    """
    Class for creating and storing RandomSplitEvents
    """

    def __init__(self, df, weights, seed, result):
        self.df = df
        self.weights = weights
        self.seed = seed
        self.result = result

    def make_event(self, syncer):
        """
        Constructs a thrift RandomSplitEvent object with appropriate fields.
        """
        syncable_dataframe = syncer.convert_df_to_thrift(self.df)
        all_syncable_frames = []
        for dataframe in self.result:
            all_syncable_frames.append(syncer.convert_df_to_thrift(dataframe))
        re = modeldb_types.RandomSplitEvent(
            syncable_dataframe, self.weights, self.seed, all_syncable_frames,
            syncer.experiment_run.id)
        return re

    def associate(self, res, syncer):
        """
        Stores the server response ids for all split dataframes into
        dictionary.
        """
        syncer.store_object(self.df, res.oldDataFrameId)
        for i in range(0, len(self.result)):
            syncer.store_object(self.result[i], res.splitIds[i])
        syncer.store_object(self, res.splitEventId)

    def sync(self, syncer):
        """
        Stores RandomSplitEvent on the server.
        """
        re = self.make_event(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeRandomSplitEvent(re)
        self.associate(res, syncer)
