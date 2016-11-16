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
 */
import edu.mit.csail.db.ml.modeldb.util._
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, NewOrExistingProject, DefaultExperiment, NewExperimentRun, SyncableMetrics}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.sql.functions.udf
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.{LogisticRegressionModel, LogisticRegression, OneVsRest}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}

object Main {
  def run(pathToData: String): Unit = {
    ModelDbSyncer.setSyncer(
      new ModelDbSyncer(projectConfig = NewOrExistingProject(
        "Animal Shelter Outcomes",
        "hsubrama@mit.edu",
        "Attempt to predict outcome for cats and dogs in shelters."
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun)
    )

    // We'll read the AgeUponOutcome and convert that to the number of years.
    val parseAge = udf((ageStr: String) => {
        if (ageStr == "Unknown") {
            0
        } else {
            val split = ageStr.split(" ")
            val age = split(0).toInt
            val numPerYear = split(1) match {
                case "days" | "day" => 365
                case "month" | "months" => 12
                case "week" | "weeks" => 52
                case "years" | "year" => 1
                case _ => 1
            }
            age / 1.0 / numPerYear
        }
    })

    // Instead of using all the breeds, we'll use the give most common breeds.
    val groupBreed = udf((breedStr: String) => 
        if (Set("Domestic Shorthair Mix", 
                "Pit Bull Mix", 
                "Chihuahua Shorthair Mix", 
                "Labrador Retriever Mix",
                "Domestic Medium Hair Mix").contains(breedStr))
            breedStr
        else
            "other"
    )

    // Instead of using all the colors, we'll use the ten most common colors.
    val groupColor = udf((colorStr: String) =>
        if (Set("Black/White",
                "Black",
                "Brown Tabby",
                "Brown Tabby/White",
                "White",
                "Brown/White",
                "Orange Tabby",
                "Tan/White",
                "Tricolor",
                "Blue/White").contains(colorStr))
            colorStr
        else
            "other"
    )

    // Read the data into a DataFrame.
    val df = spark
        .read
        .option("header", true)
        .option("inferSchema", true)
        .option("ignoreLeadingWhiteSpace", true)
        .option("ignoreTrailingWhiteSpace", true)
        .option("nullValue", "Unknown")
        .option("dateFormat", "yyyy-MM-dd")
        .csv(pathToData)
        .withColumn("AgeInYears", parseAge($"AgeuponOutcome"))
        .withColumn("SimpleBreed", groupBreed($"Breed"))
        .withColumn("SimpleColor", groupColor($"Color"))
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
        .setNumTrees(200)
        
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

