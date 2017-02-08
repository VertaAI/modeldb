package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.{GBTRegressor, LinearRegression, RandomForestRegressor}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.sql.DataFrame

/**
  * Like the AnimalExploratory workflow, but for the housing data.
  */
object HousingExploratory {
  def run(config: Config): Unit = {
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readHousingPrices(config.pathToData, spark), config.minNumRows)

    Timer.activate()

    if (config.syncer) Common.makeSyncer(
      appName = "Housing Prices",
      appDesc = "Predict sale prices for homes."
    )

    val uselessCols = Set("Id")
    val leakCols = Set("MoSold", "YrSold", "SaleType", "SaleCondition")
    val labelCol = "label"
    val predictionCol = "prediction"
    val featuresCol = "features"

    def makeLrModel(data: DataFrame, featureVectorNames: Array[String]): Unit = {
      val Array(lrTrain, lrTest) = data.randomSplitSync(Array(0.7, 0.3))

      val lr = new LinearRegression()
        .setMaxIter(20)
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)
        .setPredictionCol(predictionCol)

      val eval = new RegressionEvaluator()
        .setLabelCol(labelCol)
        .setPredictionCol(predictionCol)

      val paramGrid = new ParamGridBuilder()
        .addGrid(lr.elasticNetParam, Array(0.1, 0.3, 0.5, 0.7, 0.9))
        .addGrid(lr.regParam, Array(0.1, 0.3, 0.5, 0.7, 0.9))
        .build()

      val lrCv = new CrossValidator()
        .setEstimator(lr)
        .setEstimatorParamMaps(paramGrid)
        .setEvaluator(eval)
        .setNumFolds(3)

      val lrCvModel = lrCv.fitSync(lrTrain, featureVectorNames)
      lrCvModel.saveSync("housing_exploratory_lr")
      val lrPredictions = lrCvModel.transformSync(lrTest)
      println("Evaluation LR " + eval.evaluateSync(lrPredictions, lrCvModel.bestModel))
    }

    def makeRfModel(data: DataFrame, featureVectorNames: Array[String]): Unit = {
      val Array(rfTrain, rfTest) = data.randomSplitSync(Array(0.7, 0.3))

      val rf = new RandomForestRegressor()
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)
        .setPredictionCol(predictionCol)
        .setNumTrees(20)

      val rfParamGrid = new ParamGridBuilder()
        .addGrid(rf.featureSubsetStrategy, Array("sqrt", "onethird", "log2"))
        .addGrid(rf.maxDepth, Array(5, 7))
        .build()

      val eval = new RegressionEvaluator()
        .setLabelCol(labelCol)
        .setPredictionCol(predictionCol)

      val rfCv = new CrossValidator()
        .setEstimator(rf)
        .setEvaluator(eval)
        .setEstimatorParamMaps(rfParamGrid)
        .setNumFolds(3)

      val rfCvModel = rfCv.fitSync(rfTrain, featureVectorNames)
      rfCvModel.saveSync("housing_exploratory_rf")
      val rfPredictions = rfCvModel.transformSync(rfTest)
      println("Evaluation RF " + eval.evaluateSync(rfPredictions, rfCvModel.bestModel))
    }

    def makeGbtModel(data: DataFrame, featureVectorNames: Array[String]): Unit = {
      val Array(gbtTrain, gbtTest) = data.randomSplitSync(Array(0.7, 0.3))

      val gbt = new GBTRegressor()
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)
        .setPredictionCol(predictionCol)
        .setMaxIter(20)

      val gbtParamGrid = new ParamGridBuilder()
        .addGrid(gbt.maxDepth, Array(5, 7))
        .build()

      val eval = new RegressionEvaluator()
        .setLabelCol(labelCol)
        .setPredictionCol(predictionCol)

      val gbtCv = new CrossValidator()
        .setEstimator(gbt)
        .setEvaluator(eval)
        .setEstimatorParamMaps(gbtParamGrid)
        .setNumFolds(3)

      val gbtCvModel = gbtCv.fitSync(gbtTrain, featureVectorNames)
      gbtCvModel.saveSync("housing_exploratory_gbt")
      val gbtPredictions = gbtCvModel.transformSync(gbtTest)
      println("Evaluation " + eval.evaluateSync(gbtPredictions, gbtCvModel.bestModel))
    }

    val (preprocessedData, featureVectorNames, labelConverterOpt) = FeatureVectorizer(
      df.toDF(),
      Set("MSSubClass", "MSZoning", "Street", "Alley",
        "LotShape", "LandContour", "Utilities", "LotConfig", "LandSlope",
        "Neighborhood", "Condition1", "Condition2", "BldgType", "HouseStyle",
        "OverallQual", "OverallCond", "RoofStyle", "RoofMatl", "Exterior1st",
        "Exterior2nd", "MasVnrType", "ExterQual", "ExterCond", "Foundation",
        "BsmtQual", "BsmtCond", "BsmtExposure", "BsmtFinType1", "BsmtFinType2",
        "Heating", "HeatingQC", "CentralAir", "Electrical", "KitchenQual",
        "Functional", "FireplaceQu", "GarageType", "GarageFinish", "GarageQual",
        "GarageCond", "PavedDrive", "PoolQC", "Fence", "MiscFeature").toArray,
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Set("LotFrontage", "LotArea", "YearBuilt", "YearRemodAdd",
        "MasVnrArea", "BsmtFinSF1", "BsmtFinSF2", "BsmtUnfSF", "TotalBsmtSF",
        "1stFlrSF", "2ndFlrSF", "LowQualFinSF", "GrLivArea", "BsmtFullBath",
        "BsmtHalfBath", "FullBath", "HalfBath", "TotRmsAbvGrd", "Fireplaces",
        "GarageYrBlt", "GarageCars", "GarageArea", "WoodDeckSF", "OpenPorchSF",
        "EnclosedPorch", "3SsnPorch", "ScreenPorch", "PoolArea", "MiscVal").toArray,
      Some(true, true)
    )(spark.sqlContext)

    makeLrModel(preprocessedData, featureVectorNames)
    makeRfModel(preprocessedData, featureVectorNames)
    makeGbtModel(preprocessedData, featureVectorNames)

    val (preprocessedData2, featureVectorNames2, labelConverterOpt2) = FeatureVectorizer(
      df.toDF(),
      Set("Neighborhood", "BldgType", "HouseStyle", "OverallQual", "OverallCond").toArray,
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Set("LotArea", "YearBuilt", "YearRemodAdd", "1stFlrSF", "2ndFlrSF", "LowQualFinSF", "GrLivArea", "BsmtFullBath",
        "BsmtHalfBath", "FullBath", "HalfBath", "PoolArea").toArray,
      Some(true, true)
    )(spark.sqlContext)

    makeLrModel(preprocessedData2, featureVectorNames2)
    makeRfModel(preprocessedData2, featureVectorNames2)
    makeGbtModel(preprocessedData2, featureVectorNames2)

    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
