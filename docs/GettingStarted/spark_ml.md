# Getting Started with ModelDB on spark.ml

## 1. Clone the repo

```git
git clone https://github.com/mitdbg/modeldb
```

## 2. Install dependencies
We assume that you have Java 1.8+ and Spark 2.0 installed.

_Note: ModelDB has been tested with Spark 2.0. It may not be compatible with subsequent versions._

On Mac OSX:

```bash
brew install sqlite
brew install thrift
brew install maven
brew install sbt
brew install node

```

On Linux:

```bash
apt-get update
sudo apt-get install sqlite
sudo apt-get install maven
sudo apt-get install sbt
sudo apt-get install nodejs # may need to symlink node to nodejs. "cd /usr/bin; ln nodejs node"

# install thrift. path_to_thrift is the installation directory
cd path_to_thrift
wget
http://mirror.cc.columbia.edu/pub/software/apache/thrift/0.9.3/thrift-0.9.3.tar.gz
tar -xvzf thrift-0.9.3.tar.gz
cd thrift-0.9.3
./configure
make
export PATH=path_to_thrift/:$PATH
```

## 3. Build

ModelDB is composed of three components: the ModelDB server, the ModelDB client libraries, and the ModelDB frontend.

In the following, **path_to_modeldb** refers to the directory into which you have cloned the modeldb repo.

```bash
# build and start the server
cd path_to_modeldb/server
./codegen/gen_sqlite.sh
./start_server &

# build spark.ml client library
cd path_to_modeldb/client/scala/libs/spark.ml
./build_client.sh

# start the frontend
cd path_to_modeldb/frontend
./start_frontend.sh &

```

## 4. Incorporate ModelDB into an ML workflow

First, import the ModelDB client library classes.

```scala
import edu.mit.csail.db.ml.modeldb.client._
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

```

Create a ModelDB syncer to track models and operations. You can provide ModelDB arguments through a config file or provide them in code.

See more about ModelDB project organization [here]().

```scala
// config file example
ModelDbSyncer.setSyncer(new ModelDBSyncer(SyncerConfig(path_to_config)))
```
OR
```scala
// explicitly setting up the syncer object
ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "compare models", // project name
        "your name", // user name
        "we use the UCI Adult Census dataset to compare random forests, " // project description
          + "decision trees, and logistic regression"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun
      )
    )

```

Next use the ModelDB **sync** functions in your code. For example:

```Java
val data = preprocessingPipeline
      .fitSync(rawData)
      .transformSync(rawData)

val predictions = models.map(_.transformSync(testing))

lrModel.saveSync("imdb_simple_lr")
```

For logging metrics, use the ModelDB metrics class (SyncableMetrics) or use the spark Evaluator classes with the evaluate*Sync* method. These are thin wrappers around the spark.ml classes.

```scala
val metric = SyncableMetrics.ComputeMulticlassMetrics(model, predictions, labelCol, predictionCol)

```
OR
```scala
val model = ...
val evaluator = new MulticlassClassificationEvaluator()
  .setMetricName(...)
val metric = evaluator.evaluateSync(predictions, model)
```
<!-- At the end of your workflow, be sure to sync all the data with ModelDB.
```scala
 ModelDbSyncer.sync()
```
-->
_Run your program._

Be sure to link the client library built above to your code (e.g. by adding to your classpath).

## 5. Explore models
That's it! Explore the models you built in your workflow at [http://localhost:3000](http://localhost:3000).
