from __future__ import print_function

# configure the setup
import os
os.environ['VERTA_EMAIL'] = ''
os.environ['VERTA_DEV_KEY'] = ''
DATASET_PATH = "./"
HOST = "http://localhost:8080"

# imports
import warnings
from sklearn.exceptions import ConvergenceWarning
warnings.filterwarnings("ignore", category=ConvergenceWarning)
warnings.filterwarnings("ignore", category=FutureWarning)

import itertools
import os
import time

import six

import numpy as np
import pandas as pd

import sklearn
from sklearn import model_selection
from sklearn import linear_model
from sklearn import metrics

from verta import Client

# initialize the verta client
client = Client(HOST)

######################
# Dataset versioning #
######################

# save the CSVs as a new dataset version
from verta.dataset import Path

train_data_filename = DATASET_PATH + "census-train.csv"
test_data_filename = DATASET_PATH + "census-test.csv"

dataset = client.set_dataset(name="Census Income")
version = dataset.create_version(Path([train_data_filename, test_data_filename]))

####################
# Model versioning #
####################
# create a project and experiment for our models
proj = client.set_project("Census Income Classification")
expt = client.set_experiment("Logistic Regression")

# read the training data as a dataframe
df_train = pd.read_csv(train_data_filename)
X_train = df_train.iloc[:,:-1]
y_train = df_train.iloc[:, -1]

# configure our hyperparameter search
hyperparam_candidates = {
    'C': [1e-6, 1e-4],
    'solver': ['lbfgs'],
    'max_iter': [15, 28],
}
hyperparam_sets = [dict(zip(hyperparam_candidates.keys(), values))
                   for values
                   in itertools.product(*hyperparam_candidates.values())]

# base function to run a single experiment given a choice of hyperparameters
def run_experiment(hyperparams):
    # create object to track experiment run
    run = client.set_experiment_run()

    # create validation split
    (X_val_train, X_val_test,
     y_val_train, y_val_test) = model_selection.train_test_split(X_train, y_train,
                                                                 test_size=0.2,
                                                                 shuffle=True)

    # log hyperparameters
    run.log_hyperparameters(hyperparams)
    print(hyperparams, end=' ')

    # create and train model
    model = linear_model.LogisticRegression(**hyperparams)
    model.fit(X_train, y_train)

    # calculate and log validation accuracy
    val_acc = model.score(X_val_test, y_val_test)
    run.log_metric("val_acc", val_acc)
    print("Validation accuracy: {:.4f}".format(val_acc))

    # save what were the requirements for this model
    requirements = ["scikit-learn"]
    run.log_requirements(requirements)

    # save and log model
    run.log_model(model)

    # link the model to the dataset that was used to train it
    run.log_dataset_version("train", version)

for hyperparams in hyperparam_sets:
    run_experiment(hyperparams)

###################
# Model retrieval #
###################

# fetch the models back as a dataframe
print(proj.expt_runs.as_dataframe())

# or fetch a list sorted by validation accuracy
for run in proj.expt_runs.sort("metrics.val_acc", descending=True):
    print(run.id, run.get_metric("val_acc"))
