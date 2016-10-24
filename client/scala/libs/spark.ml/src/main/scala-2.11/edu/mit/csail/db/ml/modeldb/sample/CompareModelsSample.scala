package edu.mit.csail.db.ml.modeldb.sample

// Import the implicit classes for ModelDb.
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{DecisionTreeClassifier, LogisticRegression, RandomForestClassifier}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}


// Create a case class which represents a person (i.e. one row of the data file).
// Note, it is VERY important that we define this outside of the main function. Otherwise, we will get an error.
// In this particular case, the sbt assembly complains that the toDF function does not exist.
case class  Person(age: Integer, workclass: String, fnlwgt: Integer, education: String, education_num: Integer,
                  marital_status: String, occupation: String, relationship: String, race: String, sex: String,
                  capital_gain: Integer, capital_loss: Integer, hours_per_week: Integer, native_country: String,
                  income_level: Double)

/**
  * This program will train and compare models on the UCI Census Adult dataset.
  *
  * To run this program, do the following:
  *
  * Build:
  * sbt clean && sbt assembly
  *
  * Run:
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.CompareModelsSample" target/scala-2.11/ml.jar
  *   <path_to_adult.data>
  */
object CompareModelsSample {
  def main(args: Array[String]): Unit = {
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "compare models",
        "harihar",
        "we use the UCI Adult Census dataset to compare random forests, "
          + "decision trees, and logistic regression"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun
      )
    )

    // Read the positional argument (i.e. the path to the data file).
    val pathToDataFile = if (args.length < 1) {
      throw new IllegalArgumentException("Missing path to data file positional argument")
    } else {
      args(0)
    }

    // Set up contexts.
    val conf = new SparkConf().setAppName("Census")
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder()
      .appName("Cross Validator Sample")
      .getOrCreate()
    import spark.implicits._

    // Read the data file.
    val rawCsv = sc.textFile(pathToDataFile)

    // Create an RDD[Person] from the data.
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
    val rawData = rows.toDF()
    // Now let's process the features into something a model can use.
    // To do that, let's first see which features are strings and which are not (we ignore "income_level" because it's
    // the label).
    val (stringFields, nonStringFields) = rawData.schema.partition{ (field) =>
      field.dataType == org.apache.spark.sql.types.StringType
    }
    val stringFieldNames = stringFields.map(_.name)
    val nonStringFieldNames = nonStringFields.map(_.name).filter(_ != "income_level")

    // Index categorical variables.
    val indexers = stringFieldNames.map { (field) =>
      new StringIndexer()
        .setInputCol(field)
        .setOutputCol(field + "_index")
    }

    // One-hot encode categorical variables.
    val encoders = stringFieldNames.map { (field) =>
      new OneHotEncoder()
        .setInputCol(field + "_index")
        .setOutputCol(field + "_vec")
    }

    // Now let's combine our features into a "features" vector.
    val assembler = new VectorAssembler()
      .setInputCols((stringFieldNames.map(_ + "_vec") ++ nonStringFieldNames).toArray)
      .setOutputCol("features")

    // One final pre-processing step: Let's use a StringIndexer on the income level.
    // NOTE: I don't actually think this is necessary.
    val labelIndexer = new StringIndexer()
      .setInputCol("income_level")
      .setOutputCol("income_level_index")

    // Create a pipeline for preprocessing.
    val preprocessingPipeline = new Pipeline()
      .setStages((indexers ++ encoders ++ Seq(assembler, labelIndexer)).toArray)

    // Generate the preprocessed data.
    val data = preprocessingPipeline
      .fitSync(rawData)
      .transformSync(rawData)

    // Training/testing split.
    val Array(training, testing) = data.randomSplit(Array(0.7, 0.3))

    // Column names.
    val labelCol = "income_level_index"
    val featuresCol = "features"
    val predictionCol = "prediction"

    // We'll compare three models - Random Forest, LogisticRegression, and Decision Tree.
    val dt = new DecisionTreeClassifier()
      .setLabelCol(labelCol)
      .setFeaturesCol(featuresCol)
    val rf = new RandomForestClassifier()
      .setNumTrees(100)
      .setLabelCol(labelCol)
      .setFeaturesCol(featuresCol)
    val lr = new LogisticRegression()
      .setLabelCol(labelCol)
      .setFeaturesCol(featuresCol)
      .setMaxIter(100)
      .setElasticNetParam(0.5)

    ModelDbSyncer.annotate("I'm going to compare", dt, rf, " and ", lr)

    val estimators = Array(dt, lr, rf)

    // Make the evaluator.
    val eval = new BinaryClassificationEvaluator()
      .setLabelCol(labelCol)

    // Create search grids.
    val dtGrid = new ParamGridBuilder()
      .addGrid(dt.impurity, Array("gini", "entropy"))
      .addGrid(dt.maxDepth, Array(3, 5, 7))

    val rfGrid = new ParamGridBuilder()
      .addGrid(rf.featureSubsetStrategy, Array("onethird", "sqrt", "log2"))
      .addGrid(rf.impurity, Array("gini", "entropy"))

    val lrGrid = new ParamGridBuilder()
      .addGrid(lr.elasticNetParam, Array(0.01, 0.1, 0.5, 0.7))
      .addGrid(lr.regParam, Array(0.01, 0.1, 1))

    val grids = Array(dtGrid, lrGrid, rfGrid)

    // Create the cross validation estimators for each.
    val cvEstimators = (estimators zip grids).map { case (est, grid) =>
      new CrossValidator()
          .setEstimator(est)
          .setEvaluator(eval)
          .setNumFolds(3)
          .setEstimatorParamMaps(grid.build())
    }

    // Fit the cross validation estimators.
    val models = cvEstimators.map(_.fitSync(training))

    // Make predictions on the testing data.
    val predictions = models.map(_.transformSync(testing))

    // Now compute metrics for each model.
    val metrics = (models zip predictions).map { case (model, prediction) =>
      SyncableMetrics.ComputeMulticlassMetrics(model, prediction, labelCol, predictionCol)
    }

    (estimators zip metrics).foreach { case (est, metric) =>
      println(est.getClass.getSimpleName)
      println("f: " + metric.accuracy)
    }
  }
}
