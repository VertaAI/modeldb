package edu.mit.csail.db.ml.modeldb.client.event

import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * Events that can be synced to the ModelDb must subclass this class.
  */
abstract class ModelDbEvent {
  /**
    * Store this event on the ModelDb.
    *
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit
}