package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}

/**
  * This workflow runs a preprocessing pipeline, trains a cross-validated one vs. rest logistic regression model on
  * the animal shelter data, and evaluates it. It measures the time required to perform various operations.
  */
object AnimalFull {
  def run(config: Config): Unit = {
    // Create the session and read the dataset.
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readAnimalShelter(config.pathToData, spark), config.minNumRows)

    // Start timing operations and create the syncer if necessary.
    Timer.activate()

    if (config.syncer) Common.makeSyncer()

    // Build a preprocessing pipeline.
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

    // Split the data.
    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    // Create a cross-validated model.
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

    val eval = new MulticlassClassificationEvaluator()
      .setLabelCol(FeatureVectorizer.indexed(labelCol))
      .setPredictionCol(predictionCol)
      .setMetricName("f1")

    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.elasticNetParam, Array(0.1, 0.3, 0.5))
      .addGrid(lr.regParam, Array(0.1, 0.3, 0.5))
      .build()

    val cv = new CrossValidator()
      .setEstimator(ovr)
      .setEvaluator(eval)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3)

    val model = cv.fitSync(train)

    // Evaluate the model.
    val predictions = model.transformSync(test)
    model.saveSync("animal_full_model")

    val score = eval.evaluateSync(predictions, model)

    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
