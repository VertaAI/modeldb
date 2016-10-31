import unittest
import sys
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn.grid_search import GridSearchCV
from sklearn.svm import SVC
import pandas as pd

FMIN = sys.float_info.min
FMAX = sys.float_info.max

class TestGridSearchEvent(unittest.TestCase):
    def setUp(self):
        name = "grid search test"
        author = "srinidhi"
        description = "Grid search cross validation - 3 folds"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        X = pd.DataFrame(np.random.randint(0,100,size=(2000, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(0,100,size=(2000, 1)), columns=['output'])
        X.tag("digits-dataset")
        SyncerTest.instance.clearBuffer()

        tuned_parameters = [{'kernel': ['rbf'], 'gamma': [1e-3, 1e-4],
                     'C': [10, 100]}]
        clf = GridSearchCV(SVC(), tuned_parameters, cv=3)
        y = y.values.ravel()
        clf.fitSync(X, y)
        events = SyncerTest.instance.sync()
        self.gridSearchEvent = events[0]


    def test_gridcv_event(self):
        utils.validate_grid_search_cv_event(self.gridSearchEvent, self)
        self.assertEquals(self.gridSearchEvent.numFolds, 3)
        best_fit_event = self.gridSearchEvent.bestFit
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
            [0.0],
            'SVC',
            '')
        utils.is_equal_transformer(transformer, expected_transformer, self)

        
    def test_cross_validation_event(self):
        cross_validations = self.gridSearchEvent.crossValidations
        # Number of cross validations is equal to number of combinations of parameters, 
        # multiplied by the number of folds.
        self.assertEquals(len(cross_validations), 12)

        # Order of cross validations may vary each run, so we only validate 
        # certain hyperparameter values.
        for cv in cross_validations:
            utils.validate_cross_validate_event(cv, self)
            self.assertEquals(cv.evaluator, 'multiclass')
            self.assertEquals(cv.seed, 0)

            hyperparameters = cv.spec.hyperparameters
            features = cv.spec.features
            self.assertEquals(len(hyperparameters), 14)
            self.assertEquals(features, ['A', 'B', 'C', 'D'])

            # We check these particular hyperparameters because we pass these as 
            # our tuned parameters above.
            for hyperparam in hyperparameters:
                if hyperparam.name == 'gamma':
                    self.assertIn(hyperparam.value, ['0.001', '0.0001'])
                if hyperparam.name == 'C':
                    self.assertIn(hyperparam.value, ['10', '100'])
                if hyperparam.name == 'kernel':
                    self.assertEquals(hyperparam.value, 'rbf')
            
    def test_cross_validation_fold(self):
        cross_validations = self.gridSearchEvent.crossValidations
        for cv in cross_validations:
            folds = cv.folds
            for fold in folds:
                utils.validate_cross_validation_fold(fold, self)
                training_Df = fold.trainingDf
                validation_Df = fold.validationDf
                self.assertEquals(training_Df.numRows + validation_Df.numRows, 2000)

    
if __name__ == '__main__':
    unittest.main()
