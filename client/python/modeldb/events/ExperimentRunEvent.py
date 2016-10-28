from Event import *

class ExperimentRunEvent(Event):
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun

    def sync(self, syncer):
        thriftClient = syncer.client
        res = thriftClient.storeExperimentRunEvent(self.makeEvent(syncer))
        syncer.experimentRun.id = res.experimentRunId

    def makeEvent(self, syncer):
        return modeldb_types.ExperimentRunEvent(self.experimentRun)