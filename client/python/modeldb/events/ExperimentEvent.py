from Event import *

class ExperimentEvent(Event):
    def __init__(self, experiment):
        self.experiment = experiment

    def sync(self, syncer):
        thriftClient = syncer.client
        res = thriftClient.storeExperimentEvent(modeldb_types.ExperimentEvent(self.experiment))
        syncer.experiment.id = res.experimentId
