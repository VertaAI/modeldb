"""
Event indicating that the user performed a grid search and used
cross-validation to train an estimator.
"""
from modeldb.events.Event import *
from modeldb.events import FitEvent

class GridSearchCVEvent(Event):
    """
    Class for creating and storing GridSearchEvents
    """
    def __init__(self, inputDataFrame, crossValidations, seed, evaluator,
                 bestModel, bestEstimator, numFolds):
        self.input_dataframe = inputDataFrame
        self.cross_validations = crossValidations
        self.seed = seed
        self.evaluator = evaluator
        self.best_model = bestModel
        self.best_estimator = bestEstimator
        self.num_folds = numFolds

    def make_cross_validation_fold(self, fold, syncer):
        """
        Helper function to create a CrossValidationFold object, later
        used in making CrossValidationEvents.
        """
        [(transformer, validation_set, training_set, score)] = fold
        syncable_dataframe_valid_set = syncer.convertDftoThrift(validation_set)
        syncable_dataframe_train_set = syncer.convertDftoThrift(training_set)
        syncable_transformer = syncer.convertModeltoThrift(transformer)
        return modeldb_types.CrossValidationFold(syncable_transformer, syncable_dataframe_valid_set,
                                                 syncable_dataframe_train_set, score)

    def make_cross_validation(self, estimator, cross_validation_folds, syncer):
        """
        Helper function to create CrossValidationEvent.
        """
        syncable_dataframe = syncer.convertDftoThrift(self.input_dataframe)
        syncable_estimator = syncer.convertSpectoThrift(estimator)
        columns = syncer.setColumns(self.input_dataframe)
        # TODO: Need to add meaningful label/prediction column names
        return modeldb_types.CrossValidationEvent(syncable_dataframe, syncable_estimator,
                                                  self.seed, self.evaluator, [""], [""],
                                                  columns, cross_validation_folds,
                                                  syncer.experimentRun.id)

    def make_cross_validation_events(self, syncer):
        """
        Returns a list of cross_validation_events, used for creating GridSearchCrossValidationEvent.
        """
        cross_validation_events = []
        for estimator in self.cross_validations:
            cross_validation_folds = []
            for fold in self.cross_validations[estimator]:
                cross_validation_folds.append(self.make_cross_validation_fold(fold, syncer))
            cross_validation_events.append(
                self.make_cross_validation(estimator,
                                           cross_validation_folds, syncer))
        return cross_validation_events

    def make_gridsearch_cv_event(self, cross_validation_events, syncer):
        """
        Helper method to create a GridSearchCrossValidationEvent
        """
        fit_event = FitEvent(self.best_model, self.best_estimator, self.input_dataframe)
        gscve = modeldb_types.GridSearchCrossValidationEvent(self.num_folds,
                                                             fit_event.makeEvent(syncer),
                                                             cross_validation_events,
                                                             syncer.experimentRun.id)
        return gscve

    def associate(self, res, syncer):
        """
        Stores the server response ids for each of the events into dictionary.
        """
        #First store the fit event
        df_id = id(self.input_dataframe)
        syncer.storeObject(self, res.eventId)
        syncer.storeObject(self.best_estimator, res.fitEventResponse.specId)
        syncer.storeObject(df_id, res.fitEventResponse.dfId)
        syncer.storeObject(self.best_model, res.fitEventResponse.modelId)

        #Store each cross validation from the grid
        for cv, cver in zip(self.cross_validations.items(), res.crossValidationEventResponses):
            estimator, folds = cv
            syncer.storeObject(cver.specId, estimator)

            #Iterate through each fold
            for pair in zip(folds, cver.foldResponses):
                folds, foldr = pair
                fold = folds[0]
                df_id_valid = id(fold[1])
                df_id_train = id(fold[2])

                syncer.storeObject(fold[0], foldr.modelId)
                syncer.storeObject(df_id_valid, foldr.validationId)
                syncer.storeObject(df_id_train, foldr.trainingId)

    def makeEvent(self, syncer):
        """
        Constructs a thrift GridSearchCrossValidation event
        object with appropriate fields.
        """
        cross_validation_events = self.make_cross_validation_events(syncer)
        gscve = self.make_gridsearch_cv_event(cross_validation_events, syncer)
        return gscve

    def sync(self, syncer):
        """
        Stores GridSearchCrossValidation event on the server.
        """
        gscve = self.makeEvent(syncer)
        thrift_client = syncer.client
        res = thrift_client.storeGridSearchCrossValidationEvent(gscve)
        self.associate(res, syncer)
