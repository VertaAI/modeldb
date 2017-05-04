import unittest
from ModelDbSyncerTest import SyncerTest

import modeldb.tests.utils as utils
from modeldb.thrift.modeldb import ttypes as modeldb_types
from modeldb.basic.Structs import (
    DefaultExperiment, NewExperimentRun, ThriftConfig)
from modeldb.sklearn_native.ModelDbSyncer import *


# Tests default experiment creation within project


class TestProjectEvent(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        name = "logistic-test"
        author = "srinidhi"
        description = "income-level logistic regression"
        syncer_obj = SyncerTest(
            NewOrExistingProject(name, author, description),
            DefaultExperiment(),
            NewExperimentRun("Abc"),
            ThriftConfig(None, None),
            )
        events = syncer_obj.sync()
        self.project_event = events[0]
        self.experiment_event = events[1]
        self.experiment_run_event = events[2]

    def test_project_creation(self):
        project = self.project_event.project
        utils.validate_project_struct(project, self)
        expected_project = modeldb_types.Project(
            -1,
            'logistic-test',
            'srinidhi',
            'income-level logistic regression')
        utils.is_equal_project(expected_project, project, self)

    def test_experiment_creation(self):
        experiment = self.experiment_event.experiment
        utils.validate_experiment_struct(experiment, self)
        expected_experiment = modeldb_types.Experiment(
            -1,
            -1,
            '',
            '',
            True)
        utils.is_equal_experiment(expected_experiment, experiment, self)

    def test_experiment_run_creation(self):
        experiment_run = self.experiment_run_event.experimentRun
        utils.validate_experiment_run_struct(experiment_run, self)
        expected_experiment_run = modeldb_types.ExperimentRun(
            -1,
            -1,
            'Abc')
        utils.is_equal_experiment_run(
            expected_experiment_run, experiment_run, self)

# Tests new experiment creation


class TestNewProjectEvent(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        syncer_obj = SyncerTest(
            NewOrExistingProject("name", "author", "desc"),
            NewOrExistingExperiment("expName", "expDesc"),
            NewExperimentRun("expRunDesc"),
            ThriftConfig(None, None))
        events = syncer_obj.sync()
        self.project_event = events[0]
        self.experiment_event = events[1]
        self.experiment_run_event = events[2]

    def test_new_project_creation(self):
        project = self.project_event.project
        utils.validate_project_struct(project, self)
        expected_project = modeldb_types.Project(
            -1,
            'name',
            'author',
            'desc')
        utils.is_equal_project(expected_project, project, self)

    def test_new_experiment_creation(self):
        experiment = self.experiment_event.experiment
        utils.validate_experiment_struct(experiment, self)
        expected_experiment = modeldb_types.Experiment(
            -1,
            -1,
            'expName',
            'expDesc',
            False)
        utils.is_equal_experiment(expected_experiment, experiment, self)

    def test_new_experiment_run_creation(self):
        experiment_run = self.experiment_run_event.experimentRun
        utils.validate_experiment_run_struct(experiment_run, self)
        expected_experiment_run = modeldb_types.ExperimentRun(
            -1,
            -1,
            'expRunDesc')
        utils.is_equal_experiment_run(
            expected_experiment_run, experiment_run, self)


if __name__ == '__main__':
    unittest.main()
