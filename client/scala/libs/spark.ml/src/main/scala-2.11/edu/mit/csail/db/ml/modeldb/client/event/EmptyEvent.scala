package edu.mit.csail.db.ml.modeldb.client.event

import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

/**
  * This is a dummy Syncable Event that does nothing when it is synced.
  * @param msg A text message. This doesn't do anything, but we can't make a case class without some constructor
  *            argument.
  */
case class EmptyEvent(msg: String = "") extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {}
}