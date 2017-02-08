package edu.mit.csail.db.ml.modeldb.client

import com.twitter.util.Await
import modeldb.ModelDBService.FutureIface
import org.apache.spark.ml.Transformer

/**
  * A specific model is one for which we store additional metadata and associate it with the Transformer on ModelDB
  * server. Currently, we support TreeModels and LinearModels. This object exposes a method for storing the
  * specific models on the server.
  */
object SyncableSpecificModel {
  /**
    * Store a specific model for the given Transformer and associate it with the given modelId.
    * @param modelId - The ID of the model that the specific model should be associated with.
    * @param transformer - The Transformer representing the specific model.
    * @param client - The client that communicates with the server.
    * @param shouldStore - A boolean flag indicating whether this method should actually store the specific model,
    *                    or simply behave as a no-op.
    */
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
