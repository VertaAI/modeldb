package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Store experiment run on the server.
  */
case class ExperimentRunEvent(experimentRun: modeldb.ExperimentRun) extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeExperimentRunEvent(modeldb.ExperimentRunEvent(experimentRun)))
    mdbs.get.experimentRun = mdbs.get.experimentRun.copy(id = res.experimentRunId)
  }
}
