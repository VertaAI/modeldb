import numpy as np

from patsy import dmatrices
import statsmodels.api as sm

import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model
import client.SyncableRandomSplit as SyncableRandomSplit
import client.SyncableMetrics as SyncableMetrics
from client.ModelDbSyncer import *
from client.ModelDbSyncerTest import *

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
#model.fitSync(X,y)

class TestEvents(unittest.TestCase):
    def setUp(self):
        X, y = samples_generator.make_classification(
        n_informative=5, n_redundant=0, random_state=42)
    def tearDown(self):
        print("DONE!!")
    def test_upper(self):
        self.assertEqual('foo'.upper(), 'FOO')

    def test_isupper(self):
        self.assertTrue('FOO'.isupper())
        self.assertFalse('Foo'.isupper())

    def test_split(self):
        s = 'hello world'
        self.assertEqual(s.split(), ['hello', 'world'])
        # check that s.split fails when the separator is not a string
        with self.assertRaises(TypeError):
            s.split(2)

unittest.main()