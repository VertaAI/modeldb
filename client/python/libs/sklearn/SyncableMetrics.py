#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
from events import MetricEvent
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
import sklearn.metrics

#Computes various scores for models, such as precision, recall, and f1_score.
def computeMetrics(model, metric, X, predictionCol, labelCol, actual):
    predicted = model.predict(X)
    computeMetric = metric + "_score"
    metricFunc = getattr(sklearn.metrics, computeMetric)
    score = metricFunc(actual, predicted, average='weighted')
    metricEvent = MetricEvent(X, model, labelCol, predictionCol, metric, score, ModelDbSyncer.Syncer.instance.experimentRun.id)
    ModelDbSyncer.Syncer.instance.addToBuffer(metricEvent)
