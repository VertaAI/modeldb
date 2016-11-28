# Introduction

This is the [Spark ML](http://spark.apache.org/docs/latest/ml-guide.html) client
for ModelDB. 

This library is responsible for storing machine learning operations in Spark ML,
like `estimator.fit(dataframe)`, in ModelDB.

# Usage

To build the JAR, first make sure you have installed 
sbt (see [dependencies](../../../../docs/RequiredSoftware.md)). Then, from the spark.ml dir, run:

```
./build_client.sh
```

This will create the JAR `target/scala-2.11/ml.jar`.

# Samples

This project includes samples [here](src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/evaluation) and [here](src/main/scala-2.11/edu/mit/csail/db/ml/modeldb/sample) demonstrating usage of the library.

First, you'll need to import the classes you need, for example:

```
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewProject, SyncableMetrics}
```

The `ModelDbSyncer` is the class that is responsible for syncing machine 
learning operations to ModelDB. 

First, we'll create a `ModelDbSyncer`:

```
ModelDbSyncer.setSyncer(
    new ModelDbSyncer(projectConfig = NewProject(
        "pipeline",
        "harihar",
        "this example creates and runs a pipeline"
    ))
)
```

Now, when you want to log an operation to ModelDB, you append `Sync` to the
method call. For example,

```
myModel.transformSync(myDataFrame)
myEstimator.fitSync(myDataFrame)
myDataFrame.randomSplitSync(Array(0.7, 0.3))
```

You can also take advantage of ModelDB specific operations. For example, to
tag an object with a description, you can do:

```
myModel.tag("Some tag")
myDataFrame.tag("Some tag")
myEstimator.tag("Some tag")
```

You can also create annotations, which are short messages associated with 
Spark ML objects:

```
ModelDbSyncer.annotate("It seems", myDataFrame, "has a lot of missing entries")
```

To evaluate a model, you can do:

```
val metrics = SyncableMetrics.ComputeMulticlassMetrics(
    model,
    transformedDataFrame,
    labelColName,
    predictionColName
)
```
