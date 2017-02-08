package org.apache.spark.ml

import java.util.Random

import com.github.fommil.netlib.F2jBLAS
import edu.mit.csail.db.ml.modeldb.client.{HasFitSync, ModelDbSyncer, SyncableEvaluator}
import edu.mit.csail.db.ml.modeldb.client.event.{CrossValidationFold, GridSearchCrossValidationEvent}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.DataFrame

import scala.collection.mutable.ArrayBuffer

/**
  * This trait defines an implicit class for CrossValidator that augments the class with the fitSync functions.
  * These functions will log a GridSearchCrossValidationEvent to the server.
  */
trait SyncableCrossValidator {
  /**
    * Implicit class for storing Cross validation results.
    *
    * cv: The cross validator object.
    */
  implicit class CrossValidatorFitSync(cv: CrossValidator) extends HasFitSync[CrossValidatorModel] {
    // The original Spark code uses this object.
    val f2jBLAS = new F2jBLAS

    /**
      * This overrides the behavior of the fit() method of CrossValidator. The important changes I've made are indicated
      * with a comment.
      */
    def customFit(df: DataFrame)(implicit mdbs: Option[ModelDbSyncer]): CrossValidatorModel = {
      val schema = df.schema
      cv.transformSchema(schema)
      val sparkSession = df.sparkSession
      val est = cv.getEstimator
      val eval = cv.getEvaluator
      val epm = cv.getEstimatorParamMaps
      val numModels = epm.length
      val metrics = new Array[Double](epm.length)
      val (metricName, labelCol, predictionCol) = SyncableEvaluator.getMetricNameLabelColPredictionCol(cv.getEvaluator)

      // Create a list of estimators - one for each param map.
      val estimators = epm.map(pm => est.copy(pm))

      // For each estimator, for each fold, we want to keep track of the model trained on that fold, the score, and the
      // validation set that we trained on.
      val foldsForEstimator: Map[PipelineStage, ArrayBuffer[CrossValidationFold]] =
        estimators
          .map(est => (est.asInstanceOf[PipelineStage], ArrayBuffer[CrossValidationFold]()))
          .toMap

      // Note that we need to generate a seed here for the splitting of the dataset.
      val seed = new Random().nextInt
      val splits = MLUtils.kFold(df.rdd, cv.getNumFolds, seed)

      splits.zipWithIndex.foreach { case ((training, validation), splitIndex) =>
        val trainingDataset = sparkSession.createDataFrame(training, schema).cache()
        val validationDataset = sparkSession.createDataFrame(validation, schema).cache()
        // If this is a Pipeline, we need to store a PipelineEvent first.
        // TODO: This isn't great that we need to fall the Pipeline fitSync here. We need to think of a better
        // way to do this, because it will log two FitEvents for the Pipeline. One simple approach may be to
        // store this Pipeline with fitSync, extract its ID, and pass that along with the
        // GridSearchCrossValidationEvent. Again, the reason we need to call fitSync here is because it's not sufficient
        // to store just a FitEvent for the Pipeline, we need to store all the stages too. The logic for doing that
        // is in the Pipeline's fitSync.
        val models = (est match {
          case p: Pipeline => SyncablePipeline.PipelineFitSync(p).fitSync(df, epm)
          case _ => est.fit(trainingDataset, epm)
        }).asInstanceOf[Seq[Model[_]]]

        trainingDataset.unpersist()
        var i = 0
        while (i < numModels) {
          val metric = eval.evaluate(models(i).transform(validationDataset, epm(i)))
          metrics(i) += metric
          val thisEstimator = estimators(i)
          val thisFold = CrossValidationFold(
            models(i).asInstanceOf[Transformer],
            trainingDataset,
            validationDataset,
            metric
          )
          foldsForEstimator(thisEstimator).append(thisFold)
          i += 1
        }
        validationDataset.unpersist()
      }
      f2jBLAS.dscal(numModels, 1.0 / cv.getNumFolds, metrics, 1)
      val (bestMetric, bestIndex) =
        if (eval.isLargerBetter) metrics.zipWithIndex.maxBy(_._1)
        else metrics.zipWithIndex.minBy(_._1)

      // Note that we call fitSync() on the estimator if it exposes a fitSync().
      val bestModel = (est match {
        case p: Pipeline => SyncablePipeline.PipelineFitSync(p).fitSync(df, epm(bestIndex))
        case _ => est.fit(df, epm(bestIndex))
      }).asInstanceOf[Model[_]]

      mdbs.get.buffer(GridSearchCrossValidationEvent(
        df,
        if (mdbs.get.shouldStoreGSCVE) foldsForEstimator else Map(),
        seed,
        eval,
        bestModel,
        estimators(bestIndex),
        cv.getNumFolds
      ))


      val cvModel = new CrossValidatorModel(cv.uid, bestModel, metrics).setParent(cv)
      cvModel.set(cvModel.estimator, cv.getEstimator)
        .set(cvModel.estimatorParamMaps, cv.getEstimatorParamMaps)
        .set(cvModel.evaluator, cv.getEvaluator)
        .set(cvModel.numFolds, cv.getNumFolds)
        .set(cvModel.seed, seed.toLong)
      cvModel
    }

    /**
      * Overrides fitSync and calls the customFit method above.
      */
    override def fitSync(df: DataFrame, pms: Array[ParamMap], featureVectorNames: Seq[String])
                        (implicit mdbs: Option[ModelDbSyncer]): Seq[CrossValidatorModel] = {
      mdbs.get.featureTracker.setFeaturesForDf(df, featureVectorNames)
      val models = if (pms.length == 0) {
        Array(customFit(df))
      } else {
        pms.map(pm => new CrossValidatorFitSync(cv.copy(pm)).customFit(df))
      }
      models
    }
  }

}