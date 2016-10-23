package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Store experiment run on the server.
  */
case class ExperimentEvent(experiment: modeldb.Experiment) extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeExperimentEvent(modeldb.ExperimentEvent(experiment)))
    mdbs.get.experiment = mdbs.get.experiment.copy(id = res.experimentId)
  }
}
