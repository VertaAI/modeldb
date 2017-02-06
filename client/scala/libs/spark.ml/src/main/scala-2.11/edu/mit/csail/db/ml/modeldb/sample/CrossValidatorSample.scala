package edu.mit.csail.db.ml.modeldb.sample

import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.{DefaultExperiment, ModelDbSyncer, NewExperimentRun, NewOrExistingProject}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

/**
  * This Spark program demonstrates the use of Syncables, which are a mechanism for intercepting events and objects of
  * interest in Spark ML and then persisting them in the Model DB.
  *
  * To run this program, do the following:
  *
  * Build:
  * sbt clean && sbt assembly
  *
  * Run:
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.CrossValidatorSample" target/scala-2.11/ml.jar
  *
  */
object CrossValidatorSample {
  def main(args: Array[String]) {
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject("cross validation",
        "harihar",
        "this example creates a cross validation"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun)
    )

    val sc = new SparkContext(new SparkConf().setMaster("local[*]").setAppName("test"))
    Logger.getLogger("org").setLevel(Level.OFF);
    
    val spark = SparkSession
      .builder()
      .appName("Cross Validator Sample")
      .getOrCreate()

    // Prepare training data from a list of (id, text, label) tuples.
    val training = spark.createDataFrame(Seq(
      (0L, "a b c d e spark", 1.0),
      (1L, "b d", 0.0),
      (2L, "spark f g h", 1.0),
      (3L, "hadoop mapreduce", 0.0),
      (4L, "b spark who", 1.0),
      (5L, "g d a y", 0.0),
      (6L, "spark fly", 1.0),
      (7L, "was mapreduce", 0.0),
      (8L, "e spark program", 1.0),
      (9L, "a e c l", 0.0),
      (10L, "spark compile", 1.0),
      (11L, "hadoop software", 0.0)
    )).toDF("id", "text", "label")

    ModelDbSyncer.annotate("I wonder if the dataframe:", training, "is too small...")

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    // With 3 values for hashingTF.numFeatures and 2 values for lr.regParam,
    // this grid will have 3 x 2 = 6 parameter settings for CrossValidator to choose from.
    val paramGrid = new ParamGridBuilder()
      .addGrid(hashingTF.numFeatures, Array(10))//, 100, 1000))
      .addGrid(lr.regParam, Array(0.1))//, 0.01))
      .build()

    // We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
    // This will allow us to jointly choose parameters for all Pipeline stages.
    // A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    // Note that the evaluator here is a BinaryClassificationEvaluator and its default metric
    // is areaUnderROC.
    val cv = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new BinaryClassificationEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3) // Use 3+ in practice

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = cv.fitSync(training, Seq("id", "text"))

    // Prepare test documents, which are unlabeled (id, text) tuples.
    val test = spark.createDataFrame(Seq(
      (4L, "spark i j k"),
      (5L, "l m n"),
      (6L, "mapreduce spark"),
      (7L, "apache hadoop")
    )).toDF("id", "text")

    // // Make predictions on test documents. cvModel uses the best model found (lrModel).
    cvModel.transformSync(test)
      .select("id", "text", "prediction")
      .rdd
      .foreach { row =>
        val id = row.getLong(0)
        val text = row.getString(1)
        val prediction = row.getDouble(2)
        println(s"($id, $text) --> prediction=$prediction")
      }
    ModelDbSyncer.syncer.get.sync()
    System.out.println("Finished.")
  }

}
