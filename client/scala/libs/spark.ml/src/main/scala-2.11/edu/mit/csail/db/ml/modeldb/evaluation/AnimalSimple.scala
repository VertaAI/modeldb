package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

/**
  * This workflow trains a one vs. rest logistic regression model on the animal shelter data and evaluates it.
  * It measures the time required to perform various operations.
  */
object AnimalSimple {
  /**
    * Runs the workflow.
    * @param config - Workflow configuration.
    */
  def run(config: Config): Unit = {
    // Create the session and read the data.
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readAnimalShelter(config.pathToData, spark), config.minNumRows)

    // Apply a pre-processing pipeline.
    val categoricalCols = Array[String]("AnimalType", "SexuponOutcome",
      "SimpleBreed", "SimpleColor")
    val numericalCol = "AgeInYears"
    val labelCol = "OutcomeType"
    val featuresCol = "features"
    val predictionCol = "prediction"

    val (preprocessedData, featureVectorNames, labelConverterOpt) = FeatureVectorizer(
      df.toDF(),
      categoricalCols,
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Array(numericalCol),
      Some((true, true))
    )(spark.sqlContext)
    val labelConverter = labelConverterOpt.get

    // Now, we'll set up the timing and create the syncer (if applicable).
    Timer.activate()

    if (config.syncer) Common.makeSyncer()

    // Split the data.
    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    // Train the model.
    val lr = new LogisticRegression()
      .setMaxIter(20)
      .setLabelCol(FeatureVectorizer.indexed(labelCol))
      .setPredictionCol(predictionCol)
      .setFeaturesCol(featuresCol)

    val ovr = new OneVsRest()
      .setClassifier(lr)
      .setLabelCol(FeatureVectorizer.indexed(labelCol))
      .setPredictionCol(predictionCol)
      .setFeaturesCol(featuresCol)

    val model = ovr.fitSync(train)
    model.saveSync("animal_simple_ovr_lr")

    // Evaluate the model.
    val predictions = model.transformSync(test)

    val eval = new MulticlassClassificationEvaluator()
      .setLabelCol(FeatureVectorizer.indexed(labelCol))
      .setPredictionCol(predictionCol)
      .setMetricName("f1")

    val score = eval.evaluateSync(predictions, model)

    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
