package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamPair
import org.apache.spark.ml.tuning.CrossValidatorModel
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.sql.DataFrame


trait SyncableTransformer {
  /**
    * Implicit class that equips all Spark Transformers with the transformSync method.
    */
  implicit class TransformerSync(m: Transformer) extends HasTransformSync {
    override def transformSync(df: DataFrame, pairs: Seq[ParamPair[_]])
                              (implicit mdbc: Option[ModelDbSyncer]): DataFrame =
      transformSync(m, df, pairs, mdbc)

    def saveSync()(implicit mdbs: Option[ModelDbSyncer]): Boolean = saveSync("")(mdbs)

    def saveSync(desiredFileName: String)(implicit mdbs: Option[ModelDbSyncer]): Boolean = {
      if (mdbs.isEmpty)
        false
      else
        m match {
          case cvm: CrossValidatorModel => cvm.bestModel.saveSync(desiredFileName)
          case w: MLWritable =>
            val filepath = mdbs.get.getFilepath(m, desiredFileName)
            w.write.overwrite().save(filepath)
            true
          case _ => false
        }
    }
  }
}

object SyncableTransformer extends SyncableTransformer {
  def apply(transformer: Transformer)
           (implicit mdbs: Option[ModelDbSyncer]): modeldb.Transformer = {
    val id = mdbs.get.id(transformer).getOrElse(-1)
    val tag = mdbs.get.tag(transformer).getOrElse("")
    val transformerType = transformer.getClass.getSimpleName
    transformer match {
      case _ => modeldb.Transformer(id, transformerType, tag=tag)
    }
  }
}
