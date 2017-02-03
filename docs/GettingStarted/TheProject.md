# The Project

For this tutorial, we'll be using ModelDB and spark.ml to compare different machine learning models for the same dataset.
We will use spark.ml to model the dataset using Decision Trees, Random Forest, and logistic regression. We will use ModelDB 
syncables to track what operations we perform and annotate our work. 

We will use the [Adult]("https://archive.ics.uci.edu/ml/datasets/Adult") data set from the UCI Census.
Our model should predict whether the income of a given adult exceeds $50000/year.

You can see the full code for this project in the samples directory [here].

## Step 0: Clone the repo

Before anything, you'll need to clone the modeldb repo.

```git 
git clone https://github.com/mitdbg/modeldb
```

## Step 1: Install dependencies

The first step is to get ModelDB on your system, as well as its dependencies. In this tutorial, because our workflow is in Apache Spark,
we will use the spark.ml ModelDB client, the ModelDB server, and the frontend. In addition to this, we will need various other software packages
in order for ModelDB to run.

You will need the following packages:

*Server*
* [SQLite](http://sqlite.org/) (3.15.1): To store the models
* [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (1.8): To run the server
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the different clients
* [Maven](http://maven.apache.org/download.cgi) (3.3.9): To build the project

*Client*
* [SBT](http://www.scala-sbt.org/) (0.13.12): In order to build the Scala project (comes with Scala)
* [Apache Thrift](http://thrift.apache.org/) (0.9.3): To interface with the server
* [Apache Spark](https://spark.apache.org/downloads.html) (2.0.0 - NOT LATEST): To train and run machine learning models

*Frontend*
* [node.js]("https://nodejs.org/en/"): In order to run the front end

For Linux (You'll still have to add the binaries in mdbDependencies to your PATH):
```bash
apt-get update
sudo apt-get install sqlite
sudo apt-get install maven
sudo apt-get install nodejs
cd ~
mkdir mdbDependencies
wget http://apache.mesi.com.ar/thrift/0.9.3/thrift-0.9.3.tar.gz
wget http://d3kbcqa49mib13.cloudfront.net/spark-2.0.1-bin-hadoop2.7.tgz
wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
tar -xvzf thrift-0.9.3.tar.gz
tar -xvzf spark-2.0.1-bin-hadoop2.7.tgz
tar -xvzf sbt-0.13.13.tgz
cd thrift-0.9.3
./configure
make
cd ..


cd /usr/bin
ln nodejs node
```

**NOTE**: On some systems, nodejs will install itself as `nodejs`, on others, it will install as `node`.
ModelDB searches for the filename `node`. Therefore, you should go to the binary directory in which you install node.js,
and if you have `nodejs`, then make a symbolic link named `node` that points to it. *If you do not do this, the frontend
will not run*

Once all the dependencies are downloaded, make sure that the bin/ directory of each is in your PATH variable.

## Step 2: Build the project

Now that you have the dependencies for modeldb, and presumably the repository, you'll need to build it.
This is mostly automated through `sh` scripts. These instructions assume you are in the `modeldb` directory.

*Server* (from `modeldb`)
```bash
cd server
cd codegen
./gen_sqlite.sh
cd ..
./start_server &
```

*Client* (from `modeldb`)
```bash
cd client
cd scala/libs/spark.ml
./build_client.sh
```

*Frontend* (from `modeldb`)
```bash
cd frontend
./start_frontend.sh &
```

Building the client will create a jar located at 

**target/scala-2.11/ml.jar**


## Step 3: Create a Project

Create a new project, and import the ModelDB spark.ml 
client (Found in `target/scala-2.11/ml.jar`). 

## Interlude 3.5: ModelDB Project Organization

Before we begin writing code, let's take a look at how you can organize your work in ModelDB. 

The top level of organization in ModelDB is a `Project`. These are intended to contain work
relating to a single goal.

A Project can contain many `Experiments`. Experiments are intended to contain
many runs that relate to a single approach.

Finally, an `Experiment` can contain many `ExperimentRuns`. These are intended to contain information regarding
a single run of your program.

For this project, we will not be concerned with the organization of our project,
and make multiple `ExperimentRuns` in the `DefaultExperiment` of the `Project`.

## Step 4: Import Necessary Libraries
For this project, we'll need to import the following libraries:

```scala
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
```

## Step 5: Make your program in Spark

For this tutorial, we assume you know how to use Spark.ml, and therefore can take advantage of the full features of ModelDB.

## Step 6: Create a Syncer

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

## Step 7: Call the syncer extension methods when performing operations

Now that we have a syncer, ModelDB will add numerous extension methods onto the pre-existing spark.ml 
functions that you are used to. All you have to do is add "Sync" to the end of the method name.
Some examples:

```scala
val data = preprocessingPipeline
      .fitSync(rawData)
      .transformSync(rawData)

val predictions = models.map(_.transformSync(testing))
```

## Step 8 (Optional): Annotate your work

ModelDB also provides the ability to annotate your work as your program runs. To add an annotation,
just call the `annotate` method on your syncer. This will add an annotation, and remember
when you annotated it (e.g. before comparing multiple models). The `annotate` method can take
multiple arguments of varying types,
including data frames and other machine learning primitives, much like a print method.

```scala
ModelDbSyncer.annotate("I'm going to compare", dt, rf, " and ", lr)
```

## Step 9 (Optional): Compute metrics about your models

ModelDB is also packaged with the ability to compute metrics on how well your models perform.
See the documentation for more details on what metrics it can compute, but for this example, we'll
use `ComputeMulticlassMetrics`.

```scala
val metrics = (models zip predictions).map { case (model, prediction) =>
      SyncableMetrics.ComputeMulticlassMetrics(model, prediction, labelCol, predictionCol)
    }
```

This will compute <INSERT HERE>, and add this to your database.

## Step 10: Running the ModelDB Server
Okay - now you have a spark program, which accesses the ModelDB client. Make sure that the server
is still running from Step 2.

## Step 11: Run the program

Run your program as you would normally, using spark-submit (example given for the `CompareModelsSample` in the samples directory):

```bash
spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.CompareModelsSample" target/scala-2.11/ml.jar <path_to_adult.data>
```

## Step 12: Start Front-end
Great! Now you've ran your program, and behind the scenes, the ModelDB client has recorded the ML events, your annotations, and your metrics
in the ModelDB server with little overhead. However, having data in SQLite tables isn't very user-friendly, is it?

So, we'll use the ModelDB front-end to view the data in an intuitive way. The ModelDB front-end is written in node.js, and is run like any other node application.
Make sure you have the frontend started from Step 2.

## Step 13: View the results

Now, you can view the modeldb frontend from `localhost:3000`.

And that's all! Now, with minimal changes, your spark.ml program uses the ModelDB client, and through syncers, it reports the ML events
to the ModelDB server. The server is storing your data, and will persist even when it is restarted. You can then view and organize your projects,
the annotations you made, and the metrics that ModelDB calculated through the GUI of the front-end. Happy Coding! <REMOVE?>

