package edu.mit.csail.db.ml.modeldb.sample

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
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.PipelineSample" target/scala-2.11/ml.jar
  *   <path_to_spark_installation>/data/mllib/sample_libsvm_data.txt
  */

import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import ModelDbSyncer._

object PipelineSample {
  def main(args: Array[String]): Unit = {
    // Set up the syncer.
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject("pipeline",
        "harihar",
        "this example creates and runs a pipeline"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun)
    )

    val sc = new SparkContext(new SparkConf().setMaster("local[*]").setAppName("test"))
    val spark = SparkSession
      .builder()
      .appName("Pipeline Sample")
      .getOrCreate()

    // Read the command line argument which indicates the path to the data file.
    val pathToDataFile = args(0)
    val data = spark.read.format("libsvm").loadSync(pathToDataFile).tag("Sample libsvm data")

    // MODELDB: Optional: Register your object with ModelDB so that a creation event can be logged.
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(data)

    // Automatically identify categorical features, and index them.
    // Set maxCategories so features with > 4 distinct values are treated as continuous.
    // MODELDB: Optional: Register your object with ModelDB so that a creation event can be logged.
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(4)
      .fit(data)

    // Train a RandomForest model.
    // MODELDB: Optional: Register your object with ModelDB so that a creation event can be logged.
    val lr = new LogisticRegression()
      .setLabelCol("indexedLabel")
      .setFeaturesCol("indexedFeatures")
      .setMaxIter(10)
      .setElasticNetParam(0.5)
      .tag("logistic regression on sample data")

    // Optional: Register your object with ModelDB so that a creation event can be logged.
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Split the data into training and test sets (30% held out for testing)
    val pipeline = new Pipeline().setStages(Array(labelIndexer, featureIndexer, lr, labelConverter))
      .tag("sample libsvm pipeline")

    // MODELDB: Optional: Record the split event to the ModelDB (not implemented yet).
    val Array(trainingData, testData) = data.randomSplitSync(Array(0.7, 0.3))

    ModelDbSyncer.annotate("Pipelines are neat.", pipeline)

    // Chain indexers and forest in a Pipeline
    // MODELDB: Log the FitEvent to the ModelDB.
    val model: PipelineModel = pipeline.fitSync(trainingData).tag("pipeline model for libsvm data")

    // MODELDB: Compute metrics on the model.
    val metrics = SyncableMetrics.ComputeMulticlassMetrics(
      model,
      model.transformSync(testData),
      "label",
      "predictedLabel"
    )
    // MODELDB: Sync on demand.
    syncer.get.sync()
    println(metrics.accuracy)


    println("Done now")
  }
}
