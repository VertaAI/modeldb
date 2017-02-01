# The Project

For this tutorial, we'll be using ModelDB and spark.ml to compare different machine learning models for the same dataset.
We will use spark.ml to model the dataset using Decision Trees, Random Forest, and logistic regression. We will use ModelDB 
syncables to track what operations we perform and annotate our work. 

For simplicity's sake, we will use IntelliJ IDEA (Community Edition) to create our project.

We will use the [Adult]("https://archive.ics.uci.edu/ml/datasets/Adult") data set from the UCI Census.
Our model should predict whether the income of a given adult exceeds $50000/year.

You can see the full code for this project in the samples directory.

## Step 1: Downloading ModelDB and its Dependencies

The first step is to get ModelDB on your system, as well as its dependencies. In this tutorial, because our workflow is in Apache Spark,
we will use the spark.ml ModelDB client, the ModelDB server, and the frontend. In addition to this, we will need various other software packages
in order for ModelDB to run. Please see [Required Software]("../RequiredSoftware.md") for instructions on how to download the
dependencies. Then, refer to [Running the Client and Server]("../RunningTheClientAndServer.md") for instructions on how to run the server, and package
the client into a JAR file.

## Step 2: Create a Project

Create a new project in IntelliJ, and use the import wizard to import the spark.ml, and the ModelDB spark.ml 
client (Found in `target/scala-2.11/ml.jar`). 

## Interlude 2.5: ModelDB Project Organization

Before we begin writing code, let's take a look at how you can organize your work in ModelDB. 

The top level of organization in ModelDB is a `Project`. These are intended to contain work
relating to a single goal.

A Project can contain many `Experiments`. Experiments are intended to contain
many runs that relate to a single approach.

Finally, an `Experiment` can contain many `ExperimentRuns`. These are intended to contain information regarding
a single model.

For this project, we will not be concerned with the organization of our project,
and make multiple `ExperimentRuns` in an `Experiment`.

## Step 3: Import Necessary Libraries
For this project, we'll need to import the following libraries:

```scala
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{DecisionTreeClassifier, LogisticRegression, RandomForestClassifier}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
```

## Step 4: Make a case class for your data
Next, we'll make a class to represent a single adult. 

```scala

case class  Person(age: Integer, workclass: String, fnlwgt: Integer, education: String, education_num: Integer,
                  marital_status: String, occupation: String, relationship: String, race: String, sex: String,
                  capital_gain: Integer, capital_loss: Integer, hours_per_week: Integer, native_country: String,
                  income_level: Double)

```

We will use this class to create a DataFrame (a table of data) from the file.

## Step 5: Making the main class

Now that we can create a row in a DataFrame, let's start with our main class.

Begin by making an object:

```scala
    object CompareModelsSample {
        def main(args: Array[String]): Unit = {
        
        
        }
    }
```

## Step 6: Make a syncer

ModelDB uses syncers to record the operations that you perform in spark.ml. By using syncers,
you will be able to store your models without significant changes to your codebase. 
When setting your syncer, you'll also need to configure what project and experiment run this model
will be in.

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
You can see here that we:
1. Initialize a syncer
2. Set the project to "Compare Models". If this project does not exist, it is created.
3. Set the author and description of the project
4. Sets the experiment to the default experiment - in essence, this project is not divided into 
experiments, so we use the default experiment.
5. Creates a new Experiment Run for this syncer to put its data in

So, you can see that if we run this program multiple times, we will have a project named "Compare Models",
with many Experiment Runs, each containing the results of a single run of this program.

## Step 7: Setting up contexts and reading the data file

Next, we need to find the Adult Data Set. We will have the user pass in the path to the data file
as a command line argument. 

```scala
val pathToDataFile = if (args.length < 1) {
      throw new IllegalArgumentException("Missing path to data file positional argument")
    } else {
      args(0)
    }
```

Now that we have the data file (and print an error message if they do not pass the file),
let's set up our contexts in spark.

```scala
// Set up contexts.
    val conf = new SparkConf().setAppName("Census")
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder()
      .appName("Cross Validator Sample")
      .getOrCreate()
    import spark.implicits._
```

Finally, let's use our countext to read in the data file:

```scala
val rawCsv = sc.textFile(pathToDataFile)
```

This will read the context 

## Step 8: Parse the CSV file
In the next step, we will convert each line of the CSV file into a Person object.

```scala
val rows = rawCsv
      .filter(_.length > 5) // Remove empty lines. The choice of 5 is arbitrary.
      .map { (line) =>
      // Split the line on the commas.
      // Then, partition it into Array(Array(feature1, feature2, ..., feature14), Array(label)).
      val splitted = line.split(",").splitAt(14)

      // Figure out what the label is, then assign 0.0 to represent an income less than $50K and 1.0 to represent an income
      // above $50K.
      val labelRaw = splitted._2(0).trim
      val label = if (labelRaw == "<=50K") 0.0
      else if (labelRaw == ">50K") 1.0
      else throw new IllegalArgumentException("Invalid Label " + labelRaw)

      // Now, trim each of the features.
      val f = splitted._1.map(_.trim)

      // Create the person object (and convert features to integers where appropriate).
      new Person(
        f(0).toInt,
        f(1),
        f(2).toInt,
        f(3),
        f(4).toInt,
        f(5),
        f(6),
        f(7),
        f(8),
        f(9),
        f(10).toInt,
        f(11).toInt,
        f(12).toInt,
        f(13),
        label
      )
    }
```

This looks intimidating - let's take it step by step:

1. Firstly, we have the declaration and initialization of the "rows" variable. `val rows = `
2. Next, we remove empty lines in our CSV file by using the filter function. The filter function
takes each line, and checks that it satisfies the condition `_.length > 5`. This is to remove lines
that do not actually contain data.
3. Next we use the map function to apply a function to each line that remains, and generate a `Person` object
for each one. The function that applies this change is a lambda function, with one parameter - line, that will
be replaced with the element currently being mapped. 
4. In our mapping function, we first split the line by commas. This generates 15 fields. 
We then further split this array at 14, because the dataset contains 14 features, and 1 label field. (Our model is supposed
to predict the label). This second split results in an array, where the first element is an array of the values of 14 features, and the second element 
being a label which we have to predict. In this data, the label is ">50K" or "<=50K", because 
our model should be able to use the features to identify if a given adult has an income greater or less
than $50K.
5. We then access the 0th element of the array - the raw label ("<=50K" or ">50K"). Because we would like a numerical label,
we then change this into 1.0 for incomes > 50K, and 0.0 for incomes <= 50K. If any label does not fall under
these two categories, we throw an exception to inform the user of their bad data.
6. Now that we have the label, we would like to parse all the features. We first use the `map` function
to pass each feature to `trim`, which removes leading and trailing whitespace. Next, we create a `Person` object
from each of the elements, parsing the strings to integers where necessary for this data set.
7. Because this is the last line of the lambda function, this Person is returned.

## Step 9: Creating a Data Frame
We now have an array of 'Person' objects. Lets convert this to a DataFrame so that we can 
create and apply ML models to it.

```scala
val rawData = rows.toDF()
```

## Step 10: 
















