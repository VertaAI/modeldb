from sklearn.grid_search import GridSearchCV
from sklearn import datasets
from sklearn.metrics import precision_score
from sklearn.svm import SVC

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableMetrics

# This is a sample usage of GridSearch in scikit, adapted from
# http://scikit-learn.org/stable/auto_examples/model_selection/grid_search_digits.html
name = "grid search"
author = "srinidhi"
description = "digits dataset"
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    DefaultExperiment(),
    NewExperimentRun("Abc"))

# Loading the Digits dataset
digits = datasets.load_digits()

# To apply an classifier on this data, we need to flatten the image, to
# turn the data in a (samples, feature) matrix:
n_samples = len(digits.images)
X = digits.images.reshape((n_samples, -1))
y = digits.target

# Split the dataset in two equal parts
x_train, x_test, y_train, y_test = cross_validation.train_test_split_sync(
    X, y, test_size=0.5, random_state=0)

# Set the parameters by cross-validation
tuned_parameters = [{'kernel': ['rbf'], 'gamma': [1e-3, 1e-4],
                     'C': [1, 10, 100, 1000]},
                    {'kernel': ['linear'], 'C': [1, 10, 100, 1000]}]

clf = GridSearchCV(SVC(C=1), tuned_parameters, cv=5)
clf.fit_sync(x_train, y_train)

print("The model is trained on the full development set.")
print("The scores are computed on the full evaluation set.")
y_pred = clf.predict_sync(x_test)
mean_error = SyncableMetrics.compute_metrics(
    clf, precision_score, y_test, y_pred, x_test, '', '')

syncer_obj.sync()
