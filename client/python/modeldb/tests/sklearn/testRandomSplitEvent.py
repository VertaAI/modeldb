import unittest
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *

import pandas as pd


class TestRandomSplitEvent(unittest.TestCase):

    def setUp(self):
        name = "random split test"
        author = "srinidhi"
        description = "70/30 split"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None))
        X = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 1)), columns=['output'])

        # Add tag for dataframe
        syncer_obj.add_tag(X, "digits-dataset")

        seed = 1
        weights = [0.7, 0.3]
        syncer_obj.clear_buffer()
        x_train, x_test, y_train, y_test = (
            cross_validation.train_test_split_sync(
                X, y, train_size=0.7))
        events = syncer_obj.sync()
        self.random_split_event = events[0]

    def test_random_split_event(self):
        utils.validate_random_split_event_struct(self.random_split_event, self)
        weights = self.random_split_event.weights
        self.assertAlmostEqual(weights[0], 0.7)
        self.assertAlmostEqual(weights[1], 0.3)
        self.assertEqual(self.random_split_event.seed, 1)

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
        self.assertEqual(len(split_data_frames), 2)
        dataframe1 = split_data_frames[0]
        dataframe2 = split_data_frames[1]
        utils.validate_dataframe_struct(dataframe1, self)
        utils.validate_dataframe_struct(dataframe2, self)

        # Check if dataframes are split according to weights (within some
        # margin of error)
        self.assertIn(dataframe1.numRows, range(65, 75))
        self.assertIn(dataframe2.numRows, range(25, 35))
        self.assertEqual(dataframe1.numRows + dataframe2.numRows, 100)


if __name__ == '__main__':
    unittest.main()
