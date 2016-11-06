import numpy as np
import pandas as pd
import sys

from sklearn import preprocessing
from sklearn import linear_model
from sklearn import svm
from sklearn.datasets import samples_generator
from sklearn.feature_selection import SelectKBest
from sklearn.feature_selection import f_regression
from sklearn.metrics import f1_score
from sklearn.metrics import precision_score
from sklearn.pipeline import Pipeline

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics
from modeldb.sklearn_native import SyncableRandomSplit


#This is an extension of a common usage of Pipeline in scikit - adapted from http://scikit-learn.org/stable/modules/generated/sklearn.pipeline.Pipeline.html#sklearn.pipeline.Pipeline

name = "pipeline scikit example"
author = "srinidhi"
description = "anova filter pipeline"
# SyncerObj.startExperiment("basic pipeline")
SyncerObj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

#import some data to play with
X, y = samples_generator.make_classification(
    n_informative=5, n_redundant=0, random_state=42)

x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
    X, y, test_size=0.3, random_state=0)

# ANOVA SVM-C
# 1) anova filter, take 5 best ranked features
anova_filter = SelectKBest(f_regression, k=5)
# 2) svm
clf = svm.SVC(kernel='linear')
anova_svm = Pipeline([('anova', anova_filter),('svc', clf)])

#Fit the pipeline on the training set
anova_svm.fit_sync(x_train, y_train)

#Compute metrics for the model on the testing set

SyncableMetrics.compute_metrics(anova_svm, f1_score, x_test, "predictionCol", "label_col", y_test)
SyncableMetrics.compute_metrics(anova_svm, precision_score, x_test, "predictionCol", "label_col",y_test)

SyncerObj.instance.sync()