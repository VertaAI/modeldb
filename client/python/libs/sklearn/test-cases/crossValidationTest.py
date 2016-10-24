import numpy as np
import pandas as pd
import sys
sys.path.append('../')
sys.path.append('../thrift/gen-py')
from sklearn import preprocessing
from sklearn import linear_model
import client.ModelDbSyncer as ModelDbSyncer
import client.SyncableGridSearchCV as SyncableGridSearchCV
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.linear_model import SGDClassifier
from sklearn.grid_search import GridSearchCV
from sklearn.pipeline import Pipeline
from sklearn import datasets, linear_model, cross_validation, grid_search

#Uses GridSearch and Pipeline objects in scikit, adapted from http://scikit-learn.org/stable/auto_examples/model_selection/grid_search_text_feature_extraction.html
name = "grid search cross validation"
author = "srinidhi"
description = "digits dataset"
SyncerObj = ModelDbSyncer.Syncer([name, author, description])
SyncerObj.startExperiment("two stage pipeline")

digits = datasets.load_digits()
x = digits.data[:1000]
y = digits.target[:1000]

parameters = {
    'tfidf__use_idf': (True, False),
    'tfidf__norm': ('l1', 'l2'),
    'clf__alpha': (0.00001, 0.000001),
    'clf__penalty': ('l2', 'elasticnet')
}

pipeline = Pipeline([
    ('tfidf', TfidfTransformer()),
    ('clf', SGDClassifier()),
])

clf = GridSearchCV(pipeline, parameters, cv=None,
                       scoring='%s_weighted' % 'precision')

clf.fitSync(x,y)
SyncerObj.endExperiment()
ModelDbSyncer.Syncer.instance.sync()