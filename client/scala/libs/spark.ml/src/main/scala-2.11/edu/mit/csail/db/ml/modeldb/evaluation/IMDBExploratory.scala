package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql._
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

/**
  * Like the AnimalExploratory workflow, but for the IMDB data.
  */
object IMDBExploratory {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readImdb(config.pathToData, spark), config.minNumRows)

    Timer.activate()
    if (config.syncer) Common.makeSyncer(
      appName = "IMDB",
      appDesc = "Predict IMDB score for movies."
    )

    // So, we will use the following features in our model.
    // Color, Number of Critics, Gross, Number of User Reviews,
    // Number of User Votes, Content Rating, Title Year, Country, First Genre,
    // Second Genre.
    val categoricalCols: Array[String] = Array(
      "color",
      "content_rating",
      "country",
      "first_genre",
      "second_genre"
    )
    val scaledCols = Array(
      "num_critic_for_reviews",
      "gross",
      "num_user_for_reviews",
      "title_year",
      "num_voted_users"
    )

    // Create the feature vector.
    val (preprocessedData, featureVectorNames, _) = FeatureVectorizer(
      df.toDF(),
      categoricalCols,
      Array[String](),
      featuresCol = "features",
      scaledCols = scaledCols,
      scaler = Some(true, true)
    )(spark.sqlContext)

    // Create the train and test sets.
    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    // Define the columns that the model will use.
    val labelCol = "imdb_score"
    val featuresCol = "features"
    val predictionCol = "prediction"

    // Create a regression evaluator.
    def makeEvaluator() = {
      new RegressionEvaluator()
        .setLabelCol(labelCol)
        .setPredictionCol(predictionCol)
    }

    // Function that creates a Linear Regression model, preints out its
    // weights, and returns its predictions on the test set.
    def makeLrModel(train: DataFrame,
                    test: DataFrame,
                    featureVectorNames: Option[Array[String]] = None) = {
      val lr = new LinearRegression()
        .setMaxIter(20)
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)

      val eval = makeEvaluator()

      val paramGrid = new ParamGridBuilder()
        .addGrid(lr.regParam, Array(0.1, 0.3, 0.5))
        .addGrid(lr.elasticNetParam, Array(0.1, 0.3, 0.8))
        .build()

      val lrCv = new CrossValidator()
        .setEstimator(lr)
        .setEvaluator(eval)
        .setEstimatorParamMaps(paramGrid)
        .setNumFolds(3)

      val lrCvModel = lrCv.fitSync(train)
      lrCvModel.saveSync("imdb_exploratory_lr")
      val lrPredictions = lrCvModel.transformSync(test)

      /*
      if (featureVectorNames.isDefined)
          lrCvModel
              .bestModel
              .asInstanceOf[LinearRegressionModel]
              .coefficients
              .toArray
              .zip(featureVectorNames.get)
              .sortWith(_._1.abs > _._1.abs)
              .foreach{ case (coeff, value) => println(s"$value: $coeff")}
      */
      (lrCvModel.bestModel.asInstanceOf[LinearRegressionModel], lrPredictions)
    }

    def makeRfModel(train: DataFrame, test: DataFrame, featureVectorNames: Option[Array[String]] = None) = {
      val rf = new RandomForestRegressor()
        .setNumTrees(20)
        .setFeaturesCol(featuresCol)
        .setLabelCol(labelCol)

      val eval = makeEvaluator()

      val paramGrid = new ParamGridBuilder()
        .addGrid(rf.featureSubsetStrategy, Array("log2", "sqrt", "onethird"))
        .addGrid(rf.maxDepth, Array(5, 7, 9))
        .build()

      val rfCv = new CrossValidator()
        .setEstimator(rf)
        .setEvaluator(eval)
        .setEstimatorParamMaps(paramGrid)
        .setNumFolds(3)

      val rfCvModel = rfCv.fitSync(train)
      rfCvModel.saveSync("imdb_exploratory_rf")
      val rfPredictions = rfCvModel.transformSync(test)

      (rfCvModel.bestModel.asInstanceOf[RandomForestRegressionModel], rfPredictions)
    }

    def makeGbtModel(train: DataFrame, test: DataFrame, featureVectorNames: Option[Array[String]] = None) = {
      val gbt = new GBTRegressor()
        .setMaxIter(10)
        .setFeaturesCol(featuresCol)
        .setLabelCol(labelCol)

      val eval = makeEvaluator()

      val paramGrid = new ParamGridBuilder()
        .addGrid(gbt.lossType, Array("squared", "absolute"))
        .addGrid(gbt.maxDepth, Array(5, 7, 9))
        .build()

      val gbtCv = new CrossValidator()
        .setEstimator(gbt)
        .setEvaluator(eval)
        .setEstimatorParamMaps(paramGrid)
        .setNumFolds(3)

      val gbtCvModel = gbtCv.fitSync(train)
      gbtCvModel.saveSync("imdb_exploratory_gbt")
      val gbtPredictions = gbtCvModel.transformSync(test)

      (gbtCvModel.bestModel.asInstanceOf[GBTRegressionModel], gbtPredictions)
    }

    // Train and evaluate the model.
    val (lrModel, lrPredictions) = makeLrModel(train, test, Some(featureVectorNames))
    println("Evaluating LR " + makeEvaluator().evaluateSync(lrPredictions, lrModel))

    val (rfModel, rfPredictions) = makeRfModel(train, test, Some(featureVectorNames))
    println("Evaluating RF " + makeEvaluator().evaluateSync(rfPredictions, rfModel))

    val (gbtModel, gbtPredictions) = makeGbtModel(train, test, Some(featureVectorNames))
    println("Evaluating GBT " + makeEvaluator().evaluateSync(gbtPredictions, gbtModel))

    // Let's try doing the calculation again and try using the languages as a feature.
    val (preprocessedData2, featureVectorNames2, _) = FeatureVectorizer(
      df.toDF(),
      Array(
        "color",
        "content_rating",
        "country",
        "language",
        "first_genre",
        "second_genre"
      ),
      Array[String](),
      featuresCol = "features",
      scaledCols = Array(
        "num_critic_for_reviews",
        "gross",
        "num_user_for_reviews",
        "title_year",
        "num_voted_users"
      ),
      scaler = Some(true, true)
    )(spark.sqlContext)
    val Array(train2, test2) = preprocessedData2.randomSplitSync(Array(0.7, 0.3))

    val (lrModel2, lrPredictions2) = makeLrModel(train2, test2, Some(featureVectorNames2))
    println("Evaluating LR2 " + makeEvaluator().evaluateSync(lrPredictions2, lrModel2))

    val (rfModel2, rfPredictions2) = makeRfModel(train2, test2, Some(featureVectorNames))
    println("Evaluating RF2 " + makeEvaluator().evaluateSync(rfPredictions2, rfModel2))

    val (gbtModel2, gbtPredictions2) = makeGbtModel(train2, test2, Some(featureVectorNames))
    println("Evaluating GBT2 " + makeEvaluator().evaluateSync(gbtPredictions2, gbtModel2))

    // Using the language decreased the RMS error. Now, let's try again and remove the country and language
    // features.
    val (preprocessedData3, featureVectorNames3, _) = FeatureVectorizer(
      df.toDF(),
      Array(
        "color",
        "content_rating",
        "first_genre",
        "second_genre"
      ),
      Array[String](),
      featuresCol = "features",
      scaledCols = Array(
        "num_critic_for_reviews",
        "gross",
        "num_user_for_reviews",
        "title_year",
        "num_voted_users"
      ),
      scaler = Some(true, true)
    )(spark.sqlContext)
    val Array(train3, test3) = preprocessedData3.randomSplitSync(Array(0.7, 0.3))

    val (lrModel3, lrPredictions3) = makeLrModel(train3, test3, Some(featureVectorNames3))
    println("Evaluating LR3 " + makeEvaluator().evaluateSync(lrPredictions3, lrModel3))

    val (rfModel3, rfPredictions3) = makeRfModel(train3, test3, Some(featureVectorNames))
    println("Evaluating RF3 " + makeEvaluator().evaluateSync(rfPredictions3, rfModel3))

    val (gbtModel3, gbtPredictions3) = makeGbtModel(train3, test3, Some(featureVectorNames))
    println("Evaluating GBT3 " + makeEvaluator().evaluateSync(gbtPredictions3, gbtModel3))



    // Write the timing data to an output file.
    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }

}
