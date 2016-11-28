# ModelDB: A system to manage ML models.

ModelDB is an end-to-end system to manage machine learning models.

## Project Overview

ModelDB is made up of three main parts: the ModelDB server, the ModelDB clients, and the ModelDB frontend. 

### ModelDB server 
The [ModelDB server](https://github.com/mitdbg/modeldb/tree/master/server) stores all the data for ModelDB and exposes a thrift API for storing as well as querying data.

### ModelDB client
ModelDB clients are native libraries in various languages that can be used to log data to ModelDB. We currently provide clients for [spark.ml](https://github.com/mitdbg/modeldb/tree/master/client/scala/libs/spark.ml) and [scikit-learn](https://github.com/mitdbg/modeldb/tree/master/client/python).

Sample machine learning workflows using these client libraries are available [here](https://github.com/mitdbg/modeldb/tree/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) and [here](https://github.com/mitdbg/modeldb/tree/master/client/python/samples).

### ModelDB Frontend

Once machine learning workflows and models have been logged to ModelDB, they can be explored via the web-based ModelDB [frontend](https://github.com/mitdbg/modeldb/tree/master/frontend).

## Setup

To setup ModelDB, follow these steps:

1. ModelDB has various dependencies. See [Required Software](docs/RequiredSoftware.md). Install the relevant ones
2. Configure and start the [server](https://github.com/mitdbg/modeldb/tree/master/server)
3. Use the client libraries to log workflows and models to ModelDB, as in [here](https://github.com/mitdbg/modeldb/tree/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) and [here](https://github.com/mitdbg/modeldb/tree/master/client/python/samples).
4. Start the ModelDB [frontend](https://github.com/mitdbg/modeldb/tree/master/frontend) and explore models in ModelDB.
