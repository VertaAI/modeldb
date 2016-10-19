import numpy as np
from patsy import dmatrices
import statsmodels.api as sm

from sklearn import linear_model
from sklearn import preprocessing

import client.ModelDbSyncer as ModelDbSyncer
import client.SyncableMetrics as SyncableMetrics
import client.SyncableRandomSplit as SyncableRandomSplit

#Sample sequence of operations

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

# Creating a new project
name = "test1"
author = "srinidhi"
description = "pandas-logistic-regression"
SyncerObj = ModelDbSyncer.Syncer([name, author, description])
SyncerObj.startExperiment("First experiment - Logistic Regression")
#Create a sample logistic regression model, and test fit/predict
model = linear_model.LogisticRegression()
X,y = loadPandasDataset()
X.tag("occupation dataset")

model.fitSync(X,y)
model.predictSync(X)
model.tag("Logistic Regression model")
SyncerObj.endExperiment()

SyncerObj.startExperiment("Second experiment - preprocessing")

#Test OneHotEncoder with transform method
model2 = preprocessing.OneHotEncoder()
model2.fitSync(X)
model2.transformSync(X)

#Test Metric Class
SyncableMetrics.computeMetrics(model, 'precision', X, 'children', 'religious', X['religious'])
SyncableMetrics.computeMetrics(model, 'recall', X, 'children', 'religious', X['religious'])

#Test Random-Split Event
SyncableRandomSplit.randomSplit(X, [1,2,3], 1234)
SyncerObj.endExperiment()

#Sync all the events to database
ModelDbSyncer.Syncer.instance.sync()