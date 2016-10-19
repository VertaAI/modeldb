package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableLinearModel}
import modeldb.ModelDBService.FutureIface
import org.apache.spark.ml
import org.apache.spark.ml.{Pipeline, PipelineModel, PipelineStage, Transformer}
import org.apache.spark.sql.DataFrame

import scala.collection.mutable.ArrayBuffer

/**
  * This is an abstract class that the two subclasses below extend.
  * It gives us a convenient way to distinguish between Transformers
  * and Estimators in the Pipeline.
  */
abstract class PipelineStageEvent

/**
  * Represents a Transformer in a Pipeline (when the Pipeline is fit).
  * @param input - The input fed to the Transformer.
  * @param output - The output produced by the Transformer.
  * @param transformer - The actual Transformer itself.
  */
case class TransformerPipelineStageEvent(input: DataFrame,
                                         output: DataFrame,
                                         transformer: Transformer) extends PipelineStageEvent

/**
  * Represents an Estimator in a Pipeline (when the Pipeline is fit).
  * @param input - The input fed into the Estimator.
  * @param output - The output produced by the Model that the Estimator produces.
  * @param estimator The actual Estimator itself.
  * @param model - The Model produced by the Estimator.
  */
case class FitPipelineStageEvent(input: DataFrame,
                                 output: DataFrame,
                                 estimator: PipelineStage,
                                 model: Transformer) extends PipelineStageEvent

/**
  * Event indicating that a Pipeline was fit.
  * @param pipeline - The Pipeline estimator.
  * @param pipelineModel - The model produced by the Pipeline.
  * @param inputDataFrame The DataFrame that the Pipeline is fit on.
  * @param stages - The stage of the Pipeline (see the case classes above).
  */
case class PipelineEvent(pipeline: Pipeline,
                         pipelineModel: PipelineModel,
                         inputDataFrame: DataFrame,
                         stages: Seq[PipelineStageEvent]) extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    // First make the fit event for the overall pipeline.
    val pipelineFit = FitEvent(pipeline, inputDataFrame, pipelineModel)

    // Populate the stages and columns.
    val transformStages = ArrayBuffer[(Int, TransformEvent)]()
    val fitStages = ArrayBuffer[(Int, FitEvent)]()
    val labelCols = ArrayBuffer[String]()
    val featureCols = ArrayBuffer[String]()
    val predictionCols = ArrayBuffer[String]()
    stages.zipWithIndex.foreach { case (stage, index) =>
      stage match {
        case TransformerPipelineStageEvent(in, out, t) =>
          transformStages.append((index, TransformEvent(t, in, out)))
          // We treat the input columns as features and output columns as predictions.
          predictionCols ++= ml.SyncableEstimator.getOutputCols(t)
          featureCols ++= ml.SyncableEstimator.getInputCols(t)
        case FitPipelineStageEvent(in, out, est, mod) =>
          // Create both a transform AND a fit event for an estimator in the pipeline.
          transformStages.append((index, TransformEvent(mod, in, out)))
          fitStages.append((index, FitEvent(est, in, mod)))
          // Now get the columns.
          predictionCols ++= ml.SyncableEstimator.getPredictionCols(est)
          labelCols ++= ml.SyncableEstimator.getLabelColumns(est)
          featureCols ++= mdbs.get.getFeaturesForDf(inputDataFrame).getOrElse(ml.SyncableEstimator.getFeatureCols(est))
      }
    }

    // Now log the event.
    val pipelineEvent = modeldb.PipelineEvent(
      pipelineFit.makeEvent(mdbs.get).copy(
        featureColumns = featureCols,
        predictionColumns = predictionCols,
        labelColumns = labelCols
      ),
      transformStages.map(pair => modeldb.PipelineTransformStage(pair._1, pair._2.makeEvent(mdbs.get))),
      fitStages.map(pair => modeldb.PipelineFitStage(pair._1, pair._2.makeEvent(mdbs.get))),
      projectId = mdbs.get.project.id,
      experimentRunId = mdbs.get.experimentRun.id
    )
    val res = Await.result(client.storePipelineEvent(pipelineEvent))

    // Associate the objects and IDs.
    pipelineFit.associate(res.pipelineFitResponse, mdbs.get)
    (res.transformStagesResponses zip transformStages).foreach { case (ter, (index, te)) =>
      te.associate(ter, mdbs.get)
    }
    (res.fitStagesResponses zip fitStages).foreach { case (fer, (index, fe)) =>
      SyncableLinearModel(pipelineModel.stages(index)) match {
        case Some(lm) => Await.result(client.storeLinearModel(fer.modelId, lm))
        case None => {}
      }
      fe.associate(fer, mdbs.get)
    }
  }
}