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

class TestTransformEvent(BaseTestCases.BaseClass):
    @classmethod
    def setUpClass(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        letters = ['A', 'B', 'C', 'D']
        X = np.random.choice(letters, size=(100, 1)).ravel()
        model = preprocessing.LabelEncoder()
        model.tag("label encoder")
        model.fitSync(X)
        model.transformSync(X)
        events = SyncerTest.instance.sync()
        self.fitEvent = events[0]
        self.transformEvent = events[1]

    # Tests Transformer values.
    def test_transformer_construction(self):
        transformer = self.transformEvent.transformer
        self.assertEqual(transformer.id, -1)
        self.assertEqual(transformer.transformerType, 'LabelEncoder')
        self.assertEqual(transformer.weights, [0.0])
        self.assertEqual(transformer.tag, 'label encoder')

    # Tests values of old and new dataframes of TransformEvent object.
    def test_dataframe_transform_event(self):
        old_df = self.transformEvent.oldDataFrame
        new_df = self.transformEvent.newDataFrame
        for df in [old_df, new_df]:
            self.assertEqual(df.id, -1)
            self.assertEqual(df.numRows, 100)
            self.assertEqual(df.tag, '')
        
        # Tests individual DataFrameColumns
        new_df_column = new_df.schema[0]
        self.assertEqual(new_df_column.type, 'int64')
        self.assertEqual(new_df_column.name, '0')

    # Tests TransformerSpec values
    def test_transformer_spec_construction(self):
        spec = self.fitEvent.spec
        self.assertEqual(spec.id, -1)
        self.assertEqual(spec.transformerType, 'LabelEncoder')
        self.assertEqual(spec.features, [])
        self.assertEqual(spec.tag, 'label encoder')
        self.assertEqual(len(spec.hyperparameters), 0)

    # Tests DataFrame values, associated with FitEvent
    def test_dataframe_fit_event(self):
        df = self.fitEvent.df
        self.assertEqual(df.numRows, 100)
        self.assertEqual(df.tag, '')
        self.assertEqual(len(df.schema), 0)

    # Tests model values, associated with FitEvent
    def test_model_fit_event(self):
        transformer = self.fitEvent.model
        self.assertEqual(transformer.tag, 'label encoder')
        self.assertEqual(transformer.weights, [0.0])
        self.assertEqual(transformer.transformerType, 'LabelEncoder')
        self.assertEqual(transformer.id, -1)
    
unittest.main()
