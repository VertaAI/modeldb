from sklearn.feature_extraction.text import TfidfTransformer
from sklearn.linear_model import SGDClassifier
from sklearn.grid_search import GridSearchCV
from sklearn.pipeline import Pipeline

from modeldb.sklearn_native.ModelDbSyncer import *

# Uses GridSearch and Pipeline objects in scikit, adapted from
# http://scikit-learn.org/stable/auto_examples/model_selection/grid_search_text_feature_extraction.html
name = "grid search cross validation"
author = "srinidhi"
description = "digits dataset"
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

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

clf = GridSearchCV(
    pipeline, parameters, cv=None, scoring='%s_weighted' % 'precision')

clf.fit_sync(x, y)
syncer_obj.sync()
