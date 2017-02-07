package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}

/**
  * Like the AnimalFull workflow, but for the IMDB data.
  */
object IMDBFull {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readImdb(config.pathToData, spark), config.minNumRows)
    Timer.activate()
    if (config.syncer) Common.makeSyncer()

    // Apply preprocessing pipeline.
    val (preprocessedData, featureVectorNames, _) = FeatureVectorizer(
      df,
      Array("color", "content_rating", "country", "first_genre", "second_genre"),
      Array[String](),
      featuresCol = "features",
      scaledCols = Array("num_critic_for_reviews", "gross", "num_user_for_reviews", "title_year", "num_voted_users"),
      scaler = Some(true, true)
    )(spark.sqlContext)

    // Train test split.
    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    // Here are the columns we care about for prediction.
    val labelCol = "imdb_score"
    val featuresCol = "features"
    val predictionCol = "prediction"

    // Define the base estimator.
    val lr = new LinearRegression()
      .setMaxIter(10)
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)
      .setFeaturesCol(featuresCol)

    // We'll evaluate using RMSE.
    val eval = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)

    // We'll define a grid search for the regularization parameter and elastic net parameter.
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.3, 0.5))
      .addGrid(lr.elasticNetParam, Array(0.1, 0.3, 0.8))
      .build()

    // Define the cross validator.
    val cv = new CrossValidator()
      .setEstimator(lr)
      .setEvaluator(eval)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3)

    // Select a model with cross validation.
    val model = cv.fitSync(train)
    model.saveSync("imdb_full_model")

    // Evaluate the resulting model.
    val predictions = model.transformSync(test)
    val score = eval.evaluateSync(predictions, model)

    // Write the timing data to an output file.
    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
