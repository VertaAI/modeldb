# Getting Started with ModelDB on spark.ml

## 1. Clone the repo

```git
git clone https://github.com/mitdbg/modeldb
```

## 2. Install dependencies
We assume that you have Java 1.8+ installed. We also assume that you have Spark 2.0 installed.

_Note: ModelDB has been tested with Spark 2.0. It may not be compatible with subsequent versions._

On Mac OSX:

```bash
brew install sqlite
brew install thrift
brew install maven
brew install sbt
brew install node

```


## 3. Build

ModelDB is composed of three components: the ModelDB server, the ModelDB client libraries, and the ModelDB frontend.

In the following, **modeldb_dir** refers to the directory into which you have cloned the modeldb repo.

```bash
# build and start the server
cd modeldb_dir/server
./codegen/gen_sqlite.sh
./start_server &

# build spark.ml client library
cd modeldb_dir/client/scala/libs/spark.ml
./build_client.sh

# start the frontend
cd modeldb_dir/frontend
./start_frontend.sh &

```

## 4. Incorporate ModelDB into an ML workflow

First, import the ModelDB client library classes.

```scala
import edu.mit.csail.db.ml.modeldb.client._
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

```
Next use the ModelDB **sync** functions in your code. For example:

```scala
val data = preprocessingPipeline
      .fitSync(rawData)
      .transformSync(rawData)

val predictions = models.map(_.transformSync(testing))
```

For logging metrics, use the ModelDB metric classes. These are thin wrappers around the spark.ml classes.
_TODO: Simplify code below_

```scala
val metrics = (models zip predictions).map { case (model, prediction) =>
      SyncableMetrics.ComputeMulticlassMetrics(model, prediction, labelCol, predictionCol)
    }
```

At the end of your workflow, be sure to sync all the data with ModelDB.
```scala
ModelDbSyncer.sync()
```
_Run your program._

Be sure to link the client library built above to your code (e.g. by adding to your classpath).

## 5. Explore models!
That's it! Explore the models you built in your workflow at http://localhost:3000.
