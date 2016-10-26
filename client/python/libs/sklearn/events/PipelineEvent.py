#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
import modeldb.ttypes as modeldb_types
import FitEvent
import TransformEvent

#This class creates and stores a pipeline event in the database.
class SyncPipelineEvent:
    def __init__(self, firstPipelineEvent, transformStages, fitStages):
        self.firstPipelineEvent = firstPipelineEvent
        self.transformStages = transformStages
        self.fitStages = fitStages

    #Creates a pipeline event, using the captured transform and fit stages
    def makeEvent(self, syncer):
        pipelineFirstFitEvent = self.firstPipelineEvent.makeEvent(syncer)
        transformEventStages = []
        fitEventStages = []
        self.experimentRunId = syncer.experimentRun.id
        for index, transformEvent in self.transformStages:
            transformEventStages.append(modeldb_types.PipelineTransformStage(index, transformEvent.makeEvent(syncer)))
        for index, fitEvent in self.fitStages:
            fitEventStages.append(modeldb_types.PipelineFitStage(index, fitEvent.makeEvent(syncer)))
        pe = modeldb_types.PipelineEvent(pipelineFirstFitEvent, transformEventStages, fitEventStages, self.experimentRunId)
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

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storePipelineEvent(pe)
        self.associate(res, syncer)

#Overrides the Pipeline model's fit function
def fitFnPipeline(self,X,y):
    #Check if pipeline contains valid estimators and transformers
    checkValidPipeline(self.steps)

    #Make Fit Event for overall pipeline
    pipelineModel = self.fit(X,y)
    pipelineFit = FitEvent.SyncFitEvent(pipelineModel, self, X)

    #Extract all the estimators from pipeline
    #All estimators call 'fit' and 'transform' except the last estimator (which only calls 'fit')
    names, sk_estimators = zip(*self.steps)
    estimators = sk_estimators[:-1]
    lastEstimator = sk_estimators[-1]

    transformStages = []
    fitStages = []
    curDataset = X

    for index, estimator in enumerate(estimators):
        oldDf = curDataset
        model = estimator.fit(oldDf, y)
        transformedOutput = model.transform(oldDf)

        #Convert transformed output into a proper pandas DataFrame object
        if type(transformedOutput) is np.ndarray:
            newDf = pd.DataFrame(transformedOutput)
        else:
            newDf = pd.DataFrame(transformedOutput.toarray())

        curDataset = transformedOutput

        #populate the stages
        transformEvent = TransformEvent.SyncTransformEvent(oldDf, newDf, model)
        transformStages.append((index, transformEvent))
        fitEvent = FitEvent.SyncFitEvent(model, estimator, oldDf)
        fitStages.append((index, fitEvent))

    #Handle last estimator, which has a fit method (and may not have transform)
    oldDf = curDataset
    model = lastEstimator.fit(oldDf, y)
    fitEvent = FitEvent.SyncFitEvent(model, estimator, oldDf)
    fitStages.append((index+1, fitEvent))

    #Create the pipeline event with all components
    pipelineEvent = SyncPipelineEvent(pipelineFit, transformStages, fitStages)

    ModelDbSyncer.Syncer.instance.addToBuffer(pipelineEvent)

#Helper function to check whether a pipeline is constructed properly. Taken from original sklearn pipeline source code with minor modifications, which are commented below.
def checkValidPipeline(steps):
    names, estimators = zip(*steps)
    transforms = estimators[:-1]
    estimator = estimators[-1]

    for t in transforms:
        #Change from original scikit: checking for "fit" and "transform" methods, rather than "fit_transform" as each event is logged separately to database
        if (not (hasattr(t, "fit")) and hasattr(t, "transform")):
            raise TypeError("All intermediate steps of the chain should "
                            "be transforms and implement fit and transform"
                            " '%s' (type %s) doesn't)" % (t, type(t)))

    if not hasattr(estimator, "fit"):
        raise TypeError("Last step of chain should implement fit "
                        "'%s' (type %s) doesn't)"
                        % (estimator, type(estimator)))

