# ModelDB: A system to manage ML models.

ModelDB is an end-to-end system to manage machine learning models.

## Project Overview

ModelDB is made up of three main parts: the ModelDB server, the ModelDB clients, and the ModelDB frontend. 

### ModelDB server 
The [ModelDB server](server) stores all the data for ModelDB and exposes a thrift API for storing as well as querying data.

### ModelDB client
ModelDB clients are native libraries in various languages that can be used to log data to ModelDB. We currently provide clients for [spark.ml](client/scala/libs/spark.ml) and [scikit-learn](client/python).

Sample machine learning workflows using these client libraries are available [here](client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) and [here](client/python/samples).

### ModelDB Frontend

Once machine learning workflows and models have been logged to ModelDB, they can be explored via the web-based ModelDB [frontend](frontend).

## Setup

To setup ModelDB, follow these steps:

1. ModelDB has various dependencies. See [Required Software](docs/RequiredSoftware.md). Install the relevant ones.
2. Configure and start the [server](server).
3. Build modeldb clients as described in for [spark.ml](client/scala/libs/spark.ml) and [scikit-learn](client/python).
4. Update your ML code to use the client libraries to log workflows and models to ModelDB, as in [here](client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) and [here](client/python/samples).
5. Run your ML code normally.
6. Start the ModelDB [frontend](frontend) and explore models in ModelDB.
