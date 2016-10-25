# ModelDB: A system to manage ML models.

ModelDB is an end-to-end system to manage machine learning models.

## Setup

ModelDB has various dependencies. See [Required Software](docs/RequiredSoftware.md)

### Configuration
A Project must be defined in the config. It contains a name and description.

Multiple experiments can de defined in the config and the right one can be selected at the command line using its name.
If no experiment is specified, a default experiment is used automatically.

EXPT_DIR: the dir that has the experiment code you'd like to version
GIT_REPO_DIR: the dir where the experiment code will be copied. This is the git repo dir

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
