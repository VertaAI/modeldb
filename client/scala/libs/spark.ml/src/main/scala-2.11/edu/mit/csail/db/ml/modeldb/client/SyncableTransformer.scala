package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamPair
import org.apache.spark.sql.DataFrame


trait SyncableTransformer {
  /**
    * Implicit class that equips all Spark Transformers with the transformSync method.
    */
  implicit class TransformerSync(m: Transformer) extends HasTransformSync {
    override def transformSync(df: DataFrame, pairs: Seq[ParamPair[_]])
                              (implicit mdbc: Option[ModelDbSyncer]): DataFrame =
      transformSync(m, df, pairs, mdbc)
  }
}

object SyncableTransformer extends SyncableTransformer {
  def apply(transformer: Transformer)
           (implicit mdbs: Option[ModelDbSyncer]): modeldb.Transformer = {
    val id = mdbs.get.id(transformer).getOrElse(-1)
    val tag = mdbs.get.tag(transformer).getOrElse("")
    val transformerType = transformer.getClass.getSimpleName
    transformer match {
      case _ => modeldb.Transformer(id, Seq.empty[Double], transformerType, tag=tag)
    }
  }
}
