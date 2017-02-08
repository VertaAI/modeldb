package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Store a new experiment run on the server.
  * @param experimentRun - The experiment run to store.
  */
case class ExperimentRunEvent(experimentRun: modeldb.ExperimentRun) extends ModelDbEvent {
  /**
    * Store the experiment run on the server and set the current experiment run of the ModelDbSyncer to
    * the returned experiment run ID.
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeExperimentRunEvent(modeldb.ExperimentRunEvent(experimentRun)))
    mdbs.get.experimentRun = mdbs.get.experimentRun.copy(id = res.experimentRunId)
  }
}
