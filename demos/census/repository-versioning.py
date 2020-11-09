import os
import pandas as pd
import wget

from verta import Client
from verta.dataset import Path
from verta.configuration import Hyperparameters
from verta.environment import Python
from verta.code import Git

n_splits = 5

DATASET_PATH = "./"
train_data_filename = DATASET_PATH + "census-train.csv"
test_data_filename = DATASET_PATH + "census-test.csv"

def download_starter_dataset(bucket_name):
    train_data_url = "http://s3.amazonaws.com/" + bucket_name + "/census-train.csv"
    wget.download(train_data_url)

    test_data_url = "http://s3.amazonaws.com/" + bucket_name + "/census-test.csv"
    wget.download(test_data_url)

download_starter_dataset("verta-starter")

df_train = pd.read_csv(train_data_filename)
df_test = pd.read_csv(test_data_filename)

client = Client(os.environ['VERTA_HOST'])

repo = client.get_or_create_repository(name="Census Income")
repo.delete()
repo = client.get_or_create_repository(name="Census Income")

def save_dataset(commit, split_id):
    # date_N_days_ago = datetime.now() - timedelta(days=n_splits-i)
    # timestamp = int(date_N_days_ago.timestamp())

    n_train = int(df_train.shape[0]*float(split_id)/n_splits)
    n_test = int(df_test.shape[0]*float(split_id)/n_splits)

    sub_df = df_train[:n_train]
    sub_df.to_csv(train_data_filename)

    sub_df = df_test[:n_test]
    sub_df.to_csv(test_data_filename)

    data = Path([train_data_filename, test_data_filename])
    branch = commit.new_branch("update-dataset")
    branch.update("datasets/base", data)
    if split_id > 1:
        branch.save("Add more data to base dataset")
    else:
        branch.save("Create base dataset")
    commit.merge(branch)
    return commit

trunk = repo.get_commit().new_branch("trunk")
trunk = save_dataset(trunk, 1)
trunk = save_dataset(trunk, 2)

trunk.merge(trunk
    .new_branch("create-hyperparams")
    .update("model/hyperparam_set", Hyperparameters(hyperparameter_sets={
        'C': [1e-6, 1e-4, 1e-2, 1e0],
        'solver': ['lbfgs'],
        'max_iter': [10, 20, 30],
    }))
    .save("Create initial set of hyperparameters")
)

trunk = save_dataset(trunk, 3)

trunk.merge(trunk
    .new_branch("create-env")
    .update("model/environment", Python(
        requirements=["pandas", "sklearn"],
        constraints=Python.read_pip_environment(),
    ))
    .save("Configure model environment")
)

trunk = save_dataset(trunk, 4)

trunk.merge(trunk
    .new_branch("create-hyperparams")
    .update("model/hyperparam_set", Hyperparameters(hyperparameter_sets={
        'C': [1e-6, 1e-4, 1e-2, 1e0],
        'solver': ['lbfgs'],
        'max_iter': [10, 20, 30],
        'balanced': [1, 0],
    }))
    .save("Create initial set of hyperparameters")
)

trunk = save_dataset(trunk, 5)

trunk.merge(trunk
    .new_branch("set-repo")
    .update("model/repo", Git())
    .save("Set repository for the model code")
)

for c in trunk.log():
    print(c)
