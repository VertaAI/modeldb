import boto3
from datetime import datetime, timedelta
import os
import pandas as pd
import wget

from verta import Client
from verta.dataset import S3

enable_mdb_versioning = True
n_splits = 5

DATASET_PATH = "./"
train_data_filename = DATASET_PATH + "census-train.csv"
test_data_filename = DATASET_PATH + "census-test.csv"

def download_starter_dataset(bucket_name):
    if not os.path.exists(DATASET_PATH + "census-train.csv"):
        train_data_url = "http://s3.amazonaws.com/" + bucket_name + "/census-train.csv"
        if not os.path.isfile(train_data_filename):
            wget.download(train_data_url)

    if not os.path.exists(DATASET_PATH + "census-test.csv"):
        test_data_url = "http://s3.amazonaws.com/" + bucket_name + "/census-test.csv"
        if not os.path.isfile(test_data_filename):
            wget.download(test_data_url)

download_starter_dataset("verta-starter")

df_train = pd.read_csv(train_data_filename)
df_test = pd.read_csv(test_data_filename)

client = Client(os.environ['VERTA_HOST'])
s3_client = boto3.client('s3')


dataset = client._set_dataset2(name="Census Income S3")
dataset.delete()
dataset = client._set_dataset2(name="Census Income S3")

for i in range(n_splits):
    print(i)
    split_id = i+1

    # date_N_days_ago = datetime.now() - timedelta(days=n_splits-i)
    # timestamp = int(date_N_days_ago.timestamp())

    n_train = int(df_train.shape[0]*float(split_id)/n_splits)
    n_test = int(df_test.shape[0]*float(split_id)/n_splits)

    sub_df = df_train[:n_train]
    sub_df.to_csv("temp.csv")
    # os.utime("temp.csv", (timestamp, timestamp))
    s3_client.upload_file("temp.csv", "vertaai-demos-us-east-1", "census/train.csv")

    sub_df = df_test[:n_test]
    sub_df.to_csv("temp.csv")
    # os.utime("temp.csv", (timestamp, timestamp))
    s3_client.upload_file("temp.csv", "vertaai-demos-us-east-1", "census/test.csv")

    s3_data = S3("s3://vertaai-demos-us-east-1/census/", enable_mdb_versioning=enable_mdb_versioning)
    dataset_version = dataset.create_version(s3_data)
    dataset_version.add_attribute("n_train", n_train)
    dataset_version.add_attribute("n_test", n_test)
