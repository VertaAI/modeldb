package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.FitEvent
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.sql.DataFrame

/**
  * This trait augments a model with fitSync functions that log a FitEvent on the
  * ModelDB after fitting the model. The functions are:
  *
  * fitSync(DataFrame)
  * fitSync(DataFrame, ParamMap)
  * fitSync(DataFrame, Array[ParamMap])
  *
  * functions for an estimator.
  * These functions will fit models and record a FitEvent to the ModelDbSyncer.
  *
  * @tparam T - The type of the model.
  */
trait HasFitSync[T <: Model[T]] {
  /**
    * This function should be used by the implementing class to
    * handle the fitSync(DataFrame, Array[ParamMap]) function.
    *
    * @param estimator - The Estimator on which fitSync is called.
    * @param df - The DataFrame being fit.
    * @param pms - The ParamMaps (may be empty) to use for fitting.
    * @param mdbs - The Model DB syncer.
    * @return The trained models
    */
  def fitSync(estimator: Estimator[T],
              df: DataFrame,
              pms: Array[ParamMap],
              mdbs: Option[ModelDbSyncer],
              featureVectorNames: Seq[String]) = {
    // Associate the feature vector names with the dataframe.
    if (mdbs.isDefined) mdbs.get.featureTracker.setFeaturesForDf(df, featureVectorNames)

    // Train the models.
    val models = if (pms.length == 0) Seq(estimator.fit(df)) else estimator.fit(df, pms)

    // Turn the ParamMaps into Syncables.
    val pmsSync = if (pms.length == 0) Seq() else pms

    // Record a FitEvent in the event syncer.
    if (mdbs.isDefined) models.foreach((model) => mdbs.get.buffer(FitEvent(estimator, df, model)))

    // Return the models.
    models
  }

  // Define the fitSync methods.
  def fitSync(df: DataFrame, pms: Array[ParamMap], featureVectorNames: Seq[String])
             (implicit mdbc: Option[ModelDbSyncer]): Seq[T]
  def fitSync(df: DataFrame, pm: ParamMap, featureVectorNames: Seq[String])
             (implicit mdbc: Option[ModelDbSyncer]): T = fitSync(df, Array(pm), featureVectorNames).head
  def fitSync(df: DataFrame, featureVectorNames: Seq[String])
             (implicit mdbc: Option[ModelDbSyncer]): T = fitSync(df, Array[ParamMap](), featureVectorNames).head
  def fitSync(df: DataFrame, pms: Array[ParamMap])
             (implicit mdbc: Option[ModelDbSyncer]): Seq[T] = fitSync(df, pms, Seq.empty[String])(mdbc)
  def fitSync(df: DataFrame, pm: ParamMap)
             (implicit mdbc: Option[ModelDbSyncer]): T = fitSync(df, Array(pm), Seq.empty[String]).head
  def fitSync(df: DataFrame)
             (implicit mdbc: Option[ModelDbSyncer]): T = fitSync(df, Array[ParamMap](), Seq.empty[String]).head
}