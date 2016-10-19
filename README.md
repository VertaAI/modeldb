# ModelDB: A system to manage ML models.

ModelDB is an end-to-end system to manage machine learning models.

## Setup

### Configuration

## Running ModelDB

### Server

### Command-line utility

### Python client

### Scala client

### Frontend

## Code layout:

- thrift/   Contains thrift files
- server/   ModelDB server + storage
- client/
  - shell/  Command-line utility for running ModelDB
  - python/ Basic python client
    - libs/sklearn/  Native client for scikit-learn
  - scala/  Basic scala client
    - libs/spark.ml Native client for spark.ml
- frontend/ Web-based frontend for ModelDB
- config/   Configuration dir
