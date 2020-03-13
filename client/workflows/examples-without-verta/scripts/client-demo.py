"""Logistic Regression with Grid Search (scikit-learn)"""

import os, sys
import itertools

import joblib

import pandas as pd
import numpy as np

from sklearn import model_selection
from sklearn import linear_model
from sklearn import metrics

sys.path.append(os.path.join("..", "modeldb"))
from modeldbclient import ModelDBClient

import warnings
warnings.filterwarnings('ignore')


# Logging Workflow

# instantiate client
client = ModelDBClient()
proj = client.set_project("Test Project")
expt = client.set_experiment("Test Experiment")


# load pre-cleaned data from CSV file into pandas DataFrame
data_path = os.path.join("..", "data", "census", "cleaned-census-data.csv")
df = pd.read_csv(data_path, delimiter=',')

# split into features and labels
features_df = df.drop('>50K', axis='columns')
labels_df = df['>50K']  # we are predicting whether an individual's income exceeds $50k/yr

# extract NumPy arrays from DataFrames
X = features_df.values
y = labels_df.values

# split data into training, validation, and testing sets
X_train, X_test, y_train, y_test = model_selection.train_test_split(X, y, test_size=0.20, shuffle=False)
X_train, X_val, y_train, y_val = model_selection.train_test_split(X_train, y_train, test_size=0.20, shuffle=False)


# define hyperparameters
hyperparam_candidates = {
    'C': [1e-1, 1, 1e1],
    'solver': ['lbfgs'],
    'max_iter': [1e3, 1e4, 1e5],
}
hyperparam_sets = [dict(zip(hyperparam_candidates.keys(), values))
                   for values
                   in itertools.product(*hyperparam_candidates.values())]

# grid search through hyperparameters
for hyperparam_num, hyperparams in enumerate(hyperparam_sets):
    # create object to track experiment run
    run = client.set_experiment_run(f"run {hyperparam_num}")

    # log hyperparameters
    for key, val in hyperparams.items():
        run.log_hyperparameter(key, val)
    print(hyperparams, end=' ')

    # log data
    run.log_dataset("data", data_path)

    # create and train model
    model = linear_model.LogisticRegression(**hyperparams)
    model.fit(X_train, y_train)

    # calculate and log validation accuracy
    val_acc = model.score(X_val, y_val)
    run.log_metric("validation accuracy", val_acc)
    print(f"Validation accuracy: {val_acc}")

    # save and log model
    model_path = os.path.join("..", "output", "client-demo", f"logreg_gridsearch_{hyperparam_num}.gz")
    joblib.dump(model, model_path)
    run.log_model(model_path)


# close client
client.disconnect()


# fetch existing project, experiment, and experiment runs
client = ModelDBClient()
proj = client.set_project("Test Project")
expt = client.set_experiment("Test Experiment")
client.set_experiment_runs()


# fetch best experiment run based on validation accuracy
best_run = sorted(client.expt_runs, key=lambda expt_run: expt_run.get_metrics()['validation accuracy'])[-1]

# fetch that run's hyperparameters and validation accuracy
best_hyperparams = best_run.get_hyperparameters()
best_val_acc = best_run.get_metrics()['validation accuracy']

print("Best Validation Round:")
print(f"{best_hyperparams} Validation accuracy: {best_val_acc}")


# retrain model using best set of hyperparameters
model = linear_model.LogisticRegression(**best_hyperparams)
model.fit(np.concatenate((X_train, X_val), axis=0), np.concatenate((y_train, y_val)))


print(f"Training accuracy: {model.score(X_train, y_train)}")
print(f"Testing accuracy: {model.score(X_test, y_test)}")


# close client
client.disconnect()
