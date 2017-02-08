package edu.mit.csail.db.ml.modeldb.evaluation

/**
 * I'll work on the following Kaggle problem to predict shelter outcomes for
 * cats and dogs: https://www.kaggle.com/c/shelter-animal-outcomes
 *
 * First, I did an exploratory data analysis
 * (see https://public.tableau.com/views/AnimalShelterOutcomes/OutcomebyTop10Colors?:embed=y&:display_count=yes).
 *
 * From the analysis, it seems age is highly predictive, as is sex. 
 * The animal type (cat or dog) also matters a good deal. 
 * The breed and color are less predictive.
  *
  * Run this with:
  * spark-submit --master local[*] --class "edu.mit.csail.db.ml.modeldb.evaluation.AnimalShelterOutcomes" target/scala-2.11/ml.jar <path_to_data_file>
  */
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.util._
import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest, RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel}

object AnimalShelterOutcomes {
  def main(args: Array[String]): Unit = {
    val pathToData = args(0)
    val spark = Common.makeSession()
    val syncer = Common.makeSyncer(shouldCountRows = true, shouldStoreGSCVE = true, shouldStoreSpecificModels = true)
    val df = Common.readAnimalShelter(pathToData, spark)

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

    val Array(rfTrain, rfTest) = preprocessedData.randomSplit(Array(0.7, 0.3))

    val rf = new RandomForestClassifier()
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setFeaturesCol(featuresCol)
        .setNumTrees(20)
        
    val rfPipeline = new Pipeline()
        .setStages(Array(rf, labelConverter))

    var eval = new MulticlassClassificationEvaluator()
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setMetricName("f1")

    val rfParamGrid = new ParamGridBuilder()
        .addGrid(rf.featureSubsetStrategy, Array("onethird", "sqrt", "log2"))
        .addGrid(rf.impurity, Array("gini", "entropy"))
        .addGrid(rf.maxDepth, Array(5, 7))
        .build()

    val rfCv = new CrossValidator()
        .setEstimator(rfPipeline)
        .setEstimatorParamMaps(rfParamGrid)
        .setEvaluator(eval)
        .setNumFolds(3)

    val rfCvModel = rfCv.fitSync(rfTrain)
    val rfPredictions = rfCvModel.transformSync(rfTest)

    println("Evaluation " + eval.evaluateSync(rfPredictions, rfCvModel.bestModel))


  // Get the confusion matrix.
  println(MakeMulticlassMetrics(rfPredictions, FeatureVectorizer.indexed(labelCol), predictionCol).confusionMatrix)

  // Let's figure out the feature importances.
  rfCvModel
      .bestModel
      .asInstanceOf[PipelineModel]
      .stages(0)
      .asInstanceOf[RandomForestClassificationModel]
      .featureImportances
      .toArray
      .zip(featureVectorNames)
      .sortWith(_._1 > _._1)
      .map(_._2)
      .take(10)
      .foreach(println)

    val Array(lrTrain, lrTest) = preprocessedData.randomSplit(Array(0.7, 0.3))

    val lr = new LogisticRegression()
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setFeaturesCol(featuresCol)
        .setMaxIter(20)
        
    val lrOvr = new OneVsRest()
        .setClassifier(lr)
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setFeaturesCol(featuresCol)
        
    val lrPipeline = new Pipeline()
        .setStages(Array(lrOvr, labelConverter))

    eval = new MulticlassClassificationEvaluator()
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setMetricName("f1")

    val lrParamGrid = new ParamGridBuilder()
        .addGrid(lr.elasticNetParam, Array(0.3, 0.5, 0.7))
        .addGrid(lr.fitIntercept, Array(false, true))
        .addGrid(lr.regParam, Array(0.3, 0.5, 0.7))
        .build()

    val lrCv = new CrossValidator()
        .setEstimator(lrPipeline)
        .setEstimatorParamMaps(lrParamGrid)
        .setEvaluator(eval)
        .setNumFolds(3)

    val lrCvModel = lrCv.fitSync(lrTrain)
    val lrPredictions = lrCvModel.transformSync(lrTest)

    println("Evaluation " + eval.evaluateSync(lrPredictions, lrCvModel.bestModel))

    // Get the confusion matrix.
    println(MakeMulticlassMetrics(lrPredictions, FeatureVectorizer.indexed(labelCol), predictionCol).confusionMatrix)
  }
}

