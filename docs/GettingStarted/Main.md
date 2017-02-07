#Getting Started with ModelDB

## About ModelDB

ModelDB is an end-to-end system to manage machine learning models. It consists of a backend server that stores the models,
a front-end that allows you to view the current models, and a client that integrates into your machine learning code so that you 
can record what your models are doing as you perform ML operations.

## Installation and Setup

Please see the [Required Software](../RequiredSoftware.md) section for information on ModelDB's dependencies.
Then, follow the instructions in [RunningTheClientAndServer.md](../RunningTheClientAndServer.md) to set up your ModelDB
environment.

*Note: This getting started guide will assume you are using the ModelDB spark.ml client*

## Creating a project

ModelDB is integrated into your machine learning projects via the client libraries. For this tutorial, we will use
the spark.ml client, and write our project in Scala.

*Note: We will be making this [CrossValidatorSample](https://github.com/mitdbg/modeldb/blob/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample/CrossValidatorSample.scala)
which can also be found [locally](../../client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample/CrossValidatorSample.scala)*

