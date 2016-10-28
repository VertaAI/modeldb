from Event import *

class ExperimentEvent(Event):
    def __init__(self, experiment):
        self.experiment = experiment

    def sync(self, syncer):
    	print("THRIFT SYNCT")
        thriftClient = syncer.client
        res = thriftClient.storeExperimentEvent(self.makeEvent(syncer))
        syncer.experiment.id = res.experimentId

    def makeEvent(self, syncer):
        return modeldb_types.ExperimentEvent(self.experiment)
