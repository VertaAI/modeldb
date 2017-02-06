package edu.mit.csail.db.ml.modeldb.sample

// // modeldb start
// import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
// import edu.mit.csail.db.ml.modeldb.client.{DefaultExperiment, ModelDbSyncer, NewExperimentRun, NewOrExistingProject}
// // modeldb end

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

    // // modeldb start
    // ModelDbSyncer.setSyncer(
    //   new ModelDbSyncer(projectConfig = NewOrExistingProject("simple sample",
    //     "mvartak",
    //     "simple LR for credit default prediction"
    //   ),
    //   experimentConfig = new DefaultExperiment,
    //   experimentRunConfig = new NewExperimentRun)
    // )
    // // modeldb end


    // read in the data
    val path = "/Users/mvartak/Projects/modeldb/data/credit-default.csv"
    val df = spark
      .read
      .option("header", true)
      .option("inferSchema", true)
      .csv(path)
    // // modeldb start
    //   .csvSync(path)
    // // modeldb end

    val assembler = new VectorAssembler()
      .setInputCols(Array("LIMIT_BAL", "SEX", 
        "EDUCATION", "MARRIAGE", "AGE"))
      .setOutputCol("features")

    val transformedDf = assembler
      .transform(df)
    // // modeldb start
    //   .transformSync(df)
    // // modeldb end

    val logReg = new LogisticRegression()
      .setLabelCol("DEFAULT")

    // create train and test sets
    val Array(trainDf, testDf) = transformedDf.randomSplit(Array(0.7, 0.3))

    val logRegModel = logReg
      .fit(trainDf)
    // // modeldb start
    //   .fitSync(trainDf)
    // // modeldb end
    System.out.println(s"Coefficients: ${logRegModel.coefficients}")

    val predictions = logRegModel
      .transform(testDf)
    // // modeldb start
    //   .transformSync(testDf)
    // // modeldb end
    predictions.printSchema()

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("DEFAULT")

    // compute metrics
    val metric = evaluator
      .evaluate(predictions)
    // // modeldb start
    //   .evaluateSync(predictions, logRegModel)
    // // modeldb end
    System.out.println(s"Metric: ${metric}")

    // TODO: need to add something that produces multiple models so we can graph

  }
}