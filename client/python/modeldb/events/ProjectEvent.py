"""
Store Project on server.
"""
from modeldb.events.Event import Event
from ..thrift.modeldb import ttypes as modeldb_types


class ProjectEvent(Event):
    """
    Class for creating and storing ProjectEvents
    """

    def __init__(self, project):
        self.project = project

    def make_event(self, syncer):
        """
        Constructs a thrift ProjectEvent object with appropriate fields.
        """
        return modeldb_types.ProjectEvent(self.project)

    def sync(self, syncer):
        """
        Stores ProjectEvent on the server.
        """
        thrift_client = syncer.client
        res = thrift_client.storeProjectEvent(self.make_event(syncer))
        syncer.project.id = res.projectId
