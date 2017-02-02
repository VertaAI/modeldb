# The Project

For this tutorial, we'll be using ModelDB and spark.ml to compare different machine learning models for the same dataset.
We will use spark.ml to model the dataset using Decision Trees, Random Forest, and logistic regression. We will use ModelDB 
syncables to track what operations we perform and annotate our work. 

We will use the [Adult]("https://archive.ics.uci.edu/ml/datasets/Adult") data set from the UCI Census.
Our model should predict whether the income of a given adult exceeds $50000/year.

You can see the full code for this project in the samples directory [here].

## Step 1: Downloading ModelDB and its Dependencies

The first step is to get ModelDB on your system, as well as its dependencies. In this tutorial, because our workflow is in Apache Spark,
we will use the spark.ml ModelDB client, the ModelDB server, and the frontend. In addition to this, we will need various other software packages
in order for ModelDB to run. Please see [Required Software]("../RequiredSoftware.md") for instructions on how to download the
dependencies. Then, refer to [Running the Client and Server]("../RunningTheClientAndServer.md") for instructions on how to run the server, and package
the client into a JAR file.

## Step 2: Create a Project

Create a new project, and import the ModelDB spark.ml 
client (Found in `target/scala-2.11/ml.jar`). 

## Interlude 2.5: ModelDB Project Organization

Before we begin writing code, let's take a look at how you can organize your work in ModelDB. 

The top level of organization in ModelDB is a `Project`. These are intended to contain work
relating to a single goal.

A Project can contain many `Experiments`. Experiments are intended to contain
many runs that relate to a single approach.

Finally, an `Experiment` can contain many `ExperimentRuns`. These are intended to contain information regarding
a single run of your program.

For this project, we will not be concerned with the organization of our project,
and make multiple `ExperimentRuns` in the `DefaultExperiment` of the `Project`.

## Step 3: Import Necessary Libraries
For this project, we'll need to import the following libraries:

```scala
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
```

## Step 4: Make your program in Spark

For this tutorial, we assume you know how to use Spark.ml, and therefore can take advantage of the full features of ModelDB.

## Step 5: Create a Syncer

ModelDB is very easy to integrate into a Spark program. First, you make a syncer at the beginning of your program:

```scala
ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "compare models",
        "your name",
        "we use the UCI Adult Census dataset to compare random forests, "
          + "decision trees, and logistic regression"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun
      )
    )
```

We can see here that this syncer is initialized with some basic configuration options -
we create a project named "compare models", and set the author and description of the project,
or, if it already exists, we note that we are adding to that project.
Next, we set the syncer to load data into the "Default Experiment" - this is an experiment located
in every project if you do not want the organization that experiments provide.
Finally, we set the config to create a new experiment run every time our program is run.

And that's all! Now we can integrate ModelDB into our current code. ModelDB is accessed by adding
extension methods on many spark.ml methods. Once the syncer is created, as you perform operations,
ModelDB will automatically save them for you before computing them.

## Step 6: Call the syncer extension methods when performing operations

Now that we have a syncer, ModelDB will add numerous extension methods onto the pre-existing spark.ml 
functions that you are used to. All you have to do is add "Sync" to the end of the method name.
Some examples:

```scala
val data = preprocessingPipeline
      .fitSync(rawData)
      .transformSync(rawData)

val predictions = models.map(_.transformSync(testing))
```

## Step 7 (Optional): Annotate your work

ModelDB also provides the ability to annotate your work as your program runs. To add an annotation,
just call the `annotate` method on your syncer. This will add an annotation, and remember
when you annotated it (e.g. before comparing multiple models). The `annotate` method can take
multiple arguments of varying types,
including data frames and other machine learning primitives, much like a print method.

```scala
ModelDbSyncer.annotate("I'm going to compare", dt, rf, " and ", lr)
```

## Step 8 (Optional): Compute metrics about your models

ModelDB is also packaged with the ability to compute metrics on how well your models perform.
See the documentation for more details on what metrics it can compute, but for this example, we'll
use `ComputeMulticlassMetrics`.

```scala
val metrics = (models zip predictions).map { case (model, prediction) =>
      SyncableMetrics.ComputeMulticlassMetrics(model, prediction, labelCol, predictionCol)
    }
```

This will compute <INSERT HERE>, and add this to your database.

## Step 9: Running the ModelDB Server

Okay - now you have a spark program, which accesses the ModelDB client. Now you want to run your 
program.

Before we run the program, however, we need to start up the ModelDB server, so that it can collect and save the data
that the client provides.

Change to the `modeldb/server` directory. From there, run `codegen/gen_sqlite.sh` to generate the SQLite tables that ModelDB uses,
and then start the server using `start_server.sh`. You will most likely want to start the server as a background process.
So, run the following (from the `modeldb` directory):

```bash
cd server
cd codegen
./gen_sqlite.sh
cd ..
start_server.sh &
```

## Step 10: Run the program

Run your program as you would normally, using spark-submit (example given for the `CompareModelsSample` in the samples directory):

```bash
spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.CompareModelsSample" target/scala-2.11/ml.jar <path_to_adult.data>
```

## Step 11: Start Front-end

Great! Now you've ran your program, and behind the scenes, the ModelDB client has recorded the ML events, your annotations, and your metrics
in the ModelDB server with little overhead. However, having data in SQLite tables isn't very user-friendly, is it?

So, we'll use the ModelDB front-end to view the data in an intuitive way. The ModelDB front-end is written in node.js, and is run like any other node application.
Change to the `modeldb` directory again. From there, go to `frontend`. Then, simply run `start_frontend.sh`, which will generate the necessary 
files that it needs, install all dependencies from npm, and then start the front-end.

## Step 12: View the results

Now, you can view the modeldb frontend from `localhost:3000`.

And that's all! Now, with minimal changes, your spark.ml program uses the ModelDB client, and through syncers, it reports the ML events
to the ModelDB server. The server is storing your data, and will persist even when it is restarted. You can then view and organize your projects,
the annotations you made, and the metrics that ModelDB calculated through the GUI of the front-end. Happy Coding! <REMOVE?>

