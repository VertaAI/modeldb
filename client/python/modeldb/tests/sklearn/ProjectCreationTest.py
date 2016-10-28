import unittest
from ModelDbSyncerTest import SyncerTest
from modeldb.sklearn_native.ModelDbSyncer import *
import modeldb.tests.utils
from modeldb.thrift.modeldb import ttypes as modeldb_types

from sklearn import linear_model
import pandas as pd

class TestProjectEvent(unittest.TestCase):
    @classmethod
    def setUpClass(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        events = SyncerTest.instance.sync()

    # Tests model values, associated with FitEvent
    def test_project_creation(self):
        print("SELF", SyncerObj.instance.project)
        #utils.validate_project_struct(self)
        '''
        utils.validate_fit_event_struct(self.fitEvent, self)
        transformer = self.fitEvent.model
        expected_transformer = modeldb_types.Transformer(
            -1,
            'LinearRegression',
            [0.0],
            'linear reg')
        utils.is_equal_transformer(transformer, expected_transformer)
        '''
    

if __name__ == '__main__':
    unittest.main()