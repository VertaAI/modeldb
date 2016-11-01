import unittest
import numpy as np
from patsy import dmatrices
import statsmodels.api as sm

from sklearn import linear_model
from sklearn import preprocessing

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics
from modeldb.sklearn_native import SyncableRandomSplit

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
SyncerObj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

#Create a sample logistic regression model, and test fit/predict
model = linear_model.LogisticRegression()
X,y = loadPandasDataset()
X.tag("occupation dataset")

model.fitSync(X,y)
model.predictSync(X)
model.tag("Logistic Regression model")

#Test OneHotEncoder with transform method
model2 = preprocessing.OneHotEncoder()
model2.fitSync(X)
model2.transformSync(X)

#Test Metric Class
SyncableMetrics.computeMetrics(model, 'precision', X, 'children', 'religious', X['religious'])
SyncableMetrics.computeMetrics(model, 'recall', X, 'children', 'religious', X['religious'])

#Test Random-Split Event
SyncableRandomSplit.randomSplit(X, [1,2,3], 1234)

#Sync all the events to database
SyncerObj.instance.sync()


class TestLinearModelEndToEnd(unittest.TestCase):
    # Tests if workflow above is stored in database correctly
    def test_project(self):
        projectOverview = SyncerObj.client.getProjectOverviews()[0]
        project = projectOverview.project
        self.assertEquals(project.description, 'pandas-logistic-regression')
        self.assertEquals(project.author, 'srinidhi')
        self.assertEquals(project.name, 'test1')
        self.assertNotEqual(project.id, -1)
        self.assertNotEqual(projectOverview.numExperimentRuns, -1)
        self.assertNotEqual(projectOverview.numExperiments, -1)

    def test_models(self):
        model_responses = SyncerObj.client.getExperimentRunDetails(1).modelResponses
        projectOverview = SyncerObj.client.getProjectOverviews()[0]
        project = projectOverview.project
        # Two models are stored above - ensure both are in database
        self.assertEquals(len(model_responses), 2)

        model1 = model_responses[0]
        model2 = model_responses[1]

        self.assertEqual(model1.projectId, project.id)
        self.assertEqual(model2.projectId, project.id)
        self.assertEqual(model1.trainingDataFrame.numRows, X.shape[0])
        self.assertEqual(model2.trainingDataFrame.numRows, X.shape[0])

        transformer1 = model1.specification
        transformer2 = model2.specification
        self.assertEqual(transformer1.transformerType, 'LogisticRegression')
        self.assertEqual(transformer2.transformerType, 'OneHotEncoder')

    def test_metrics(self):
        model_responses = SyncerObj.client.getExperimentRunDetails(1).modelResponses
        model1 = model_responses[0]
        model2 = model_responses[1]
        # Metrics are only stored for the first model.
        self.assertEquals(len(model1.metrics), 2)
        self.assertEquals(len(model2.metrics), 0)
        self.assertIn('recall', model1.metrics)
        self.assertIn('precision', model1.metrics)


if __name__ == '__main__':
    unittest.main()
