#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
import events.RandomSplitEvent as RandomSplitEvent

#Splits X according to the weights provided. If the optional y dataframe is provided, it will also be split accordingly.
def randomSplit(X, weights, seed, y=None):
    result = []
    yresult = []
    np.random.seed(seed)
    df = X
    s = float(sum(weights))
    cweights = [0.0]
    for w in weights:
        cweights.append(cweights[-1] + w / s)
    zipped = zip(cweights,cweights[1:])
    for i in range(0,len(zipped)-1):
        lower_bound, higher_bound = zipped[i]
        #generating the correct mask for the dataframe, based on the weights array
        msk = np.logical_and(np.random.rand(len(df)) <= higher_bound, np.random.rand(len(df)) >= lower_bound)
        #when mask is applied to dataframe, it splits the frame randomly
        result.append(df[msk])
        df = df[~msk]
        if y is not None:
            yresult.append(y[msk])
            y = y[~msk]
    result.append(df)
    yresult.append(y)
    randomEvent = RandomSplitEvent.SyncRandomSplitEvent(X, weights, seed, result, ModelDbSyncer.Syncer.instance.experimentRun.id)
    ModelDbSyncer.Syncer.instance.addToBuffer(randomEvent)
    return result, yresult
