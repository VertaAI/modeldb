"""Logistic Regression (scikit-learn)"""

import os

import joblib

import pandas as pd
import numpy as np

from sklearn import model_selection
from sklearn import linear_model
from sklearn import metrics


# load pre-cleaned data from CSV file into pandas DataFrame
df = pd.read_csv(os.path.join("..", "data", "census", "cleaned-census-data.csv"), delimiter=',')

# split into features and labels
features_df = df.drop('>50K', axis='columns')
labels_df = df['>50K']  # we are predicting whether an individual's income exceeds $50k/yr


# extract NumPy arrays from DataFrames
X = features_df.values
y = labels_df.values

# split data into training and testing sets
X_train, X_test, y_train, y_test = model_selection.train_test_split(X, y, test_size=0.33)

# instantiate iterator that yields train/val indices for each fold of cross validation
validation_splitter = model_selection.KFold(n_splits=5, shuffle=True)


# define hyperparameter values
hyperparams = {
    'C': 1.0,
    'solver': 'lbfgs',
    'max_iter': 10000,
}
print(hyperparams, end=' ')

# cross-validate hyperparameter values
val_acc = 0  # track average validation accuracy across folds
for idxs_train, idxs_val in validation_splitter.split(X_train, y_train):
    # index into training data to produce train/val splits
    X_val_train, y_val_train = X[idxs_train], y[idxs_train]
    X_val, y_val = X[idxs_val], y[idxs_val]

    # create and fit model
    model = linear_model.LogisticRegression(**hyperparams)
    model.fit(X_val_train, y_val_train)

    # accumulate average validation accuracy
    val_acc += model.score(X_val, y_val)/validation_splitter.get_n_splits()

print(f"Validation accuracy: {val_acc}")


# create and fit model using proven hyperparameters
model = linear_model.LogisticRegression(**hyperparams)
model.fit(X_train, y_train)

print(f"Training accuracy: {model.score(X_train, y_train)}")
print(f"Testing accuracy: {model.score(X_test, y_test)}")

print(f"Training F-score: {metrics.f1_score(y_train, model.predict(X_train))}")
print(f"Testing F-score: {metrics.f1_score(y_test, model.predict(X_test))}")


# save model to disk
joblib.dump(model, os.path.join("..", "output", "logreg_basic.gz"))
