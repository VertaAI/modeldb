"""
Store Experiment on server.
"""
from modeldb.events.Event import *

class ExperimentEvent(Event):
    """
    Class for creating and storing ExperimentEvents
    """
    def __init__(self, experiment):
        self.experiment = experiment

    def makeEvent(self, syncer):
        """
        Constructs a thrift ExperimentEvent object with appropriate fields.
        """
        return modeldb_types.ExperimentEvent(self.experiment)

    def sync(self, syncer):
        """
        Stores ExperimentEvent on the server.
        """
        thrift_client = syncer.client
        res = thrift_client.storeExperimentEvent(self.makeEvent(syncer))
        syncer.experiment.id = res.experimentId
