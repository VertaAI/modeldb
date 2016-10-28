from Event import *

class ExperimentRunEvent(Event):
    def __init__(self, experimentRun):
        self.experimentRun = experimentRun

    def sync(self, syncer):
        thriftClient = syncer.client
        res = thriftClient.storeExperimentRunEvent(modeldb_types.ExperimentRunEvent(self.experimentRun))
        syncer.experimentRun.id = res.experimentRunId