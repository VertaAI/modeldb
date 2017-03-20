# Getting Started with ModelDB on spark.ml

## 1. Setup

First, make sure you have followed the [setup instructions for ModelDB](../../README.md#setup-and-installation) and have built the client.

## 2. Incorporate ModelDB into an ML workflow

#### a. Import the ModelDB client library classes

```scala
import edu.mit.csail.db.ml.modeldb.client._
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

```

#### b. Create a ModelDB syncer
ModelDBSyncer is the object that logs models and operations to the ModelDB backend. You can initialize the syncer either from a config file (e.g. [modeldb/client/syncer.json](https://github.com/mitdbg/modeldb/blob/master/client/syncer.json)) or explicitly via arguments.

```scala
// initialize syncer from config file
ModelDbSyncer.setSyncer(new ModelDbSyncer(SyncerConfig(path_to_config)))
```
OR
```scala
// initialize syncer explicitly
ModelDbSyncer.setSyncer(
      // what project are you working on
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "Demo", // project name
        "modeldbuser", // user name
        "Project to hold all models from the demo" // project description
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun
      )
    )

```

#### c. Log models and pre-processing operations
Next, when you want to log an operation to ModelDB, use the ModelDB **sync** variants of functions. So the original _fit_ calls from spark.ml would turn into **fitSync**, _save_ calls would turn into **saveSync** and so on.

```scala
val logReg = new LogisticRegression()
val logRegModel = logReg.fitSync(trainDf)
val predictions = logRegModel.transformSync(test)

logRegModel.saveSync("simple_lr")

```

#### d. Log metrics
Use the ModelDB metrics class (**SyncableMetrics**) or use the spark Evaluator classes with the **evaluateSync** method.

```scala
val metrics = SyncableMetrics.ComputeMulticlassMetrics(lrModel, predictions, labelCol, predictionCol)

```
OR
```scala
val evaluator = new BinaryClassificationEvaluator()
val metric = evaluator.evaluateSync(predictions, logRegModel)
```
<!-- At the end of your workflow, be sure to sync all the data with ModelDB.
```scala
 ModelDbSyncer.sync()
```
-->

**The full code for this example can be found [here](https://github.com/mitdbg/modeldb/blob/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample/SimpleSample.scala).**

#### e. _Run your model!_
Be sure to link the client library built above to your code (e.g. by adding to your classpath).

## 3. Explore models
That's it! Explore the models you built in your workflow at [http://localhost:3000](http://localhost:3000).

<img src="images/frontend-1.png">

**More complex spark.ml workflows using ModelDB are located [here](https://github.com/mitdbg/modeldb/tree/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) and [here](https://github.com/mitdbg/modeldb/tree/master/client/scala/libs/spark.ml/src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/evaluation).**
