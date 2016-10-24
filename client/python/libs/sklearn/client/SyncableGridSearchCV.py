#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import SyncableFitEvent
import GridCrossValidation
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *

#This class creates and stores a Grid-Search Cross Validation event in the database.
class SyncGridCVEvent:
    def __init__(self, inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds, experimentRunId):
        self.inputDataFrame = inputDataFrame
        self.crossValidations = crossValidations
        self.seed = seed
        self.evaluator = evaluator
        self.bestModel = bestModel
        self.bestEstimator = bestEstimator
        self.numFolds = numFolds
        self.experimentRunId = experimentRunId

    #Helper function to create a CrossValidationFold object, later used in making CrossValidationEvents.
    def makeCrossValidationFold(self, fold):
        syncer = ModelDbSyncer.Syncer.instance
        [(transformer, validationSet, trainingSet, score)] = fold
        syncableDataFrameValidSet = syncer.convertDftoThrift(validationSet)
        syncableDataFrameTrainSet = syncer.convertDftoThrift(trainingSet)
        syncableTransformer = syncer.convertModeltoThrift(transformer)
        return CrossValidationFold(syncableTransformer, syncableDataFrameValidSet, syncableDataFrameTrainSet, score)

    #Helper function to create CrossValidationEvent.
    def makeCrossValidation(self, estimator, crossValidationFolds):
        syncer = ModelDbSyncer.Syncer.instance
        syncableDataFrame = syncer.convertDftoThrift(self.inputDataFrame)
        syncableEstimator = syncer.convertSpectoThrift(estimator,self.inputDataFrame)
        # TODO: Need to add meaningful label/feature/prediction column names
        return CrossValidationEvent(syncableDataFrame, syncableEstimator, self.seed, self.evaluator,
                                            [""], [""], [""], crossValidationFolds, syncer.project.id, self.experimentRunId)

    #Returns a list of CrossValidationEvents, used for creating GridSearchCrossValidationEvent.
    def makeCrossValidationEvents(self):
        crossValidationEvents = []
        for estimator in self.crossValidations:
            crossValidationFolds = []
            for fold in self.crossValidations[estimator]:
                crossValidationFolds.append(self.makeCrossValidationFold(fold))
            crossValidationEvents.append(self.makeCrossValidation(estimator, crossValidationFolds))
        return crossValidationEvents

    #Creates a GridSearchCrossValidationEvent
    def makeGridSearchCVEvent(self, crossValidationEvents):
        syncer = ModelDbSyncer.Syncer.instance
        fitEvent = SyncableFitEvent.SyncFitEvent(self.bestModel, self.bestEstimator, self.inputDataFrame, self.experimentRunId)
        gscve = GridSearchCrossValidationEvent(self.numFolds, fitEvent.makeFitEvent(), crossValidationEvents, syncer.project.id, self.experimentRunId)
        return gscve

    #Stores each of the associated events.
    def associate(self, res):
        syncer = ModelDbSyncer.Syncer.instance

        #First store the fit event
        dfImm = id(self.inputDataFrame)
        syncer.storeObject(self,res.eventId)
        syncer.storeObject(self.bestEstimator, res.fitEventResponse.specId)
        syncer.storeObject(dfImm, res.fitEventResponse.dfId)
        syncer.storeObject(self.bestModel, res.fitEventResponse.modelId)

        #Store each cross validation from the grid
        for cv, cver in zip(self.crossValidations.items(), res.crossValidationEventResponses):
            estimator, folds = cv
            syncer.storeObject(cver.specId, estimator)

            #Iterate through each fold
            for pair in zip(folds, cver.foldResponses):
                folds, foldr = pair
                fold = folds[0]
                dfImmValid = id(fold[1])
                dfImmTrain = id(fold[2])

                syncer.storeObject(fold[0], foldr.modelId)
                syncer.storeObject(dfImmValid, foldr.validationId)
                syncer.storeObject(dfImmTrain, foldr.trainingId)

    def sync(self):
        syncer = ModelDbSyncer.Syncer.instance
        crossValidationEvents = self.makeCrossValidationEvents()
        gscve = self.makeGridSearchCVEvent(crossValidationEvents)

        #Invoking thrift client
        thriftClient = syncer.client
        res = thriftClient.storeGridSearchCrossValidationEvent(gscve)
        self.associate(res)

#Overrides GridSearchCV's fit function.
def fitFnGridSearch(self, X,y):
    GridCrossValidation.fit(self,X,y)
    [inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds] = self.gridCVevent

    #Calls SyncGridCVEvent and adds to buffer.
    gridEvent = SyncGridCVEvent(inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds, ModelDbSyncer.Syncer.instance.experimentRun.id)
    ModelDbSyncer.Syncer.instance.addToBuffer(gridEvent)