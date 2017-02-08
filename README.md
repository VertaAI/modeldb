# ModelDB: A system to manage ML models.

## News
2017.02.08: ModelDB publicly available! Try it out and contribute.

## Setup

To setup ModelDB, follow these steps:

1. ModelDB has various dependencies. See [Required Software](docs/required_software.md). Install the relevant ones.
2. Configure and start the [server](server).
3. Build modeldb clients as described in for [spark.ml](client/scala/libs/spark.ml) and [scikit-learn](client/python).
4. Update your ML code to use the client libraries to log workflows and models to ModelDB, as in [here](client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample), [here](client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/evaluation), and [here](client/python/samples).
5. Run your ML code normally.
6. Start the ModelDB [frontend](frontend) and explore models in ModelDB.
