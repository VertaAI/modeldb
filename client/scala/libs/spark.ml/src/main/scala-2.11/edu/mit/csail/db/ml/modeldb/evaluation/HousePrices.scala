package edu.mit.csail.db.ml.modeldb.evaluation

/**
  * Here's a notebook for the Advanced Housing Regression Kaggle competition.
  * (see https://www.kaggle.com/c/house-prices-advanced-regression-techniques)
  *
  * Here's an exploratory data analysis
  * (see https://public.tableau.com/views/HousingRegression/Utilities?:embed=y&:display_count=yes)
  *
  * Run this with:
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.evaluation.HousePrices" target/scala-2.11/ml.jar <path_to_data_file>
  */
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.util._
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.{GBTRegressor, LinearRegression, RandomForestRegressor}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}

object HousePrices {
  def main(args: Array[String]): Unit = {
    val pathToData = args(0)
    val spark = Common.makeSession()
    val syncer = Common.makeSyncer(shouldCountRows = true, shouldStoreGSCVE = true, shouldStoreSpecificModels = true)
    val df = Common.readHousingPrices(pathToData, spark)

    // Let's categorize the columns:
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

    // Let's make a cross validated LinearRegressionModel.

    val Array(lrTrain, lrTest) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

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

    val lrCvModel = lrCv.fitSync(lrTrain)
    val lrPredictions = lrCvModel.transformSync(lrTest)
    println("Evaluation " + eval.evaluateSync(lrPredictions, lrCvModel.bestModel))

    // Let's try using a RandomForestRegressor

    val Array(rfTrain, rfTest) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    val rf = new RandomForestRegressor()
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)
        .setPredictionCol(predictionCol)
        .setNumTrees(20)

    val rfParamGrid = new ParamGridBuilder()
        .addGrid(rf.featureSubsetStrategy, Array("sqrt", "onethird", "log2"))
        .addGrid(rf.maxDepth, Array(5, 7))
        .build()

    val rfCv = new CrossValidator()
        .setEstimator(rf)
        .setEvaluator(eval)
        .setEstimatorParamMaps(rfParamGrid)
        .setNumFolds(3)

    val rfCvModel = rfCv.fitSync(rfTrain)
    val rfPredictions = rfCvModel.transformSync(rfTest)
    println("Evaluation " + eval.evaluateSync(rfPredictions, rfCvModel.bestModel))

    // The random forest regressor is slightly better. Let's try gradient boosted trees.

    val Array(gbtTrain, gbtTest) = preprocessedData.randomSplitSync(Array(0.7, 0.3))

    val gbt = new GBTRegressor()
        .setLabelCol(labelCol)
        .setFeaturesCol(featuresCol)
        .setPredictionCol(predictionCol)
        .setMaxIter(20)

    val gbtParamGrid = new ParamGridBuilder()
        .addGrid(gbt.maxDepth, Array(5, 7))
        .build()

    val gbtCv = new CrossValidator()
        .setEstimator(gbt)
        .setEvaluator(eval)
        .setEstimatorParamMaps(gbtParamGrid)
        .setNumFolds(3)

    val gbtCvModel = gbtCv.fitSync(gbtTrain)
    val gbtPredictions = gbtCvModel.transformSync(gbtTest)
    println("Evaluation " + eval.evaluateSync(gbtPredictions, gbtCvModel.bestModel))
  }
}
