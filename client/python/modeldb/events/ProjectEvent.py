from Event import *

class ProjectEvent(Event):
    def __init__(self, project):
        self.project = project

    def sync(self, syncer):
        thriftClient = syncer.client
        res = thriftClient.storeProjectEvent(self.makeEvent(syncer))
        syncer.project.id = res.projectId
        
    def makeEvent(self, syncer):
        return modeldb_types.ProjectEvent(self.project)