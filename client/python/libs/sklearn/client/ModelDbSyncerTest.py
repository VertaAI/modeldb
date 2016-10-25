#!/usr/bin/python
import ModelDbSyncer
import sys
sys.path.append('./thrift/gen-py')
from modeldb import ModelDBService
from modeldb.ttypes import *
import SyncableProjectEvent
import SyncableExperimentEvent
import SyncableExperimentRunEvent

class SyncerTest(ModelDbSyncer.Syncer):
    def setup(self, projectConfig, experimentConfig, experimentRunConfig):
        self.setProject(projectConfig)
        self.setExperiment(experimentConfig)
        self.setExperimentRun(experimentRunConfig)
    def setExperiment(self, experimentConfig):
        print("test setExperiment")
        self.experiment = experimentConfig.toThrift()
        self.experiment.projectId = self.project.id
        experimentEvent = SyncableExperimentEvent.SyncExperimentEvent(
            self.experiment)
        #experimentEvent.sync()
    
    def setExperimentRun(self, experimentRunConfig):
        print("test setExperimentRun")
        self.experimentRun = experimentRunConfig.toThrift()
        self.experimentRun.experimentId = self.experiment.id
        experimentRunEvent = \
          SyncableExperimentRunEvent.SyncExperimentRunEvent(self.experimentRun)
        #experimentRunEvent.sync()

    def setProject(self, projectConfig):
        print("test setProject")
        self.project = projectConfig.toThrift()
        # TODO: can we clean up this construct: SyncableBlah.syncblah
        projectEvent = SyncableProjectEvent.SyncProjectEvent(self.project)
        #projectEvent.sync()

    def makeFitEvent(self):
        self.syncableTransformer = self.convertModeltoThrift(self.model)
        self.modelSpec = self.convertSpectoThrift(self.spec,self.df)
        self.syncableDataFrame = self.convertDftoThrift(self.df)
        fe = FitEvent(self.syncableDataFrame, self.modelSpec, self.syncableTransformer, 
                            [], [], [], self.experiment.id)
        return fe

    def sync():
        print("SYNC DO NOT DO ANYTHING")
