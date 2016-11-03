"""
Store Experiment Run on server.
"""
from modeldb.events.Event import *

class ExperimentRunEvent(Event):
    """
    Class for creating and storing ExperimentRunEvents
    """
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun

    def makeEvent(self, syncer):
        """
        Constructs a thrift ExperimentRunEvent object with appropriate fields.
        """
        return modeldb_types.ExperimentRunEvent(self.experimentRun)

    def sync(self, syncer):
        """
        Stores ExperimentRunEvent on the server.
        """
        thrift_client = syncer.client
        res = thrift_client.storeExperimentRunEvent(self.makeEvent(syncer))
        syncer.experimentRun.id = res.experimentRunId
