#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import GridCrossValidation
import events.GridSearchCVEvent as GridSearchCVEvent

#Overrides GridSearchCV's fit function.
def fitFnGridSearch(self, X,y):
    GridCrossValidation.fit(self,X,y)
    [inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds] = self.gridCVevent

    #Calls SyncGridCVEvent and adds to buffer.
    gridEvent = GridSearchCVEvent.SyncGridCVEvent(inputDataFrame, crossValidations, seed, evaluator, bestModel, bestEstimator, numFolds, ModelDbSyncer.Syncer.instance.experimentRun.id)
    ModelDbSyncer.Syncer.instance.addToBuffer(gridEvent)