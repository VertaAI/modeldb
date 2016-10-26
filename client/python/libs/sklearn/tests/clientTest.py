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

from sklearn.datasets import samples_generator
import unittest

name = "logistic-test"
author = "srinidhi"
description = "income-level logistic regression"
SyncerObj = SyncerTest(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

#Helper function to import data
def loadPandasDataset():
    # load dataset
    dta = sm.datasets.fair.load_pandas().data

    # add "affair" column: 1 represents having affairs, 0 represents not
    dta['affair'] = (dta.affairs > 0).astype(int)


    # create dataframes with an intercept column and dummy variables for
    # occupation and occupation_husb
    y, X = dmatrices('affair ~ rate_marriage + age + yrs_married + children + \
                  religious + educ + C(occupation) + C(occupation_husb)',
                  dta, return_type="dataframe")
    # flatten y into a 1-D array
    y = np.ravel(y)
    return X, y

model = linear_model.LogisticRegression()
X,y = loadPandasDataset()

class TestFitEvent(unittest.TestCase):
    def setUp(self):
        model.fitSync(X,y)
        self.events = SyncerTest.instance.sync()
        self.fitEvent = self.events[0]
    def tearDown(self):
        SyncerTest.instance.clearBuffer()
    def test_fitevent(self):
        self.assertTrue(hasattr(self.fitEvent, 'df'))
        self.assertTrue(hasattr(self.fitEvent, 'spec'))
        self.assertTrue(hasattr(self.fitEvent, 'model'))
        self.assertTrue(hasattr(self.fitEvent, 'featureColumns'))
        self.assertTrue(hasattr(self.fitEvent, 'predictionColumns'))
        self.assertTrue(hasattr(self.fitEvent, 'labelColumns'))
        self.assertTrue(hasattr(self.fitEvent, 'experimentRunId'))
    def test_dataframe(self):
        df = self.fitEvent.df
        self.assertTrue(hasattr(df, 'id'))
        self.assertTrue(hasattr(df, 'schema'))
        self.assertTrue(hasattr(df, 'numRows'))
        self.assertTrue(hasattr(df, 'tag')) 

unittest.main()
