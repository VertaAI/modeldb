import os
import unittest
import argparse

from sklearn import svm
from sklearn.datasets import samples_generator
from sklearn.feature_selection import SelectKBest
from sklearn.feature_selection import f_regression
from sklearn.metrics import f1_score
from sklearn.metrics import precision_score
from sklearn.pipeline import Pipeline

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

ROOT_DIR = '../../../../server/'

# This is an extension of a common usage of Pipeline in scikit - adapted from
# http://scikit-learn.org/stable/modules/generated/sklearn.pipeline.Pipeline.html#sklearn.pipeline.Pipeline


def run_pipeline_anova_workflow():
    name = "pipeline scikit example"
    author = "srinidhi"
    description = "anova filter pipeline"
    syncer_obj = Syncer(
        NewOrExistingProject(name, author, description),
        DefaultExperiment(),
        NewExperimentRun("Abc"))

    # import some data to play with
    X, y = samples_generator.make_classification(
        n_informative=5, n_redundant=0, random_state=42)

    x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
        X, y, test_size=0.3, random_state=0)
    syncer_obj.add_tag(X, "samples generated data")
    syncer_obj.add_tag(x_train, "training data")
    syncer_obj.add_tag(x_test, "testing data")

    # ANOVA SVM-C
    # 1) anova filter, take 5 best ranked features
    anova_filter = SelectKBest(f_regression, k=5)
    syncer_obj.add_tag(anova_filter, "Anova filter, with k=5")
    # 2) svm
    clf = svm.SVC(kernel='linear')
    syncer_obj.add_tag(clf, "SVC with linear kernel")
    anova_svm = Pipeline([('anova', anova_filter), ('svc', clf)])

    syncer_obj.add_tag(anova_svm, "Pipeline with anova_filter and SVC")

    # Fit the pipeline on the training set
    anova_svm.fit_sync(x_train, y_train)
    y_pred = anova_svm.predict(x_test)
    # Compute metrics for the model on the testing set
    f1 = SyncableMetrics.compute_metrics(
        anova_svm, f1_score, y_test, y_pred, x_test, "predictionCol",
        'label_col')
    precision = SyncableMetrics.compute_metrics(
        anova_svm, precision_score, y_test, y_pred, x_test, "predictionCol",
        'label_col')
    syncer_obj.sync()
    return syncer_obj, f1, precision, x_train, x_test


class TestPipelineEndToEnd(unittest.TestCase):
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
        self.syncer_obj, self.f1, self.precision, self.x_train, self.x_test = run_pipeline_anova_workflow()

    def test_project(self):
        """
        Tests if project is stored correctly.
        """
        projectOverview = self.syncer_obj.client.getProjectOverviews()[0]
        project = projectOverview.project
        self.assertEqual(project.description, 'anova filter pipeline')
        self.assertEqual(project.author, 'srinidhi')
        self.assertEqual(project.name, 'pipeline scikit example')
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
        self.assertEqual(len(model_responses), 3)

        model1 = model_responses[0]
        model2 = model_responses[1]
        model3 = model_responses[2]

        self.assertEqual(model1.projectId, project.id)
        self.assertEqual(model2.projectId, project.id)
        self.assertEqual(model3.projectId, project.id)

        transformer1 = model1.specification
        transformer2 = model2.specification
        transformer3 = model3.specification

        self.assertEqual(transformer1.transformerType, 'Pipeline')
        self.assertEqual(transformer2.transformerType, 'SelectKBest')
        self.assertEqual(transformer3.transformerType, 'SVC')

        self.assertEqual(transformer1.tag,
                         'Pipeline with anova_filter and SVC')
        self.assertEqual(transformer2.tag, 'Anova filter, with k=5')
        self.assertEqual(transformer3.tag, 'SVC with linear kernel')

        # Check hyperparameters for both models
        hyperparams1 = transformer1.hyperparameters
        hyperparams2 = transformer2.hyperparameters
        hyperparams3 = transformer3.hyperparameters
        self.assertEqual(len(hyperparams1), 19)
        self.assertEqual(len(hyperparams2), 2)
        self.assertEqual(len(hyperparams3), 14)

    def test_dataframe_ancestry(self):
        """
        Tests if dataframe ancestry is stored correctly.
        """
        # Check ancestry for x_test and x_train.
        # The data the models were trained and tested on.
        print("x_train_id", self.syncer_obj.get_modeldb_id_for_object(
                    self.x_train))
        print("x_test_id", self.syncer_obj.get_modeldb_id_for_object(
                    self.x_test))

        for df in [self.x_train, self.x_test]:
            dataframe_id = self.syncer_obj.get_modeldb_id_for_object(df)
            print(dataframe_id)

            ancestry = self.syncer_obj.client.getDataFrameAncestry(
                dataframe_id).ancestors
            self.assertEqual(len(ancestry), 2)
            df_1 = ancestry[0]
            df_2 = ancestry[1]
            if df is self.x_train:
                self.assertEqual(df_1.tag, 'training data')
            if df is self.x_test:
                self.assertEqual(df_1.tag, 'testing data')
            # Ancestor is the original dataframe
            self.assertEqual(df_2.tag, 'samples generated data')

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
        model3 = model_responses[2]

        # Metrics are only stored for the overall pipeline model.
        self.assertEqual(len(model1.metrics), 2)
        self.assertEqual(len(model2.metrics), 0)
        self.assertEqual(len(model3.metrics), 0)
        self.assertIn('f1_score', model1.metrics)
        self.assertIn('precision_score', model1.metrics)

        # Metrics are mapped to their associated dataframe.
        dataframe_id = self.syncer_obj.get_modeldb_id_for_object(self.x_test)
        self.assertAlmostEqual(
            self.f1, model1.metrics['f1_score'][dataframe_id], places=4)
        self.assertAlmostEqual(
            self.precision, model1.metrics['precision_score'][dataframe_id],
            places=4)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Pass in -test flag if you wish'
        ' to run unittests on this workflow')
    parser.add_argument('-test', action='store_true')
    args = parser.parse_args()
    if args.test:
        suite = unittest.TestLoader().loadTestsFromTestCase(
            TestPipelineEndToEnd)
        unittest.TextTestRunner().run(suite)
    else:
        run_pipeline_anova_workflow()
