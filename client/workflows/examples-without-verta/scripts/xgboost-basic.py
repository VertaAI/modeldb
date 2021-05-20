"""Random Forest (XGBoost)"""

import os
import itertools

import joblib

import pandas as pd
import numpy as np

from sklearn import model_selection
from sklearn import metrics

import xgboost as xgb


# load pre-cleaned data from CSV file into pandas DataFrame
df = pd.read_csv(os.path.join("..", "data", "census", "cleaned-census-data.csv"), delimiter=',')

# split into features and labels
features_df = df.drop('>50K', axis='columns')
labels_df = df['>50K']  # we are predicting whether an individual's income exceeds $50k/yr


# extract NumPy arrays from DataFrames
X = features_df.values
y = labels_df.values

# split data into training and testing sets
X_train, X_test, y_train, y_test = model_selection.train_test_split(X, y, test_size=0.2)
# split training data into training and validation sets
X_train, X_val, y_train, y_val = model_selection.train_test_split(X_train, y_train, test_size=0.2)

# load data into XGBoost DMatrices
dtrain = xgb.DMatrix(X_train, y_train)
dval = xgb.DMatrix(X_val, y_val)
dtest = xgb.DMatrix(X_test, y_test)


# define hyperparameter values
hyperparams = {
    'eta': 0.5,
    'max_depth': 7,
}
print(hyperparams)
hyperparams['objective'] = "binary:logistic"
hyperparams['eval_metric'] = ['error']

num_rounds = 20
eval_list = [(dtrain, 'train'), (dval, 'val')]


# train model, while evaluating on validation set
bst = xgb.train(hyperparams, dtrain, num_rounds, eval_list)


print(f"Training accuracy: {metrics.accuracy_score(dtrain.get_label(), bst.predict(dtrain).round())}")
print(f"Testing accuracy: {metrics.accuracy_score(dtest.get_label(), bst.predict(dtest).round())}")

print(f"Training F-score: {metrics.f1_score(dtrain.get_label(), bst.predict(dtrain).round())}")
print(f"Testing F-score: {metrics.f1_score(dtest.get_label(), bst.predict(dtest).round())}")


# save model to disk
joblib.dump(bst, os.path.join("..", "output", "xgboost.gz"))
