package org.apache.spark.ml

import edu.mit.csail.db.ml.modeldb.client.event._
import edu.mit.csail.db.ml.modeldb.client.{HasFitSync, HasTransformSync, ModelDbSyncer, SyncableTransformer}
import org.apache.spark.ml.param.{ParamMap, ParamPair}
import org.apache.spark.sql.DataFrame

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * This trait defines an implicit class for Pipelines that stores a Pipeline event on the server when the user
  * calls fitSync.
  */
trait SyncablePipeline {
  /**
    * Implicit class for storing pipelines.
    */
  implicit class PipelineFitSync(pipeline: Pipeline) extends HasFitSync[PipelineModel] {

    /**
      * This is a customized version of the fit method which basically just copies code from fit()
      * from Spark's Pipeline.scala with some minor modifications (see comments in the code).
      */
    def customFit(df: DataFrame)(implicit mdbs: Option[ModelDbSyncer]): PipelineModel = {
      val stageEvents = ArrayBuffer[PipelineStageEvent]()

      pipeline.transformSchema(df.schema)
      val theStages = pipeline.getStages
      var curDataset = df
      val transformers = ListBuffer.empty[Transformer]
      theStages.view.foreach { stage =>
        val oldDf = curDataset
        val transformer = stage match {
          case estimator: Estimator[_] =>
            val model = estimator.fit(oldDf)
            val newDf = model.transform(oldDf)
            if (mdbs.isDefined)
              mdbs.get.featureTracker.copyFeatures(oldDf, df)
            stageEvents.append(FitPipelineStageEvent(oldDf, newDf, estimator, model))
            curDataset = newDf
            model
          case transformer: Transformer =>
            val newDf = transformer.transform(oldDf)
            stageEvents.append(TransformerPipelineStageEvent(oldDf, newDf, transformer))
            curDataset = newDf
            transformer
          case _ =>
            throw new IllegalArgumentException(
              s"Do not support stage $stage of type ${stage.getClass}")
        }
        transformers += transformer
      }

      val model = new PipelineModel(pipeline.uid, transformers.toArray).setParent(pipeline)
      if (mdbs.isDefined) mdbs.get.buffer(PipelineEvent(pipeline, model, df, stageEvents))
      model
    }

    override def fitSync(df: DataFrame, pms: Array[ParamMap], featureVectorNames: Seq[String])
                        (implicit mdbs: Option[ModelDbSyncer]): Seq[PipelineModel] = {
      if (mdbs.isDefined) mdbs.get.featureTracker.setFeaturesForDf(df, featureVectorNames)
      if (pms.length == 0) {
        Array(customFit(df))
      } else {
        pms.map(pm => new PipelineFitSync(pipeline.copy(pm)).customFit(df))
      }
    }
  }

  implicit class PipelineTransformSync(pm: PipelineModel) extends HasTransformSync {
    override def transformSync(df: DataFrame, pairs: Seq[ParamPair[_]])
                              (implicit mdbc: Option[ModelDbSyncer]): DataFrame = {
      pm.transformSchema(df.schema)
      // The code below has not been erased because I need it in order evaluate the performance of an optimization.
//      pm.stages.foldLeft(df)((cur, transformer) => transformer match {
//        case pm: PipelineModel => pm.transformSync(cur, pairs)(mdbc)
//        case _ => SyncableTransformer.TransformerSync(transformer).transformSync(cur, pairs)(mdbc)
//      })
      val transformEvents = ArrayBuffer[TransformEvent]()
      val finalResult = pm.stages.foldLeft(df)((cur, transformer) => transformer match {
        case pm: PipelineModel => pm.transformSync(cur, pairs)(mdbc)
        case _ =>
          val result =
            if (pairs.isEmpty)
            transformer.transform(cur)
          else if (pairs.size == 1)
            transformer.transform(cur, pairs.head)
          else
            transformer.transform(cur, pairs.head, pairs.tail:_*)
          transformEvents.append(TransformEvent(transformer, cur, result))
          result
      })
      if (mdbc.isDefined)
        mdbc.get.buffer(PipelineTransformEvent(transformEvents:_*))
      finalResult
    }
  }
}

object SyncablePipeline extends SyncablePipeline {

}