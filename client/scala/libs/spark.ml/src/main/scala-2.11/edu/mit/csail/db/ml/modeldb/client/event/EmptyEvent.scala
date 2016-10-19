package edu.mit.csail.db.ml.modeldb.client.event

import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import modeldb.ModelDBService.FutureIface

case class EmptyEvent(msg: String = "") extends ModelDbEvent {
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {}
}