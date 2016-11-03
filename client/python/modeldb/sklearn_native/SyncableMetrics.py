#!/usr/bin/python
import numpy as np
import pandas as pd
import ModelDbSyncer
from ..events import MetricEvent
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
import sklearn.metrics

#Computes various scores for models, such as precision, recall, and f1_score.
def computeMetrics(model, metricFunc, X, predictionCol, labelCol, actual):
	predicted = model.predict(X)
	score = metricFunc(actual, predicted)
	metricEvent = MetricEvent(X, model, labelCol, predictionCol, metricFunc.__name__, score)
	ModelDbSyncer.Syncer.instance.addToBuffer(metricEvent)
	return score
