import numpy as np

from patsy import dmatrices
import statsmodels.api as sm

import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model

from ModelDbSyncer import *
import SyncableRandomSplit 
import SyncableMetrics 
from ModelDbSyncerTest import *

import unittest
from BaseClass import BaseTestCases

class TestFitEvent(BaseTestCases.BaseClass):
    @classmethod
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
        transformer = self.fitEvent.model
        self.assertEqual(transformer.id, -1)
        self.assertEqual(transformer.transformerType, 'LinearRegression')
        self.assertEqual(transformer.weights, [0.0])
        self.assertEqual(transformer.tag, 'linear reg')
        
    # Tests TransformerSpec values.
    def test_transformer_spec(self):
        spec = self.fitEvent.spec
        self.assertEqual(spec.id, -1)
        self.assertEqual(spec.transformerType, 'LinearRegression')
        self.assertEqual(spec.features, ['A', 'B', 'C', 'D', 'output'])
        self.assertEqual(spec.tag, 'linear reg')
        # Tests individual hyperparameters
        self.assertEqual(len(spec.hyperparameters), 4)
        params = ['copy_X', 'normalize', 'n_jobs', 'fit_intercept']
        for i in range(4):
            hyperparam = spec.hyperparameters[i]
            self.assertEqual(hyperparam.name, params[i])

    # Tests DataFrame values
    def test_dataframe_construction(self):
        df = self.fitEvent.df
        self.assertEqual(df.numRows, 100)
        self.assertEqual(df.tag, 'digits-dataset')
        self.assertEqual(df.id, -1)
        # Tests individual DataFrameColumns
        self.assertEqual(len(df.schema), 5)
        column_names = ['A', 'B', 'C', 'D', 'output']
        for i in range(0,5):
            df_column = df.schema[i]
            self.assertEqual(df_column.name, column_names[i])
            self.assertEqual(df_column.type, 'int64')

unittest.main()
