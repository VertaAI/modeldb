package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Event indicating that a Pipeline performed a transformation.
  *
  * @param transformEvents - The individual TransformEvents involved in the overall transformation performed by the
  *                        PipelineModel.
  */
case class PipelineTransformEvent(transformEvents: TransformEvent*) extends ModelDbEvent {
  /**
    * Store the PipelineTransformEvent on the server.
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storePipelineTransformEvent(transformEvents.map(_.makeEvent(mdbs.get))))

    res.zipWithIndex.foreach { pair =>
      transformEvents(pair._2).associate(pair._1, mdbs.get)
    }
  }
}