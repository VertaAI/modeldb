"""
Source: https://www.kaggle.com/cbourguignat/otto-group-product-classification-challenge/why-calibration-works
"""
import os
import unittest
import argparse
import pandas as pd
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier, BaggingClassifier
from sklearn.metrics import log_loss
from sklearn.calibration import CalibratedClassifierCV

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics


# During the Otto Group competition, some Kagglers discussed in the forum about Calibration for Random Forests.
# It was a brand new functionality of the last scikit-learn version (0.16) :
# see : http://scikit-learn.org/stable/whats_new.html
# Calibration makes that the output of the models gives a true probability of a sample to belong to
# a particular class
# For instance, a well calibrated (binary) classifier should classify the samples such that among
# the samples
# to which it gave a predict_proba value close to 0.8, approximately 80% actually belong to the positive class
# See http://scikit-learn.org/stable/modules/calibration.html for more details
# This script is an example of how to implement calibration, and check if
# it boosts performance.

ROOT_DIR = '../../../../server/'
DATA_PATH = '../../../../data/'


def run_otto_workflow():
    name = "test1"
    author = "author"
    description = "kaggle-otto-script"
    # Creating a new project
    syncer_obj = Syncer(
        NewOrExistingProject(name, author, description),
        NewOrExistingExperiment("expName", "expDesc"),
        NewExperimentRun("otto test"))

    # Import Data
    # Note: This dataset is not included in the repo because of Kaggle
    # restrictions.
    # It can be downloaded from
    # https://www.kaggle.com/c/otto-group-product-classification-challenge/data
    X = pd.read_csv_sync(DATA_PATH + 'otto-train.csv')
    syncer_obj.add_tag(X, "original otto csv data")
    X = X.drop_sync('id', axis=1)

    syncer_obj.add_tag(X, "dropped id column")
    # Extract target
    # Encode it to make it manageable by ML algo
    y = X.target.values

    y = LabelEncoder().fit_transform_sync(y)

    # Remove target from train, else it's too easy ...
    X = X.drop_sync('target', axis=1)

    syncer_obj.add_tag(X, "data with dropped id and target columns")

    # Split Train / Test
    x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
        X, y, test_size=0.20, random_state=36)

    syncer_obj.add_tag(x_test, "testing data")
    syncer_obj.add_tag(x_train, "training data")
    # First, we will train and apply a Random Forest WITHOUT calibration
    # we use a BaggingClassifier to make 5 predictions, and average
    # because that's what CalibratedClassifierCV do behind the scene,
    # and we want to compare things fairly, i.e. be sure that averaging several
    # models
    # is not what explains a performance difference between no calibration,
    # and calibration.

    clf = RandomForestClassifier(n_estimators=50, n_jobs=-1)

    clfbag = BaggingClassifier(clf, n_estimators=5)
    clfbag.fit_sync(x_train, y_train)

    y_preds = clfbag.predict_proba_sync(x_test)

    SyncableMetrics.compute_metrics(
        clfbag, log_loss, y_test, y_preds, x_test, "", "", eps=1e-15,
        normalize=True)
    # print("loss WITHOUT calibration : ", log_loss(
    #     ytest, ypreds, eps=1e-15, normalize=True))

    # Now, we train and apply a Random Forest WITH calibration
    # In our case, 'isotonic' worked better than default 'sigmoid'
    # This is not always the case. Depending of the case, you have to test the
    # two possibilities

    clf = RandomForestClassifier(n_estimators=50, n_jobs=-1)
    calibrated_clf = CalibratedClassifierCV(clf, method='isotonic', cv=5)
    calibrated_clf.fit_sync(x_train, y_train)
    y_preds = calibrated_clf.predict_proba_sync(x_test)
    SyncableMetrics.compute_metrics(
        calibrated_clf, log_loss, y_test, y_preds, x_test, "", "", eps=1e-15,
        normalize=True)

    # print("loss WITH calibration : ", log_loss(
    #     ytest, ypreds, eps=1e-15, normalize=True))

    print(" ")
    print("Conclusion : in our case, calibration improved"
          "performance a lot ! (reduced loss)")
    syncer_obj.sync()
    return syncer_obj, x_train, x_test
    # We can see that we highly improved performance with
    # calibration (loss is reduced) !
    # Using calibration helped our team a lot to climb the leaderboard.
    # In the future competitions, that's for sure, I will not forget to test
    # this trick !


class TestOttoCalibration(unittest.TestCase):
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
        self.syncer_obj, self.x_train, self.x_test = run_otto_workflow()

    def test_project(self):
        """
        Tests if project is stored correctly.
        """
        projectOverview = self.syncer_obj.client.getProjectOverviews()[0]
        project = projectOverview.project
        self.assertEqual(project.description, 'kaggle-otto-script')
        self.assertEqual(project.author, 'author')
        self.assertEqual(project.name, 'test1')
        self.assertGreaterEqual(project.id, 0)
        self.assertGreaterEqual(projectOverview.numExperimentRuns, 0)
        self.assertGreaterEqual(projectOverview.numExperiments, 0)

    def test_dataframe_ancestry(self):
        """
        Tests if dataframe ancestry is stored correctly for training dataset.
        """
        # Check ancestry for the Xtrain dataframe (data the model is fit on)
        dataframe_id = self.syncer_obj.id_for_object[id(self.x_train)]
        ancestry = self.syncer_obj.client.getDataFrameAncestry(
            dataframe_id).ancestors
        self.assertEqual(len(ancestry), 4)

        df_1 = ancestry[0]
        df_2 = ancestry[1]
        df_3 = ancestry[2]
        df_4 = ancestry[3]

        self.assertEqual(df_1.tag, 'training data')
        self.assertEqual(df_2.tag, 'data with dropped id and target columns')
        self.assertEqual(df_3.tag, 'dropped id column')
        self.assertEqual(df_4.tag, 'original otto csv data')

    def test_models_derived_from_dataframe(self):
        """
        Tests if models are properly derived from dataframe, given id
        """
        dataframe_id = self.syncer_obj.id_for_object[id(self.x_train)]

        # Two models use the x_train dataset.
        model_ids = self.syncer_obj.client.modelsDerivedFromDataFrame(
            dataframe_id)
        self.assertEqual(len(model_ids), 2)
        model1_spec = self.syncer_obj.client.getModel(
            model_ids[0]).specification
        model2_spec = self.syncer_obj.client.getModel(
            model_ids[1]).specification
        self.assertEqual(model1_spec.transformerType, 'BaggingClassifier')
        self.assertEqual(model2_spec.transformerType, 'CalibratedClassifierCV')

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

        # There are three models: LabelEncoder, BaggingClassifier,
        # CalibratedClassifierCV
        self.assertEqual(len(model_responses), 3)
        # The classifier models have metrics
        model2 = model_responses[1]
        model3 = model_responses[2]

        self.assertEqual(len(model2.metrics), 1)
        self.assertEqual(len(model3.metrics), 1)

        dataframe_id = self.syncer_obj.id_for_object[id(self.x_test)]
        # Calibrated Classifier has lower log loss than Bagging Classfier
        self.assertGreater(
            model2.metrics['log_loss'][
                dataframe_id], model3.metrics['log_loss'][dataframe_id])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Pass in -test flag if you wish'
        ' to run unittests on this workflow')
    parser.add_argument('-test', action='store_true')
    args = parser.parse_args()
    if args.test:
        suite = unittest.TestLoader().loadTestsFromTestCase(
            TestOttoCalibration)
        unittest.TextTestRunner().run(suite)
    else:
        run_otto_workflow()
