import unittest
import sys
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.sklearn_native.ModelDbSyncer import *

from sklearn import linear_model
import pandas as pd

# Tests default experiment creation within project
class TestProjectEvent(unittest.TestCase):
    @classmethod
    def setUpClass(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        SyncerObj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"))
        events = SyncerTest.instance.sync()
        self.projectEvent = events[0]
        self.experimentEvent = events[1]
        self.experimentRunEvent = events[2]

    def test_project_creation(self):
        project = self.projectEvent.project
        utils.validate_project_struct(project, self)
        expected_project = modeldb_types.Project(
            -1,
            'logistic-test',
            'srinidhi',
            'income-level logistic regression')
        utils.is_equal_project(expected_project, project, self)

    def test_experiment_creation(self):
        experiment = self.experimentEvent.experiment
        utils.validate_experiment_struct(experiment, self)
        expected_experiment = modeldb_types.Experiment(
            -1,
            -1,
            '',
            '', 
            True)
        utils.is_equal_experiment(expected_experiment, experiment, self)

    def test_experiment_run_creation(self):
        experimentRun = self.experimentRunEvent.experimentRun
        utils.validate_experiment_run_struct(experimentRun, self)
        expected_experiment_run = modeldb_types.ExperimentRun(
            -1,
            -1,
            'Abc')
        utils.is_equal_experiment_run(expected_experiment_run, experimentRun, self)

# Tests new experiment creation
class TestNewProjectEvent(unittest.TestCase):
    @classmethod
    def setUpClass(self):
        SyncerObj = SyncerTest(
            NewOrExistingProject("name", "author", "desc"),
            NewOrExistingExperiment("expName", "expDesc"),
            NewExperimentRun("expRunDesc"))
        events = SyncerTest.instance.sync()
        self.projectEvent = events[0]
        self.experimentEvent = events[1]
        self.experimentRunEvent = events[2]

    def test_new_project_creation(self):
        project = self.projectEvent.project
        utils.validate_project_struct(project, self)
        expected_project = modeldb_types.Project(
            -1,
            'name',
            'author',
            'desc')
        utils.is_equal_project(expected_project, project, self)

    def test_new_experiment_creation(self):
        experiment = self.experimentEvent.experiment
        utils.validate_experiment_struct(experiment, self)
        expected_experiment = modeldb_types.Experiment(
            -1,
            -1,
            'expName',
            'expDesc', 
            False)
        utils.is_equal_experiment(expected_experiment, experiment, self)

    def test_new_experiment_run_creation(self):
        experimentRun = self.experimentRunEvent.experimentRun
        utils.validate_experiment_run_struct(experimentRun, self)
        expected_experiment_run = modeldb_types.ExperimentRun(
            -1,
            -1,
            'expRunDesc')
        utils.is_equal_experiment_run(expected_experiment_run, experimentRun, self)

if __name__ == '__main__':
    unittest.main()
