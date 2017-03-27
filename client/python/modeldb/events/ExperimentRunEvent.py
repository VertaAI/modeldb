"""
Store Experiment Run on server.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types


class ExperimentRunEvent(Event):
    """
    Class for creating and storing experiment_run_events
    """

    def __init__(self, experiment_run):
        self.experiment_run = experiment_run

    def make_event(self, syncer):
        """
        Constructs a thrift experiment_run_event object with appropriate
        fields.
        """
        return modeldb_types.ExperimentRunEvent(self.experiment_run)

    def sync(self, syncer):
        """
        Stores experiment_run_event on the server.
        """
        thrift_client = syncer.client
        res = thrift_client.storeExperimentRunEvent(self.make_event(syncer))
        syncer.experiment_run.id = res.experimentRunId
