package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Stores a new project on the server.
  * @param project - The project to store.
  */
case class ProjectEvent(project: modeldb.Project) extends ModelDbEvent {
  /**
    * Store the project on the server.
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val res = Await.result(client.storeProjectEvent(modeldb.ProjectEvent(project)))
    mdbs.get.project = mdbs.get.project.copy(id = res.projectId)
  }
}
