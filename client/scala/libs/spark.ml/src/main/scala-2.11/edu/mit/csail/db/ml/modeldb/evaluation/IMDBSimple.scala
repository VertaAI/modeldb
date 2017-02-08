package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegression
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

/**
  * Like the AnimalSimple workflow, but for the IMDB data.
  */
object IMDBSimple {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readImdb(config.pathToData, spark), config.minNumRows)

    // We cannot do machine learning without doing some preprocessing, so we'll
    // preprocess the data, but we won't time it.
    val (preprocessedData, featureVectorNames, _) = FeatureVectorizer(
      df,
      Array("color", "content_rating", "country", "first_genre", "second_genre"),
      Array[String](),
      featuresCol = "features",
      scaledCols = Array("num_critic_for_reviews", "gross", "num_user_for_reviews", "title_year", "num_voted_users"),
      scaler = Some(true, true)
    )(spark.sqlContext)

    Timer.activate()

    if (config.syncer) Common.makeSyncer()

    // Train test split.
    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    // Train a Linear Regression model.
    val labelCol = "imdb_score"
    val featuresCol = "features"
    val predictionCol = "prediction"
    val lr = new LinearRegression()
      .setMaxIter(10)
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)
      .setFeaturesCol(featuresCol)
      .setRegParam(0.3)
      .setElasticNetParam(0.1)
    val lrModel = lr.fitSync(train)
    lrModel.saveSync("imdb_simple_lr")

    // Evaluate the model.
    val eval = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)

    val predictions = lrModel.transformSync(test)
    val score = eval.evaluateSync(predictions, lrModel)

    // Write the timing data to an output file.
    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
