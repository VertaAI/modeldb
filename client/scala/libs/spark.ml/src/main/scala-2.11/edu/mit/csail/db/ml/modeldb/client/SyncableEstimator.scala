package org.apache.spark.ml
// Note that we need to make this part of the org.apache.spark.ml package instead of the
// edu.mit.csail.db.ml.modeldb.client package. This is because we are writing new behavior for the Pipeline fit function.
// This requires us to use some classes (e.g. PipelineModel) that are private to the org.apache.spark.ml package.
// In order to access these classes, SyncableEstimator must be part of that package.

import edu.mit.csail.db.ml.modeldb.client._
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel}
import org.apache.spark.sql.DataFrame
import org.apache.spark.ml.param.shared._

/**
  * This trait contains an implicit class that augments an estimator
  * with fitSync functions.
  */
trait SyncableEstimator {
  /**
    * Implicit class that equips all Spark Estimators with the fitSync method.
    */
  implicit class EstimatorSync[M <: Model[M]](m: Estimator[M]) extends HasFitSync[M] {
    override def fitSync(df: DataFrame, pms: Array[ParamMap], featureVectorNames: Seq[String])
                        (implicit mdbs: Option[ModelDbSyncer]): Seq[M] =
      fitSync(m, df, pms, mdbs, featureVectorNames)
  }
}

object SyncableEstimator extends SyncableEstimator {

  /**
    * Create a sequence of ModelDB Hyperparameters from a Spark ParamMap.
    * @param pm - The ParamMap.
    * @return The Hyperparameters extracted from the ParamMap.
    */
  def extractHyperParameters(pm: ParamMap): Seq[modeldb.HyperParameter] = {
    pm.toSeq.map { (pair) =>
      // Note that a transformer spec should override the bounds [Float.MinValue, Float.MaxValue] as appropriate.
      modeldb.HyperParameter(pair.param.name,
        pair.value.toString,
        pair.value.getClass.getSimpleName,
        Float.MinValue,
        Float.MaxValue)
    }
  }

  /**
    * Gets the input column (or columns) for a given PipelineStage.
    * @param stage - The PipelineStage, it should not be a PipelineModel
    *              or Pipeline.
    * @return The sequence of input columns.
    */
  private def getInputColsHelper(stage: PipelineStage): Seq[String] = {
    val cols: Seq[String] = stage match {
      case ic: HasInputCol => Seq(ic.getInputCol)
      case ics: HasInputCols => ics.getInputCols
      case _ => Seq[String]()
    }

    // If there are no input cols, then this is a Model, so treat the features as the input cols.
    if (cols.isEmpty) {
      getFeatureCols(stage)
    } else {
      cols
    }
  }

  /**
    * Get the input column (or columns) for a PipelineStage.
    * @param stage - The PipelineStage (this may be a PipelineModel or Pipeline).
    * @return The sequence of input columns.
    */
  def getInputCols(stage: PipelineStage): Seq[String] = {
    val realStage = stage match {
      case cv: CrossValidatorModel => cv.bestModel
      case cv: CrossValidator => cv.getEstimator
      case ps: PipelineStage => ps
    }
    realStage match {
      case pm: PipelineModel => pm.stages.flatMap(ps => getInputCols(ps))
      case p: Pipeline => p.getStages.flatMap(ps => getInputCols(ps))
      case _ => getInputColsHelper(realStage)
    }
  }

  /**
    * Get the feature column (or columns) for a PipelineStage.
    * @param stage - The PipelineStage (this may be a PipelineModel or Pipeline).
    * @return The sequence of feature columns.
    */
  def getFeatureCols(stage: PipelineStage): Seq[String] = {
    val realStage = stage match {
      case cv: CrossValidator => cv.getEstimator
      case ps: PipelineStage => ps
    }
    realStage match {
      case fc: HasFeaturesCol => Seq(fc.getFeaturesCol)
      case p: Pipeline => p.getStages.flatMap(ps => getFeatureCols(ps) ++ getInputCols(ps))
      case _ => getInputCols(realStage)
    }
  }

  /**
    * Get the output column (or columns) for a PipelineStage.
    * @param stage The PipelineStage (this should NOT be a PipelineModel or Pipeline).
    * @return The sequence of output columns.
    */
  private def getOutputColsHelper(stage: PipelineStage): Seq[String] = {
    val cols: Seq[String] = stage match {
      case oc: HasOutputCol => Seq(oc.getOutputCol)
      case _ => Seq[String]()
    }

    // If there are no output cols, then this is a Model,
    // so treat the predictions as the output cols.
    if (cols.isEmpty) {
      getPredictionCols(stage)
    } else {
      cols
    }
  }

  /**
    * Get the output column (or columns) for a PipelineStage.
    * @param stage - The PipelineStage (this can be a PipelineModel or Pipeline).
    * @return The sequence of output columns.
    */
  def getOutputCols(stage: PipelineStage): Seq[String] = {
    val realStage = stage match {
      case cv: CrossValidatorModel => cv.bestModel
      case cv: CrossValidator => cv.getEstimator
      case ps: PipelineStage => ps
    }
    realStage match {
      case pm: PipelineModel => pm.stages.flatMap(ps => getOutputCols(ps))
      case p: Pipeline => p.getStages.flatMap(ps => getOutputCols(ps))
      case _ => getOutputColsHelper(realStage)
    }
  }

  /**
    * Get the prediction column (or columns) for the PipelineStage.
    * @param stage - The PipelineStage (this can be a PipelineModel or Pipeline).
    * @return The sequence of prediction columns.
    */
  def getPredictionCols(stage: PipelineStage): Seq[String] = {
    val realStage = stage match {
      case cv: CrossValidator => cv.getEstimator
      case ps: PipelineStage => ps
    }
    realStage match {
      case pc: HasPredictionCol => Seq(pc.getPredictionCol)
      case p: Pipeline => p.getStages.flatMap(ps => getPredictionCols(ps) ++ getOutputCols(ps))
      case _ => getOutputCols(realStage)
    }
  }

  /**
    * Get the label column (or columns) for the PipelineStage.
    * @param stage - The PipelineStage (this can be a Pipeline).
    * @return The sequence of label columns.
    */
  def getLabelColumns(stage: PipelineStage): Seq[String] = {
    val realStage = stage match {
      case cv: CrossValidator => cv.getEstimator
      case ps: PipelineStage => ps
    }
    realStage match {
      case lc: HasLabelCol => Seq(lc.getLabelCol)
      case p: Pipeline => p.getStages.flatMap(ps => getLabelColumns(ps))
      case _ => Seq[String]()
    }
  }

  /**
    * Create a TransformerSpec from an Estimator.
    *
    * @param estimator - The Estimator. We set it as a PipelineStage because we can't easily type parametrize all
    *                  Estimators.
    * @return A TransformerSpec.
    */
  def apply(estimator: PipelineStage)(implicit mdbs: Option[ModelDbSyncer]): modeldb.TransformerSpec = {
    // Default values.
    var id = mdbs.get.id(estimator).getOrElse(-1)
    val tag = mdbs.get.tag(estimator).getOrElse("")
    var name = estimator.getClass.getSimpleName
    var hyperparameters = extractHyperParameters(estimator.extractParamMap)

    // Override for custom classes.
    if (estimator.isInstanceOf[Pipeline]) {
      // We don't need to store hyperparameters for a Pipeline (which is Array[PipelineStage]), because those will be
      // stored in their own FitEvents.
      hyperparameters = Seq.empty[modeldb.HyperParameter]
    }

    modeldb.TransformerSpec(
      id,
      name,
      hyperparameters,
      tag
    )
  }
}
