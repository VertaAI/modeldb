"""
Sample workflow using scikit-learn linear_model.
"""
import unittest
import argparse
import numpy as np
from patsy import dmatrices
import statsmodels.api as sm

from sklearn import linear_model
from sklearn import preprocessing

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics
from modeldb.sklearn_native import SyncableRandomSplit



def load_pandas_dataset():
    """
    Helper function to import data.
    """
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
data, target = load_pandas_dataset()
data.tag("occupation dataset")

model.fit_sync(data, target)
model.predict_sync(data)
model.tag("Logistic Regression model")

#Test OneHotEncoder with transform method
model2 = preprocessing.OneHotEncoder()
model2.fit_sync(data)
model2.transform_sync(data)
model2.tag("One Hot encoding")

#Test Metric Class
precision_score = SyncableMetrics.compute_metrics(model, 'precision', data, 'children',
                                                 'religious', data['religious'])
recall_score = SyncableMetrics.compute_metrics(model, 'recall', data, 'children',
                                              'religious', data['religious'])

#Test Random-Split Event
SyncableRandomSplit.random_split(data, [1, 2, 3], 1234)

#Sync all the events to database
SyncerObj.instance.sync()


class TestLinearModelEndToEnd(unittest.TestCase):
    """
    Tests if workflow above is stored in database correctly.
    """
    def test_project(self):
        """
        Tests if project is stored correctly.
        """
        projectOverview = SyncerObj.client.getProjectOverviews()[0]
        project = projectOverview.project
        self.assertEquals(project.description, 'pandas-logistic-regression')
        self.assertEquals(project.author, 'srinidhi')
        self.assertEquals(project.name, 'test1')
        self.assertGreaterEqual(project.id, 0)
        self.assertGreaterEqual(projectOverview.numExperimentRuns, 0)
        self.assertGreaterEqual(projectOverview.numExperiments, 0)

    def test_models(self):
        """
        Tests if the two models are stored correctly.
        """
        model_responses = SyncerObj.client.getExperimentRunDetails(1).modelResponses
        project_overview = SyncerObj.client.getProjectOverviews()[0]
        project = project_overview.project
        # Two models are stored above - ensure both are in database
        self.assertEquals(len(model_responses), 2)

        model1 = model_responses[0]
        model2 = model_responses[1]

        self.assertEqual(model1.projectId, project.id)
        self.assertEqual(model2.projectId, project.id)
        self.assertEqual(model1.trainingDataFrame.numRows, data.shape[0])
        self.assertEqual(model2.trainingDataFrame.numRows, data.shape[0])

        transformer1 = model1.specification
        transformer2 = model2.specification

        self.assertEqual(transformer1.transformerType, 'LogisticRegression')
        self.assertEqual(transformer2.transformerType, 'OneHotEncoder')

        self.assertEqual(transformer1.tag, 'Logistic Regression model')
        self.assertEqual(transformer2.tag, 'One Hot encoding')

        # Check hyperparameters for both models
        hyperparams1 = transformer1.hyperparameters
        hyperparams2 = transformer2.hyperparameters
        self.assertEqual(len(hyperparams1), 14)
        self.assertEqual(len(hyperparams2), 5)

    def test_metrics(self):
        """
        Tests if metrics are stored correctly.
        """
        model_responses = SyncerObj.client.getExperimentRunDetails(1).modelResponses
        model1 = model_responses[0]
        model2 = model_responses[1]

        self.assertNotEqual(model1.trainingDataFrame.id, model2.trainingDataFrame.id)

        # Metrics are only stored for the first model.
        self.assertEquals(len(model1.metrics), 2)
        self.assertEquals(len(model2.metrics), 0)
        self.assertIn('recall', model1.metrics)
        self.assertIn('precision', model1.metrics)
        self.assertAlmostEqual(recall_score, model1.metrics['recall'][2])
        self.assertAlmostEqual(precision_score, model1.metrics['precision'][2])

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Pass in '
                                     ' -test flag if you wish to run unittests on this workflow')
    parser.add_argument('-test', action='store_true')
    args = parser.parse_args()
    if args.test:
        suite = unittest.TestLoader().loadTestsFromTestCase(TestLinearModelEndToEnd)
        unittest.TextTestRunner().run(suite)
