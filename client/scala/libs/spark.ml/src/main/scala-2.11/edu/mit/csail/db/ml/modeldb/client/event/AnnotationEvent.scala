package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableDataFrame, SyncableTransformer}
import modeldb.ModelDBService.FutureIface
import org.apache.spark.ml.{PipelineStage, SyncableEstimator, Transformer}
import org.apache.spark.sql.DataFrame

/**
  * Event indicating that the user has created an annotation.
  * @param items - A sequence of Strings, DataFrames, Transformers, and PipelineStages.
  *              These constitute the annotation.
  */
case class AnnotationEvent(items: Any*) extends ModelDbEvent {
  // These are the currently supported annotation types.
  // These strings need to correspond to the strings used on the ModelDB server.
  object AnnotationTypes {
    val MESSAGE = "message"
    val DATAFRAME = "dataframe"
    val SPEC = "spec"
    val TRANSFORMER = "transformer"
  }

  /**
    * AnnotationEvents are made from AnnotationFragments. This is a convenience
    * function that creates an empty AnnotationFragment whose fields we can modify.
    * @return An empty annotation fragment with sensible defaults.
    */
  private def makeEmptyFragment = modeldb.AnnotationFragment(
    "",
    modeldb.DataFrame(numRows=1),
    modeldb.TransformerSpec(transformerType=""),
    modeldb.Transformer(transformerType=""),
    ""
  )

  /**
    * Stores the AnnotationEvent on the server.
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    // Create the fragments.
    val fragments = items.map {
      case df: DataFrame =>
        makeEmptyFragment.copy(`type`=AnnotationTypes.DATAFRAME, df=SyncableDataFrame(df))
      case message: String =>
        makeEmptyFragment.copy(`type`=AnnotationTypes.MESSAGE, message=message)
      case transformer: Transformer =>
        makeEmptyFragment.copy(`type`=AnnotationTypes.TRANSFORMER, transformer=SyncableTransformer(transformer))
      case estimator: PipelineStage =>
        makeEmptyFragment.copy(`type`=AnnotationTypes.SPEC, spec=SyncableEstimator(estimator))
    }

    // Store the annotation event.
    val res = Await.result(client.storeAnnotationEvent(modeldb.AnnotationEvent(
      fragments,
      experimentRunId = mdbs.get.experimentRun.id
    )))

    // Associate objects and IDs.
    (items zip res.fragmentResponses).foreach { case (item, response) =>
      if (!item.isInstanceOf[String]) {
        mdbs.get.associateObjectAndId(item, response.id)
      }
    }
  }
}