"""
Event indicating that a dataFrame was split into smaller dataFrames.
"""
from modeldb.events.Event import *

class RandomSplitEvent(Event):
    """
    Class for creating and storing RandomSplitEvents
    """
    def __init__(self, df, weights, seed, result):
        self.df = df
        self.weights = weights
        self.seed = seed
        self.result = result

    def makeEvent(self, syncer):
        """
        Constructs a thrift RandomSplitEvent object with appropriate fields.
        """
        syncable_dataframe = syncer.convertDftoThrift(self.df)
        all_syncable_frames = []
        for dataframe in self.result:
            all_syncable_frames.append(syncer.convertDftoThrift(dataframe))
        re = modeldb_types.RandomSplitEvent(syncable_dataframe, self.weights, self.seed,
                                            all_syncable_frames, syncer.experimentRun.id)
        return re

    def associate(self, res, syncer):
        """
        Stores the server response ids for all split dataframes into dictionary.
        """
        df_id = id(self.df)
        syncer.storeObject(df_id, res.oldDataFrameId)
        for i in range(0, len(self.result)):
            syncer.storeObject(id(self.result[i]), res.splitIds[i])
        syncer.storeObject(self, res.splitEventId)

    def sync(self, syncer):
        """
        Stores RandomSplitEvent on the server.
        """
        re = self.makeEvent(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeRandomSplitEvent(re)
        self.associate(res, syncer)
