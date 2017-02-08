package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.TransformEvent
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{ParamMap, ParamPair}
import org.apache.spark.sql.DataFrame

/**
  * This trait augments a Transformer with the transformSync functions that log a
  * TransformEvent to the ModelDB after transforming a DataFrame.
  *
  * The trait exposes methods that resemble the transform method of a Spark Transformer.
  */
trait HasTransformSync {

  /**
    * This function should be used by the implementing class to
    * handle the transformSync(DataFrame, Seq[ParamPair[_]]) function.
    *
    * @param transformer - The Transformer on which transformSync is called.
    * @param df - The DataFrame being transformed.
    * @param pairs - The ParamPairs (may be empty) to use for transformation.
    * @param mdbs - The ModelDB Syncer.
    * @return The transformed DataFrame.
    */
  def transformSync(transformer: Transformer,
                    df: DataFrame,
                    pairs: Seq[ParamPair[_]],
                    mdbs: Option[ModelDbSyncer]): DataFrame = {
    val result = if (pairs.isEmpty)
      transformer.transform(df)
    else if (pairs.length == 1)
      transformer.transform(df, pairs.head)
    else
      transformer.transform(df, pairs.head, pairs.tail:_*)

    if (mdbs.isDefined) mdbs.get.buffer(TransformEvent(transformer, df, result))
    SyncableDataFramePaths.getPath(df) match {
      case Some(path) => SyncableDataFramePaths.setPath(result, path)
      case None => {}
    }
    result
  }

  def transformSync(df: DataFrame, pairs: Seq[ParamPair[_]])(implicit mdbc: Option[ModelDbSyncer]): DataFrame

  def transformSync(df: DataFrame, firstParamPair: ParamPair[_], otherParamPairs: ParamPair[_]*)
                   (implicit mdbc: Option[ModelDbSyncer]): DataFrame =
    transformSync(df,  Seq(firstParamPair) ++ otherParamPairs)(mdbc)

  def transformSync(df: DataFrame, paramMap: ParamMap)(implicit mdbc: Option[ModelDbSyncer]): DataFrame =
    transformSync(df, paramMap.toSeq)(mdbc)

  def transformSync(df: DataFrame)(implicit mdbc: Option[ModelDbSyncer]): DataFrame =
    transformSync(df, Seq[ParamPair[_]]())(mdbc)
}
