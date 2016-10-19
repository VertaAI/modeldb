package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Store a project on the server.
  */
case class ProjectEvent(project: modeldb.Project) extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeProjectEvent(modeldb.ProjectEvent(project)))
    mdbs.get.project = mdbs.get.project.copy(id = res.projectId)
  }
}
