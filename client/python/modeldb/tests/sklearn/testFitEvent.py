import unittest
import sys
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn import linear_model
import pandas as pd

FMIN = sys.float_info.min
FMAX = sys.float_info.max


class TestFitEvent(unittest.TestCase):

    def setUp(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None))
        model = linear_model.LinearRegression()
        np.random.seed(0)
        X = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 1)), columns=['output'])

        # Add tags for models / dataframes
        syncer_obj.add_tag(X, "digits-dataset")
        syncer_obj.add_tag(model, "linear reg")

        syncer_obj.clear_buffer()
        model.fit_sync(X, y)
        events = syncer_obj.sync()
        self.fit_event = events[0]

    # Tests model values, associated with FitEvent
    def test_model(self):
        utils.validate_fit_event_struct(self.fit_event, self)
        transformer = self.fit_event.model
        self.assertEqual(self.fit_event.featureColumns,
                              ['A', 'B', 'C', 'D'])
        expected_transformer = modeldb_types.Transformer(
            -1,
            'LinearRegression',
            'linear reg')
        utils.is_equal_transformer(transformer, expected_transformer, self)

    # Tests TransformerSpec values.
    def test_transformer_spec(self):
        spec = self.fit_event.spec
        expected_spec = modeldb_types.TransformerSpec(
            -1,
            'LinearRegression',
            [
                modeldb_types.HyperParameter(
                    'copy_X', 'True', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'normalize', 'False', 'bool', FMIN, FMAX),
                modeldb_types.HyperParameter('n_jobs', '1', 'int', FMIN, FMAX),
                modeldb_types.HyperParameter(
                    'fit_intercept', 'True', 'bool', FMIN, FMAX)
            ],
            'linear reg')
        utils.is_equal_transformer_spec(spec, expected_spec, self)

    def test_dataframe(self):
        df = self.fit_event.df
        expected_df = modeldb_types.DataFrame(
            -1,
            [
                modeldb_types.DataFrameColumn('A', 'int64'),
                modeldb_types.DataFrameColumn('B', 'int64'),
                modeldb_types.DataFrameColumn('C', 'int64'),
                modeldb_types.DataFrameColumn('D', 'int64')
            ],
            100,
            'digits-dataset')
        utils.is_equal_dataframe(df, expected_df, self)


if __name__ == '__main__':
    unittest.main()
