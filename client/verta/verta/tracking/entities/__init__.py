# -*- coding: utf-8 -*-

from verta._internal_utils import documentation

from ._project import Project
from ._projects import Projects
from ._experiment import Experiment
from ._experiments import Experiments
from ._experimentrun import ExperimentRun
from ._experimentruns import ExperimentRuns


documentation.reassign_module(
    [
        Project,
        Projects,
        Experiment,
        Experiments,
        ExperimentRun,
        ExperimentRuns,
    ],
    module_name=__name__,
)
