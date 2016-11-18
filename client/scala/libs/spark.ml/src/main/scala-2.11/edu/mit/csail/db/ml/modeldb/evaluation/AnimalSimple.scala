package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator

object AnimalSimple {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readAnimalShelter(config.pathToData, spark), config.minNumRows)

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

    Timer.activate()

    if (config.syncer) Common.makeSyncer()

    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

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
