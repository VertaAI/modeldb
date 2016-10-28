from Event import *

class ProjectEvent(Event):
    def __init__(self, project):
        self.project = project

    def sync(self, syncer):
        thriftClient = syncer.client
        res = thriftClient.storeProjectEvent(modeldb_types.ProjectEvent(self.project))
        syncer.project.id = res.projectId
        