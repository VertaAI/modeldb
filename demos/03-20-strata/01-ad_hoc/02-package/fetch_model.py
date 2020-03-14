import boto3

boto3.client('s3').download_file("verta-strata", "models/01/model.spacy", "model.spacy")
boto3.client('s3').download_file("verta-strata", "models/01/model_metadata.json", "model_metadata.json")