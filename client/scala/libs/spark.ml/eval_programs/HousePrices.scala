/**
 * Here's a notebook for the Advanced Housing Regression Kaggle competition.
 * (see https://www.kaggle.com/c/house-prices-advanced-regression-techniques)
 *
 * Here's an exploratory data analysis
 * (see https://public.tableau.com/views/HousingRegression/Utilities?:embed=y&:display_count=yes)
 */
import org.apache.spark.sql.types.{IntegerType, StructType, StructField}
import org.apache.spark.sql.Row
import scala.util.Try
import edu.mit.csail.db.ml.modeldb.util._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, DefaultExperiment, NewExperimentRun, SyncableMetrics}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.regression.{LinearRegression}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.GBTRegressor
import org.apache.spark.ml.regression.RandomForestRegressor

object Main {
  def run(pathToData: String): Unit = {
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "House Prices",
        "hsubrama@mit.edu",
        "Attempt to predict home prices."
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun)
    )

    // Read the data.
    val dfOrig = spark
        .read
        .option("header", true)
        .option("inferSchema", true)
        .option("ignoreLeadingWhiteSpace", true)
        .option("ignoreTrainingWhiteSpace", true)
        .option("nullValue", "Unknown")
        .csv(pathToData)
        .withColumn("label", $"SalePrice".cast("double"))

    val hasNAs = Set("GarageYrBlt", "MasVnrArea", "LotFrontage")
    val numericalIndices = dfOrig
        .schema
        .fields
        .zipWithIndex
        .filter{case (value, index) => value.dataType == IntegerType || hasNAs.contains(value.name)}
        .map{case (value, index) => index}
        .toSet
    val df = spark.sqlContext.createDataFrame(dfOrig.rdd.map{ row =>
        Row.fromSeq(row.toSeq.zipWithIndex.map{ case (value, index) =>
            if (numericalIndices.contains(index) && (value == null || value.isInstanceOf[String]))
                Try(value.asInstanceOf[String].toInt).toOption.getOrElse(0)
            else
                value  
        })
    }, StructType(dfOrig.schema.map(field => StructField(field.name, if (hasNAs.contains(field.name)) IntegerType else field.dataType))))

    // Print the schema.
    df.schema.foreach(println)

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
        .setNumTrees(200)

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
