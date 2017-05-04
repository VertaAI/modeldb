import unittest
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn import linear_model
from sklearn import cross_validation
import pandas as pd


class TestMetricEvent(unittest.TestCase):

    @classmethod
    def setUp(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None))
        model = linear_model.LinearRegression()
        np.random.seed(0)
        X = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 4)), columns=list('ABCD'))
        y = pd.DataFrame(np.random.randint(
            0, 100, size=(100, 1)), columns=['output'])

        # Add tags for models / dataframes
        syncer_obj.add_tag(X, "digits-dataset")
        syncer_obj.add_tag(model, "linear reg")

        syncer_obj.clear_buffer()

        scores = cross_validation.cross_val_score_sync(model, X, y, cv=3)

        self.events = syncer_obj.sync()

    def test_events(self):
        # There should be 3 FitEvents and 3 MetricEvents
        self.assertEqual(len(self.events), 6)

        # Validate fit and metric event structs
        for i in range(0, 5, 2):
            utils.validate_fit_event_struct(self.events[i], self)
            utils.validate_metric_event_struct(self.events[i + 1], self)

    def test_metric_events(self):
        metric_event_1 = self.events[1]
        metric_event_2 = self.events[3]
        metric_event_3 = self.events[5]
        self.assertAlmostEqual(metric_event_1.metricValue, -0.273693, places=4)
        self.assertAlmostEqual(metric_event_2.metricValue, -0.007407, places=4)
        self.assertAlmostEqual(metric_event_3.metricValue, -0.086532, places=4)

        self.assertEqual(metric_event_1.metricType, 'accuracy')
        self.assertEqual(metric_event_2.metricType, 'accuracy')
        self.assertEqual(metric_event_3.metricType, 'accuracy')


if __name__ == '__main__':
    unittest.main()
