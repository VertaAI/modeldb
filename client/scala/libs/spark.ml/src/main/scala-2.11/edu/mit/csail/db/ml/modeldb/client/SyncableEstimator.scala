package org.apache.spark.ml
// Note that we need to make this part of the org.apache.spark.ml package instead of the
// edu.mit.csail.db.ml.modeldb.client package. This is because we are writing new behavior for the Pipeline fit function.
// This requires us to use some classes (e.g. PipelineModel) that are private to the org.apache.spark.ml package.
// In order to access these classes, SyncableEstimator must be part of that package.

import edu.mit.csail.db.ml.modeldb.client._
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame

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
