#!/usr/bin/python
import numpy as np
import pandas as pd
from . import ModelDbSyncer
from ..events import MetricEvent
from sklearn.linear_model import *
from sklearn.preprocessing import *
from sklearn.pipeline import Pipeline
from sklearn.grid_search import GridSearchCV
import sklearn.metrics

# Computes various scores for models, such as precision, recall, and f1_score.


def compute_metrics(
        model, metric_func, actual, predicted, X, prediction_col, label_col,
        **params):
    score = metric_func(actual, predicted, **params)
    metric_event = MetricEvent(
        X, model, label_col, prediction_col, metric_func.__name__, score)
    ModelDbSyncer.Syncer.instance.add_to_buffer(metric_event)
    return score
