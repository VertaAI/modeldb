import itertools
import os
import pandas as pd

import sklearn
from sklearn import model_selection
from sklearn import linear_model
from sklearn import metrics
import warnings
from sklearn.exceptions import ConvergenceWarning
warnings.filterwarnings("ignore", category=ConvergenceWarning)
warnings.filterwarnings("ignore", category=FutureWarning)

from verta import Client
from verta.dataset import S3
from verta.utils import ModelAPI
from verta._protos.public.modeldb.versioning import VersioningService_pb2 as _VersioningService

client = Client(os.environ['VERTA_HOST'])
dataset = client.get_dataset(name="Census Income S3")
dataset_version = dataset.get_latest_version()

# content = dataset_version._msg.dataset_blob
# content = _VersioningService.Blob(dataset=content)
# content = S3._from_proto(content)
# print(content)
# content.download("./")

df_train = pd.read_csv("census-train.csv")
X_train = df_train.iloc[:,:-1]
y_train = df_train.iloc[:, -1]

project = client.set_project(name="Census Income S3")
project.delete()
project = client.set_project(name="Census Income S3")
experiment = client.set_experiment(name="Linear regression")

hyperparam_candidates = {
    'C': [1e-6, 1e-4, 1e-2, 1e0],
    'solver': ['lbfgs'],
    'max_iter': [10, 20, 30],
    'balanced': [1, 0],
}
hyperparam_sets = [dict(zip(hyperparam_candidates.keys(), values))
                   for values
                   in itertools.product(*hyperparam_candidates.values())]

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
    hyperparams['class_weight'] = 'balanced' if hyperparams['balanced'] else None
    del hyperparams['balanced']

    # create and train model
    model = linear_model.LogisticRegression(**hyperparams)
    model.fit(X_train, y_train)

    # calculate and log validation accuracy
    train_acc = model.score(X_val_train, y_val_train)
    run.log_metric("train_acc", train_acc)
    val_acc = model.score(X_val_test, y_val_test)
    run.log_metric("val_acc", val_acc)
    print("Validation accuracy: {:.4f}".format(val_acc))

    # create deployment artifacts
    model_api = ModelAPI(X_train, y_train)
    requirements = ["scikit-learn"]

    # save and log model
    run.log_model(model, model_api=model_api)
    run.log_requirements(requirements)

    # log dataset snapshot as version
    run.log_dataset_version("train", dataset_version)

    # log Git information as code version
    run.log_code()

# from multiprocessing import Pool
# with Pool(8) as p:
#     p.map(run_experiment, hyperparam_sets)
for hyperparams in hyperparam_sets:
    run_experiment(hyperparams)

print(project.expt_runs.find('hyperparameters.balanced == 1').as_dataframe())
