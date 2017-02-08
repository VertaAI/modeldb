package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.util.FeatureVectorizer
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.IndexToString
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.DataFrame
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

/**
  * This workflow creates a preprocessing pipeline, trains multiple cross-validated models with various feature-sets,
  * and evaluates each of them.
  */
object AnimalExploratory {
  def run(config: Config): Unit = {
    // Read the dataset.
    val spark = Common.makeSession()
    val df = Common.ensureMinSize(Common.readAnimalShelter(config.pathToData, spark), config.minNumRows)

    // Start timing operations and create the syncer if applicable.
    Timer.activate()
    if (config.syncer) Common.makeSyncer(
      appName = "Animal Shelter Outcomes",
      appDesc = "Predict outcome (e.g. adopted, transferred) for animals in a shelter."
    )

    val labelCol = "OutcomeType"
    val featuresCol = "features"
    val predictionCol = "prediction"

    // Helper function for creating and evaluating a cross-validated one vs. rest logistic regression model.
    def makeLrModel(preprocData: DataFrame,
                    labelConverter: IndexToString,
                    featureVectorNames: Option[Array[String]] = None): Unit = {
      val Array(train, test) = preprocData.randomSplitSync(Array(0.7, 0.3))

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

      val eval = new MulticlassClassificationEvaluator()
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

      val model = lrCv.fitSync(train)
      model.saveSync("animal_exploratory_lr")
      val predictions = model.transformSync(test)
      val score = eval.evaluateSync(predictions, model.bestModel)
      println("Evaluated LR model: " + score)
    }

    // Helper function to create and evaluate a cross-validated random forest model.
    def makeRfModel(preprocData: DataFrame,
                    labelConverter: IndexToString,
                    featureVectorNames: Option[Array[String]] = None): Unit = {
      val Array(train, test) = preprocData.randomSplitSync(Array(0.7, 0.3))

      val rf = new RandomForestClassifier()
        .setLabelCol(FeatureVectorizer.indexed(labelCol))
        .setPredictionCol(predictionCol)
        .setFeaturesCol(featuresCol)
        .setNumTrees(20)

      val rfPipeline = new Pipeline()
        .setStages(Array(rf, labelConverter))

      val eval = new MulticlassClassificationEvaluator()
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

      val model = rfCv.fitSync(train)
      model.saveSync("animal_exploratory_rf")
      val predictions = model.transformSync(test)
      val score = eval.evaluateSync(predictions, model.bestModel)
      println("Evaluated RF model: " + score)
    }

    // For three feature-sets, create LR and RF models.
    val (preprocessedData, featureVectorNames, labelConverterOpt) = FeatureVectorizer(
      df.toDF(),
      Array("AnimalType", "SexuponOutcome", "SimpleBreed", "SimpleColor"),
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Array("AgeInYears"),
      Some((true, true))
    )(spark.sqlContext)
    makeLrModel(preprocessedData, labelConverterOpt.get, Some(featureVectorNames))
    makeRfModel(preprocessedData, labelConverterOpt.get, Some(featureVectorNames))

    val (preprocessedData2, featureVectorNames2, labelConverterOpt2) = FeatureVectorizer(
      df.toDF(),
      Array("AnimalType", "SexuponOutcome", "SimpleColor"),
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Array("AgeInYears"),
      Some((true, true))
    )(spark.sqlContext)
    makeLrModel(preprocessedData2, labelConverterOpt2.get, Some(featureVectorNames2))
    makeRfModel(preprocessedData2, labelConverterOpt2.get, Some(featureVectorNames2))

    val (preprocessedData3, featureVectorNames3, labelConverterOpt3) = FeatureVectorizer(
      df.toDF(),
      Array("AnimalType", "SexuponOutcome"),
      Array[String](),
      labelCol,
      predictionCol,
      featuresCol,
      Array("AgeInYears"),
      Some((true, true))
    )(spark.sqlContext)
    makeLrModel(preprocessedData3, labelConverterOpt3.get, Some(featureVectorNames3))
    makeRfModel(preprocessedData3, labelConverterOpt3.get, Some(featureVectorNames3))

    Timer.deactivate()
    Timer.writeTimingsToFile(config.outfile)
  }
}
