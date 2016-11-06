"""
Source: https://www.kaggle.com/cbourguignat/otto-group-product-classification-challenge/why-calibration-works
"""
import os
import pandas as pd
import sklearn
from sklearn.preprocessing import LabelEncoder
from sklearn.cross_validation import train_test_split
from sklearn.ensemble import RandomForestClassifier, BaggingClassifier 
from sklearn.metrics import log_loss
from sklearn.calibration import CalibratedClassifierCV

from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableRandomSplit
from modeldb.sklearn_native import SyncableMetrics


# During the Otto Group competition, some Kagglers discussed in the forum about Calibration for Random Forests.
# It was a brand new functionality of the last scikit-learn version (0.16) : 
# see : http://scikit-learn.org/stable/whats_new.html
# Calibration makes that the output of the models gives a true probability of a sample to belong to a particular class
# For instance, a well calibrated (binary) classifier should classify the samples such that among the samples 
# to which it gave a predict_proba value close to 0.8, approximately 80% actually belong to the positive class
# See http://scikit-learn.org/stable/modules/calibration.html for more details
# This script is an example of how to implement calibration, and check if it boosts performance.

ROOT_DIR = '../../../../server/'

# Adding this so we clear the database before each run (easier to debug)
os.system("cat " + ROOT_DIR + "codegen/sqlite/clearDb.sql "
                  "| sqlite3 " + ROOT_DIR + "modeldb_test.db")
name = "test1"
author = "author"
description = "kaggle-otto-script"
# Creating a new project
SyncerObj = Syncer(
        NewOrExistingProject(name, author, description),
        NewOrExistingExperiment("expName", "expDesc"),
        NewExperimentRun("otto test"))

# Import Data
X = pd.read_csv_sync('../data/otto-train.csv')

X = X.drop_sync('id', axis=1)

# Extract target
# Encode it to make it manageable by ML algo
y = X.target.values

y = LabelEncoder().fit_transform_sync(y)

# Remove target from train, else it's too easy ...
X = X.drop_sync('target', axis=1)

SyncerObj.instance.add_tag(X, "data - label encoded data")

# Split Train / Test
Xtrain, Xtest, ytrain, ytest = cross_validation.train_test_split_sync(X, y, test_size=0.20, random_state=36)

SyncerObj.instance.add_tag(Xtest, "testing data")
# First, we will train and apply a Random Forest WITHOUT calibration
# we use a BaggingClassifier to make 5 predictions, and average
# because that's what CalibratedClassifierCV do behind the scene,
# and we want to compare things fairly, i.e. be sure that averaging several models 
# is not what explains a performance difference between no calibration, and calibration.

clf = RandomForestClassifier(n_estimators=50, n_jobs=-1)

clfbag = BaggingClassifier(clf, n_estimators=5)
clfbag.fit_sync(Xtrain, ytrain)
#NOTE: Need syncing for predict_proba
ypreds = clfbag.predict_proba(Xtest)
SyncableMetrics.compute_metrics(clfbag, log_loss, ytest, ypreds, Xtest, "", "", eps=1e-15, normalize=True)
#print("loss WITHOUT calibration : ", log_loss(ytest, ypreds, eps=1e-15, normalize=True))


# Now, we train and apply a Random Forest WITH calibration
# In our case, 'isotonic' worked better than default 'sigmoid'
# This is not always the case. Depending of the case, you have to test the two possibilities

clf = RandomForestClassifier(n_estimators=50, n_jobs=-1)
calibrated_clf = CalibratedClassifierCV(clf, method='isotonic', cv=5)
calibrated_clf.fit_sync(Xtrain, ytrain)
ypreds = calibrated_clf.predict_proba(Xtest)
SyncableMetrics.compute_metrics(calibrated_clf, log_loss, ytest, ypreds, Xtest, "", "", eps=1e-15, normalize=True)

#print("loss WITH calibration : ", log_loss(ytest, ypreds, eps=1e-15, normalize=True))

print(" ")
print("Conclusion : in our case, calibration improved performance a lot ! (reduced loss)")
SyncerObj.instance.sync()
# We can see that we highly improved performance with calibration (loss is reduced) !
# Using calibration helped our team a lot to climb the leaderboard.
# In the future competitions, that's for sure, I will not forget to test this trick !