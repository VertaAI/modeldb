import unittest
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn.grid_search import GridSearchCV
from sklearn.svm import SVC
import pandas as pd


class TestGridSearchEvent(unittest.TestCase):

    def setUp(self):
        name = "grid search test"
        author = "srinidhi"
        description = "Grid search cross validation - 3 folds"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None))
        X = pd.DataFrame(np.random.randint(
            0, 100, size=(2000, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(
            0, 100, size=(2000, 1)), columns=['output'])

        # Add tag for dataframe
        syncer_obj.add_tag(X, "digits-dataset")
        syncer_obj.clear_buffer()

        tuned_parameters = [{'kernel': ['rbf'], 'gamma': [1e-3, 1e-4],
                             'C': [10, 100]}]
        clf = GridSearchCV(SVC(), tuned_parameters, cv=3)
        y = y.values.ravel()
        clf.fit_sync(X, y)
        events = syncer_obj.sync()
        self.grid_search_event = events[0]

    def test_gridcv_event(self):
        utils.validate_grid_search_cv_event(self.grid_search_event, self)
        self.assertEqual(self.grid_search_event.numFolds, 3)
        best_fit_event = self.grid_search_event.bestFit
        df = best_fit_event.df
        expected_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('A', 'int64'),
                modeldb_types.DataFrameColumn('B', 'int64'),
                modeldb_types.DataFrameColumn('C', 'int64'),
                modeldb_types.DataFrameColumn('D', 'int64'),
            ],
            2000,
            'digits-dataset')
        utils.is_equal_dataframe(df, expected_df, self)

        transformer = best_fit_event.model
        utils.validate_transformer_struct(transformer, self)
        expected_transformer = modeldb_types.Transformer(
            -1,
            'SVC',
            '')
        utils.is_equal_transformer(transformer, expected_transformer, self)
        self.assertEqual(best_fit_event.featureColumns, [
                              'A', 'B', 'C', 'D'])

    def test_cross_validation_event(self):
        cross_validations = self.grid_search_event.crossValidations
        # Number of cross validations is equal to number of combinations
        # of parameters, multiplied by the number of folds.
        self.assertEqual(len(cross_validations), 12)

        # Order of cross validations may vary each run, so we only validate
        # certain hyperparameter values.
        for cv in cross_validations:
            utils.validate_cross_validate_event(cv, self)
            self.assertEqual(cv.featureColumns, ['A', 'B', 'C', 'D'])
            self.assertEqual(cv.evaluator, 'multiclass')
            self.assertEqual(cv.seed, 0)

            hyperparameters = cv.spec.hyperparameters
            self.assertEqual(len(hyperparameters), 14)

            # We check these particular hyperparameters because we pass these
            # as our tuned parameters above.
            for hyperparam in hyperparameters:
                if hyperparam.name == 'gamma':
                    self.assertIn(hyperparam.value, ['0.001', '0.0001'])
                if hyperparam.name == 'C':
                    self.assertIn(hyperparam.value, ['10', '100'])
                if hyperparam.name == 'kernel':
                    self.assertEqual(hyperparam.value, 'rbf')

    def test_cross_validation_fold(self):
        cross_validations = self.grid_search_event.crossValidations
        for cv in cross_validations:
            folds = cv.folds
            for fold in folds:
                utils.validate_cross_validation_fold(fold, self)
                training_df = fold.trainingDf
                validation_df = fold.validationDf
                self.assertEqual(training_df.numRows +
                                  validation_df.numRows, 2000)


if __name__ == '__main__':
    unittest.main()
