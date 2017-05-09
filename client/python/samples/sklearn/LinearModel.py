"""
Sample workflow using scikit-learn linear_model.
"""
import os
import unittest
import argparse
import numpy as np
import statsmodels.api as sm

from sklearn import linear_model
from sklearn import preprocessing
from sklearn.metrics import mean_squared_error

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

ROOT_DIR = '../../../../server/'


def load_pandas_dataset():
    """
    Helper function to import data.
    """
    # load dataset
    df = sm.datasets.fair.load_pandas().data
    target = pd.DataFrame(df['affairs'])
    df = df.drop('affairs', 1)
    target = np.ravel(target)
    return df, target


def run_linear_model_workflow():
    """
    Sample workflow using OneHotEncoder and LinearRegression.
    """
    syncer_obj = Syncer.create_syncer("test1", "test_user",
                                      "pandas-linear-regression")

    data, target = load_pandas_dataset()
    syncer_obj.add_tag(data, "occupation dataset")

    # Hot encode occupation column of data
    hot_enc = preprocessing.OneHotEncoder()
    syncer_obj.add_tag(hot_enc, "Hot encoding occupation column")

    hot_enc.fit_sync(data['occupation'].reshape(-1, 1))
    hot_enc_rows = hot_enc.transform_sync(data['occupation'].reshape(-1, 1))
    hot_enc_df = pd.DataFrame(hot_enc_rows.toarray())

    # Drop column as it is now encoded
    dropped_data = data.drop_sync('occupation', axis=1)
    # Join the hot encoded rows with the rest of the data
    data = dropped_data.join(hot_enc_df)

    x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
        data, target, test_size=0.3, random_state=1)

    syncer_obj.add_tag(x_train, "training data - 70%")
    syncer_obj.add_tag(x_test, "testing data - 30%")

    model = linear_model.LinearRegression()
    syncer_obj.add_tag(model, "Basic linear reg")

    model.fit_sync(x_train, y_train)
    y_pred = model.predict_sync(x_test)

    mean_error = SyncableMetrics.compute_metrics(
        model, mean_squared_error, y_test, y_pred, x_test, "", 'affairs')

    # Sync all the events to database
    syncer_obj.sync()

    # Certain variables are returned so they can be used for unittests below.
    return syncer_obj, x_test, mean_error, dropped_data


class TestLinearModelEndToEnd(unittest.TestCase):
    """
    Tests if workflow above is stored in database correctly.
    """
    @classmethod
    def setUpClass(self):
        """
        This executes at the beginning of unittest.
        Database is cleared before testing.
        """
        os.system("cat " + ROOT_DIR + "codegen/sqlite/clearDb.sql "
                  "| sqlite3 " + ROOT_DIR + "modeldb_test.db")
        self.syncer_obj, self.x_test, self.mean_error, self.dropped_data = run_linear_model_workflow()

    def test_project(self):
        """
        Tests if project is stored correctly.
        """
        projectOverview = self.syncer_obj.client.getProjectOverviews()[0]
        project = projectOverview.project
        self.assertEqual(project.description, 'pandas-linear-regression')
        self.assertEqual(project.author, 'srinidhi')
        self.assertEqual(project.name, 'test1')
        self.assertGreaterEqual(project.id, 0)
        self.assertGreaterEqual(projectOverview.numExperimentRuns, 0)
        self.assertGreaterEqual(projectOverview.numExperiments, 0)

    def test_models(self):
        """
        Tests if the two models are stored correctly.
        """

        projectOverview = self.syncer_obj.client.getProjectOverviews()[0]
        project = projectOverview.project
        runs_and_exps = self.syncer_obj.client.getRunsAndExperimentsInProject(
            project.id)
        # Get the latest experiment run id
        exp_id = runs_and_exps.experimentRuns[-1].id
        model_responses = self.syncer_obj.client.getExperimentRunDetails(
            exp_id).modelResponses

        # Two models are stored above - ensure both are in database
        self.assertEqual(len(model_responses), 2)

        model1 = model_responses[0]
        model2 = model_responses[1]

        self.assertEqual(model1.projectId, project.id)
        self.assertEqual(model2.projectId, project.id)

        transformer1 = model1.specification
        transformer2 = model2.specification

        self.assertEqual(transformer1.transformerType, 'OneHotEncoder')
        self.assertEqual(transformer2.transformerType, 'LinearRegression')

        self.assertEqual(transformer1.tag, 'Hot encoding occupation column')
        self.assertEqual(transformer2.tag, 'Basic linear reg')

        # Check hyperparameters for both models
        hyperparams1 = transformer1.hyperparameters
        hyperparams2 = transformer2.hyperparameters
        self.assertEqual(len(hyperparams1), 5)
        self.assertEqual(len(hyperparams2), 4)

    def test_metrics(self):
        """
        Tests if metrics are stored correctly.
        """
        projectOverview = self.syncer_obj.client.getProjectOverviews()[0]
        project = projectOverview.project
        runs_and_exps = self.syncer_obj.client.getRunsAndExperimentsInProject(
            project.id)

        # Get the latest experiment run id
        exp_id = runs_and_exps.experimentRuns[-1].id
        model_responses = self.syncer_obj.client.getExperimentRunDetails(
            exp_id).modelResponses
        model1 = model_responses[0]
        model2 = model_responses[1]

        # Metrics are only stored for the second model.
        self.assertEqual(len(model1.metrics), 0)
        self.assertEqual(len(model2.metrics), 1)
        self.assertIn('mean_squared_error', model2.metrics)

        dataframe_id = self.syncer_obj.get_modeldb_id_for_object(self.x_test)
        self.assertAlmostEqual(
            self.mean_error,
            model2.metrics['mean_squared_error'][dataframe_id], places=4)

    def test_dataframe_ancestry(self):
        """
        Tests if dataframe ancestry is stored correctly for dropped column of
        dataset.
        """
        # Check ancestry for dropped dataframe
        # Confirm dropped column has the original dataframe in ancestry
        dataframe_id = self.syncer_obj.get_modeldb_id_for_object(
            self.dropped_data)
        ancestry = self.syncer_obj.client.getDataFrameAncestry(
            dataframe_id).ancestors
        self.assertEqual(len(ancestry), 2)

        df_1 = ancestry[0]
        df_2 = ancestry[1]
        df1_schema = df_1.schema
        df2_schema = df_2.schema
        self.assertEqual(len(df1_schema), 7)
        self.assertEqual(len(df2_schema), 8)
        # Ancestor is the original dataframe
        self.assertEqual(df_2.tag, 'occupation dataset')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Pass in -test flag if you wish'
        ' to run unittests on this workflow')
    parser.add_argument('-test', action='store_true')
    args = parser.parse_args()
    if args.test:
        suite = unittest.TestLoader().loadTestsFromTestCase(
            TestLinearModelEndToEnd)
        unittest.TextTestRunner().run(suite)
    else:
        run_linear_model_workflow()
