package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableDataFrame}
import modeldb.ModelDBService.FutureIface
import org.apache.spark.sql.DataFrame

/**
  * Event indicating that a DataFrame was split into smaller DataFrames.
  *
  * @param dataframe - The DataFrame that was split.
  * @param weights - The weights used to split the DataFrame into
  *                smaller pieces. For example: Array(0.5, 0.5, 1) splits
  *                the DataFrame into three pieces, where the first two
  *                pieces are half as large as the third piece.
  * @param seed - The seed for the random number generator used for the splitting.
  * @param result - The pieces produced by the splitting process. There should
  *               be one entry here for each entry in weights.
  */
case class RandomSplitEvent(dataframe: DataFrame,
                            weights: Array[Double],
                            seed: Long,
                            result: Array[DataFrame]) extends ModelDbEvent {
  def makeEvent(mdbs: ModelDbSyncer): modeldb.RandomSplitEvent = {
    modeldb.RandomSplitEvent(
      SyncableDataFrame(dataframe),
      weights,
      seed,
      result.map(df => SyncableDataFrame(df)),
      experimentRunId = mdbs.experimentRun.id
    )
  }

  def associate(res: modeldb.RandomSplitEventResponse, mdbs: ModelDbSyncer) = {
    mdbs.associateObjectAndId(dataframe, res.oldDataFrameId)
    result.zipWithIndex.foreach { case (df, i) =>
      mdbs.associateObjectAndId(result(i), res.splitIds(i))
    }
    mdbs.associateObjectAndId(this, res.splitEventId)
  }

  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val event = makeEvent(mdbs.get)
    val res = Await.result(client.storeRandomSplitEvent(event))
    associate(res, mdbs.get)
  }
}