package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegression
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}

/**
  * Like the AnimalFull workflow, but for the housing data.
  */
object HousingFull {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readHousingPrices(config.pathToData, spark), config.minNumRows)
    Timer.activate()

    if (config.syncer) Common.makeSyncer()

    val uselessCols = Set("Id")
    val leakCols = Set("MoSold", "YrSold", "SaleType", "SaleCondition")
    val labelCol = "label"
    val predictionCol = "prediction"
    val featuresCol = "features"
    val categoricalCols = Set("MSSubClass", "MSZoning", "Street", "Alley",
      "LotShape", "LandContour", "Utilities", "LotConfig", "LandSlope",
      "Neighborhood", "Condition1", "Condition2", "BldgType", "HouseStyle",
      "OverallQual", "OverallCond", "RoofStyle", "RoofMatl", "Exterior1st",
      "Exterior2nd", "MasVnrType", "ExterQual", "ExterCond", "Foundation",
      "BsmtQual", "BsmtCond", "BsmtExposure", "BsmtFinType1", "BsmtFinType2",
      "Heating", "HeatingQC", "CentralAir", "Electrical", "KitchenQual",
      "Functional", "FireplaceQu", "GarageType", "GarageFinish", "GarageQual",
      "GarageCond", "PavedDrive", "PoolQC", "Fence", "MiscFeature")
    val scaledCols = Set("LotFrontage", "LotArea", "YearBuilt", "YearRemodAdd",
      "MasVnrArea", "BsmtFinSF1", "BsmtFinSF2", "BsmtUnfSF", "TotalBsmtSF",
      "1stFlrSF", "2ndFlrSF", "LowQualFinSF", "GrLivArea", "BsmtFullBath",
      "BsmtHalfBath", "FullBath", "HalfBath", "TotRmsAbvGrd", "Fireplaces",
      "GarageYrBlt", "GarageCars", "GarageArea", "WoodDeckSF", "OpenPorchSF",
      "EnclosedPorch", "3SsnPorch", "ScreenPorch", "PoolArea", "MiscVal")

    val (preprocessedData, featureVectorNames, labelConverterOpt) = FeatureVectorizer(
      df.toDF(),
      categoricalCols.toArray,
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      scaledCols.toArray,
      Some(true, true)
    )(spark.sqlContext)

    val Array(train, test) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    val lr = new LinearRegression()
      .setMaxIter(20)
      .setLabelCol(labelCol)
      .setFeaturesCol(featuresCol)
      .setPredictionCol(predictionCol)
      .setElasticNetParam(0.1)
      .setRegParam(0.1)

    val eval = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol(labelCol)
      .setPredictionCol(predictionCol)

    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.elasticNetParam, Array(0.1, 0.3, 0.5))
      .addGrid(lr.regParam, Array(0.1, 0.3, 0.5))
      .build()

    val cv = new CrossValidator()
      .setEstimator(lr)
      .setEvaluator(eval)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3)

    val model = cv.fitSync(train)
    model.saveSync("housing_full_model")
    val predictions = model.transformSync(test)

    val score = eval.evaluateSync(predictions, model)

    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
