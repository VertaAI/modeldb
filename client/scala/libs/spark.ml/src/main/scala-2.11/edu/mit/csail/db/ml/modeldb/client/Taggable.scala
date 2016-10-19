package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.ml.PipelineStage
import org.apache.spark.sql.DataFrame

/**
  * This trait exposes implicit classes that augments Spark objects
  * with the ability to receive tags. Tags are human-readable names
  * for objects (e.g. "Income dataset", "best regression model")
  * that are persisted in the ModelDB.
  */
trait Taggable {

  /**
    * This augments any PipelineStage objects with a tag function. This
    * includes Transformers, Models, Pipelines, CrossValidators,
    * and Estimators.
    */
  implicit class TaggablePipelineStage[M <: PipelineStage](m: M) {
    def tag(tagName: String)(implicit mdbs: Option[ModelDbSyncer]): M = {
      mdbs.get.associateObjectAndTag(m, tagName)
      m
    }
  }

  /**
    * This augments DataFrames with a tag function.
    */
  implicit class TaggableDataFrame(m: DataFrame) {
    def tag(tagName: String)(implicit mdbs: Option[ModelDbSyncer]) = {
      mdbs.get.associateObjectAndTag(m, tagName)
      m
    }
  }

}
