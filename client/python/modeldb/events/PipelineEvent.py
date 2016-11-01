from Event import *

#This class creates and stores a pipeline event in the database.
class PipelineEvent(Event):
    def __init__(self, firstPipelineEvent, transformStages, fitStages):
        self.firstPipelineEvent = firstPipelineEvent
        self.transformStages = transformStages
        self.fitStages = fitStages

    #Creates a pipeline event, using the captured transform and fit stages
    def makeEvent(self, syncer):
        pipelineFirstFitEvent = self.firstPipelineEvent.makeEvent(syncer)
        transformEventStages = []
        fitEventStages = []
        for index, transformEvent in self.transformStages:
            transformEventStages.append(modeldb_types.PipelineTransformStage(index, transformEvent.makeEvent(syncer)))
        for index, fitEvent in self.fitStages:
            fitEventStages.append(modeldb_types.PipelineFitStage(index, fitEvent.makeEvent(syncer)))
        pe = modeldb_types.PipelineEvent(pipelineFirstFitEvent, transformEventStages, fitEventStages, syncer.experimentRun.id)
        return pe

    #Stores each of the individual fit/transform events
    def associate(self, res, syncer):
        self.firstPipelineEvent.associate(res.pipelineFitResponse, syncer)
        for (transformRes, (index, te)) in zip(res.transformStagesResponses, self.transformStages):
            te.associate(transformRes, syncer)
        for (fitRes, (index, fe)) in zip(res.fitStagesResponses, self.fitStages):
            fe.associate(fitRes, syncer)

    def sync(self, syncer):
        pe = self.makeEvent(syncer)
        thriftClient = syncer.client
        res = thriftClient.storePipelineEvent(pe)
        self.associate(res, syncer)

