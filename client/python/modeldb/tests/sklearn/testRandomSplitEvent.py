import unittest
import sys
from sklearn.cross_validation import train_test_split
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableRandomSplit

import pandas as pd
import random

class TestRandomSplitEvent(unittest.TestCase):
    def setUp(self):
        name = "random split test"
        author = "srinidhi"
        description = "70/30 split"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        X = pd.DataFrame(np.random.randint(0,100,size=(100, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(0,100,size=(100, 1)), columns=['output'])
        X.tag("digits-dataset")
        seed = 1       
        weights = [0.7, 0.3]
        SyncerTest.instance.clear_buffer()
        x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(X, y, train_size=0.7)
        events = SyncerTest.instance.sync()
        self.random_split_event = events[0]

    def test_random_split_event(self):
        utils.validate_random_split_event_struct(self.random_split_event, self)
        weights = self.random_split_event.weights
        self.assertAlmostEqual(weights[0], 0.7)
        self.assertAlmostEqual(weights[1], 0.3)
        self.assertEquals(self.random_split_event.seed, 1)

    def test_old_dataframe(self):
        old_df = self.random_split_event.oldDataFrame
        expected_df = modeldb_types.DataFrame(
            -1, 
            [
                modeldb_types.DataFrameColumn('A', 'int64'), 
                modeldb_types.DataFrameColumn('B', 'int64'), 
                modeldb_types.DataFrameColumn('C', 'int64'), 
                modeldb_types.DataFrameColumn('D', 'int64'),
            ],
            100,
            'digits-dataset')
        utils.is_equal_dataframe(old_df, expected_df, self)
        
    def test_split_dataframes(self):
        split_data_frames = self.random_split_event.splitDataFrames
        self.assertEquals(len(split_data_frames), 2)
        dataframe1 = split_data_frames[0]
        dataframe2 = split_data_frames[1]
        utils.validate_dataframe_struct(dataframe1, self)
        utils.validate_dataframe_struct(dataframe2, self)

        # Check if dataframes are split according to weights (within some margin of error)
        self.assertIn(dataframe1.numRows, range(65,75))
        self.assertIn(dataframe2.numRows, range(25,35))
        self.assertEquals(dataframe1.numRows + dataframe2.numRows, 100)

if __name__ == '__main__':
    unittest.main()
