package edu.mit.csail.db.ml.modeldb.sample

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator

/**
* http://archive.ics.uci.edu/ml/datasets/default+of+credit+card+clients
*/

object SimpleSample {
  def main(args: Array[String]) {
    // spark setup
    val sc = new SparkContext(new SparkConf().setMaster("local[*]").setAppName("test"))
    Logger.getLogger("org").setLevel(Level.OFF);
    System.out.println("*******************")
    
    val spark = SparkSession
      .builder()
      .appName("Simple Sample")
      .getOrCreate()

    // read in the data
    val path = "data/credit-default.csv"
    val df = spark
      .read
      .option("header", true)
      .option("inferSchema", true)
      .csv(path)

    val assembler = new VectorAssembler()
      .setInputCols(Array("LIMIT_BAL", "SEX", "EDUCATION", "MARRIAGE", "AGE"))
      .setOutputCol("features")

    val transformedDf = assembler.transform(df)

    // create train and test sets
    val Array(trainDf, testDf) = transformedDf.randomSplit(Array(0.7, 0.3))

    val logReg = new LogisticRegression()
      .setLabelCol("DEFAULT")

    val logRegModel = logReg.fit(trainDf)
    System.out.println(s"Coefficients: ${logRegModel.coefficients}")

    val predictions = logRegModel.transform(testDf)
    predictions.printSchema()

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("DEFAULT")

    // compute metrics
    val metric = evaluator.evaluate(predictions)
    System.out.println(s"Metric: ${metric}")

  }
}