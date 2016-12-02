package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableDataFrame, SyncableTransformer}
import modeldb.ModelDBService.FutureIface
import modeldb.TransformEventResponse
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.sql.DataFrame

/**
  * Event indicating that a Transformer transformed a DataFrame.
  *
  * @param transformer - The transformer doing the transforming.
  * @param inputDataframe - The input to the transformer.
  * @param outputDataframe - The output from the transformer.
  */
case class TransformEvent(transformer: Transformer,
                          inputDataframe: DataFrame,
                          outputDataframe: DataFrame) extends ModelDbEvent {
  def makeEvent(mdbs: ModelDbSyncer) = modeldb.TransformEvent(
    SyncableDataFrame(inputDataframe),
    SyncableDataFrame(outputDataframe),
    SyncableTransformer(transformer),
    mdbs.featureTracker.getInputCols(transformer),
    mdbs.featureTracker.getOutputCols(transformer),
    experimentRunId = mdbs.experimentRun.id
  )


  def associate(ter: modeldb.TransformEventResponse, mdbs: ModelDbSyncer) = {
    mdbs.associateObjectAndId(inputDataframe, ter.oldDataFrameId)
      .associateObjectAndId(outputDataframe, ter.newDataFrameId)
      .associateObjectAndId(transformer, ter.transformerId)
      .associateObjectAndId(this, ter.eventId)
  }

  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeTransformEvent(makeEvent(mdbs.get)))
    associate(res, mdbs.get)
  }
}