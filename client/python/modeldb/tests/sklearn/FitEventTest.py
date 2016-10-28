import unittest
from ModelDBSyncerTest import SyncerTest
import utils
from modeldb.thrift import ttypes as modeldb_types

from sklearn import linear_model
import pandas as pd

class TestFitEvent(unittest.TestCase):
    def setUpClass(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        model = linear_model.LinearRegression()
        X = pd.DataFrame(np.random.randint(0,100,size=(100, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(0,100,size=(100, 1)), columns=['output'])
        X.tag("digits-dataset")
        model.tag("linear reg")

        model.fitSync(X,y)
        events = SyncerTest.instance.sync()
        self.fitEvent = events[0]

    # Tests model values, associated with FitEvent
    def test_model(self):
        utils.validate_fit_event_struct(self.fitEvent, self)
        transformer = self.fitEvent.model
        expected_transformer = modeldb_types.Transformer(
            -1,
            'LinearRegression',
            [0.0],
            'linear reg')
        utils.is_equal_transformer(transformer, expected_transformer)
        
    # Tests TransformerSpec values.
    def test_transformer_spec(self):
        spec = self.fitEvent.spec
        expected_spec = modeldb_types.TransformerSpec(
            -1, 
            'LinearRegression',
            ['A', 'B', 'C', 'D', 'output'],
            'linear reg',
            4,
            None) # Fix hyperparams here. see below
        # params = ['copy_X', 'normalize', 'n_jobs', 'fit_intercept']
        # for i in range(4):
        #     hyperparam = spec.hyperparameters[i]
        #     self.assertEqual(hyperparam.name, params[i])

    def test_dataframe(self):
        df = self.fitEvent.df
        expected_df = modeldb_types.DataFrame(
            100,
            'digits-dataset',
            -1,
            None) # fix columns
        utils.is_equal_dataframe(df, expected_df)  
        # # Tests individual DataFrameColumns
        # self.assertEqual(len(df.schema), 5)
        # column_names = ['A', 'B', 'C', 'D', 'output']
        # for i in range(0,5):
        #     df_column = df.schema[i]
        #     self.assertEqual(df_column.name, column_names[i])
        #     self.assertEqual(df_column.type, 'int64')