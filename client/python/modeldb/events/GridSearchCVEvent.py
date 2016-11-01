from Event import *
from modeldb.events import FitEvent

class GridSearchCVEvent(Event):
    def __init__(self, inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds):
        self.inputDataFrame = inputDataFrame
        self.crossValidations = crossValidations
        self.seed = seed
        self.evaluator = evaluator
        self.bestModel = bestModel
        self.bestEstimator = bestEstimator
        self.numFolds = numFolds

    #Helper function to create a CrossValidationFold object, later used in making CrossValidationEvents.
    def makeCrossValidationFold(self, fold, syncer):
        [(transformer, validationSet, trainingSet, score)] = fold
        syncableDataFrameValidSet = syncer.convertDftoThrift(validationSet)
        syncableDataFrameTrainSet = syncer.convertDftoThrift(trainingSet)
        syncableTransformer = syncer.convertModeltoThrift(transformer)
        return modeldb_types.CrossValidationFold(syncableTransformer, syncableDataFrameValidSet, syncableDataFrameTrainSet, score)

    #Helper function to create CrossValidationEvent.
    def makeCrossValidation(self, estimator, crossValidationFolds, syncer):
        self.experimentRunId = syncer.experimentRun.id
        syncableDataFrame = syncer.convertDftoThrift(self.inputDataFrame)
        syncableEstimator = syncer.convertSpectoThrift(estimator,self.inputDataFrame)
        # TODO: Need to add meaningful label/feature/prediction column names
        return modeldb_types.CrossValidationEvent(syncableDataFrame, syncableEstimator, self.seed, self.evaluator,
                                            [""], [""], [""], crossValidationFolds, self.experimentRunId)

    #Returns a list of CrossValidationEvents, used for creating GridSearchCrossValidationEvent.
    def makeCrossValidationEvents(self, syncer):
        crossValidationEvents = []
        for estimator in self.crossValidations:
            crossValidationFolds = []
            for fold in self.crossValidations[estimator]:
                crossValidationFolds.append(self.makeCrossValidationFold(fold, syncer))
            crossValidationEvents.append(self.makeCrossValidation(estimator, crossValidationFolds, syncer))
        return crossValidationEvents

    #Creates a GridSearchCrossValidationEvent
    def makeGridSearchCVEvent(self, crossValidationEvents, syncer):
        self.experimentRunId = syncer.experimentRun.id
        fitEvent = FitEvent(self.bestModel, self.bestEstimator, self.inputDataFrame)
        gscve = modeldb_types.GridSearchCrossValidationEvent(self.numFolds, fitEvent.makeEvent(syncer), crossValidationEvents, self.experimentRunId)
        return gscve

    #Stores each of the associated events.
    def associate(self, res, syncer):

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

    def makeEvent(self, syncer):
        crossValidationEvents = self.makeCrossValidationEvents(syncer)
        gscve = self.makeGridSearchCVEvent(crossValidationEvents, syncer)
        return gscve

    def sync(self, syncer):
        gscve = self.makeEvent(syncer)
        thriftClient = syncer.client
        res = thriftClient.storeGridSearchCrossValidationEvent(gscve)
        self.associate(res, syncer)
