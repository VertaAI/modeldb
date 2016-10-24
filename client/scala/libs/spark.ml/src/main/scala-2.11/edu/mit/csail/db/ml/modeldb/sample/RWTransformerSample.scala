package edu.mit.csail.db.ml.modeldb.sample

import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, SyncableMetrics, DefaultExperiment, NewExperimentRun}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.feature.Tokenizer
import org.apache.spark.sql.SparkSession
import ModelDbSyncer._

/**
  * This program shows how to read a Transformer from the ModelDB server.
  * To run this program, do the following:
  *
  * Build:
  * sbt clean && sbt assembly
  *
  * Run:
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.sample.RWTransformerSample" target/scala-2.11/ml.jar
  *
  */
object RWTransformerSample {
  def main(args: Array[String]): Unit = {
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject("read/write transformer",
        "harihar",
        "this example writes a transformer and reads it from modeldb"
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun)
    )

    val sc = new SparkContext(new SparkConf().setMaster("local[*]").setAppName("test"))
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

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")

    tokenizer.transformSync(training)

    val readTokenizer = ModelDbSyncer.syncer.get.load(ModelDbSyncer.syncer.get.id(tokenizer).get, Tokenizer.read).get
    println(readTokenizer.getInputCol + " and " + readTokenizer.getOutputCol)
  }
}
