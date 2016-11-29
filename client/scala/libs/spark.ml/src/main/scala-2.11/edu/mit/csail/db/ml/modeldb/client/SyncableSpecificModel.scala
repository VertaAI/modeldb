package edu.mit.csail.db.ml.modeldb.client

import com.twitter.util.Await
import modeldb.ModelDBService.FutureIface
import org.apache.spark.ml.Transformer

object SyncableSpecificModel {
  def apply(modelId: Int, transformer: Transformer, client: Option[FutureIface], shouldStore: Boolean): Unit = {
    if (client.isDefined && shouldStore) {
      val cli = client.get
      SyncableLinearModel(transformer) match {
        case Some(m) => Await.result(cli.storeLinearModel(modelId, m))
        case None => {}
      }
      SyncableTreeModel(transformer) match {
        case Some(m) => Await.result(cli.storeTreeModel(modelId, m))
        case None => {}
      }
    }
  }
}
