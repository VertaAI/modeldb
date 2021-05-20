import os

import boto3

bucket = os.environ["BUCKET"]
model_path = os.environ["MODEL_PATH"]
metadata_path = os.environ["METADATA_PATH"]

boto3.client("s3").download_file(bucket, model_path, "model.spacy")
boto3.client("s3").download_file(bucket, metadata_path, "model_metadata.json")
