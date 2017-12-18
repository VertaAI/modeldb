"""
Source: https://www.kaggle.com/kelvin0815/titanic/first-trial-following-dataquest/run/79128
"""
import os
import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.linear_model import LogisticRegression
from sklearn.cross_validation import KFold
from sklearn import cross_validation
from modeldb.sklearn_native.ModelDbSyncer import *
from modeldb.sklearn_native import SyncableRandomSplit
from modeldb.sklearn_native import SyncableMetrics

ROOT_DIR = '../../../../server/'
DATA_PATH = '../../../../data/'

name = "test1"
author = "author"
description = "kaggle-titanic-script"
# Creating a new project
syncer_obj = Syncer(
    NewOrExistingProject(name, author, description),
    NewOrExistingExperiment("expName", "expDesc"),
    NewExperimentRun("titanic test"))

# Read the training set csv file.
# Note: This dataset is not included in the repo because of Kaggle
# restrictions.
# It can be downloaded from https://www.kaggle.com/c/titanic/data
titanic = pd.read_csv_sync(DATA_PATH + 'titanic-train.csv')

# =====================Preprocessing the data=====================
# Fill the missing value in "Age".
titanic["Age"] = titanic["Age"].fillna(titanic["Age"].median())

# Converting the Sex Column to numeric value
titanic.loc[titanic["Sex"] == "male", "Sex"] = 0
titanic.loc[titanic["Sex"] == "female", "Sex"] = 1
# Converting the Embarked Column
titanic["Embarked"] = titanic["Embarked"].fillna("S")
titanic.loc[titanic["Embarked"] == "S", "Embarked"] = 0
titanic.loc[titanic["Embarked"] == "C", "Embarked"] = 1
titanic.loc[titanic["Embarked"] == "Q", "Embarked"] = 2

# NOTE: .loc commands don't create a new dataframe id
# =====================Record the prediction======================
# Making predictions with Linear Regression
predictors = ["Pclass", "Sex", "Age", "SibSp", "Parch", "Fare", "Embarked"]
alg = LinearRegression()
kf = KFold(titanic.shape[0], n_folds=3, random_state=1)
predictions = []

for train, test in kf:
    train_predictors = (titanic[predictors].iloc[train, :])
    train_target = titanic["Survived"].iloc[train]
    alg.fit_sync(train_predictors, train_target)
    test_predictions = alg.predict_sync(titanic[predictors].iloc[test, :])
    predictions.append(test_predictions)

# Evaluating error and accuracy
predictions = np.concatenate(predictions, axis=0)
predictions[predictions > .5] = 1
predictions[predictions <= .5] = 0

accuracy = 1 - sum(abs(predictions - titanic["Survived"])) / len(predictions)

print(accuracy)

# Logistic Regression
alg = LogisticRegression(random_state=1)

scores = cross_validation.cross_val_score_sync(
    alg, titanic[predictors], titanic["Survived"], cv=3)
print(scores.mean())
syncer_obj.sync()
