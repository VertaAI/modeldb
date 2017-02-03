package edu.mit.csail.db.ml.modeldb.client

import java.util.Random

import edu.mit.csail.db.ml.modeldb.client.event.RandomSplitEvent
import org.apache.spark.sql.DataFrame

import scala.collection.mutable


/**
  * This trait defines an implicit class that
  * augments a DataFrame with the randomSplitSync functions
  * that log a RandomSplitEvent to the ModelDB.
  */
trait SyncableDataFrame {
  implicit class DataFrameSync(m: DataFrame) {
    /**
      * Randomly split a DataFrame into pieces (see DataFrame.randomSplit).
      * This function will generate a random seed for you.
      * @param weights - The weights used for splitting.
      * @param mdbs - The ModelDB Syncer.
      * @return The pieces of the DataFrame.
      */
    def randomSplitSync(weights: Array[Double])(implicit mdbs: Option[ModelDbSyncer]): Array[DataFrame] =
      randomSplitSync(weights, new Random().nextLong)

    /**
      * Randomly split a DataFrame into pieces (see DataFrame.split).
      * @param weights - The weights used for splitting.
      * @param seed - The seed to use for splitting.
      * @param mdbs - The ModelDB Syncer.
      * @return The pieces of the DataFrame.
      */
    def randomSplitSync(weights: Array[Double], seed: Long)(implicit mdbs: Option[ModelDbSyncer]): Array[DataFrame] = {
      val splits = m.randomSplit(weights, seed)

      if (mdbs.isDefined) {
        mdbs.get.buffer(RandomSplitEvent(m, weights, seed, splits))
        splits.foreach(df => mdbs.get.featureTracker.copyFeatures(m, df))
      }

      // We can think of random splitting as performing n transformations from the original DataFrame to
      // n smaller DataFrames where there are no input features or output features.
      // Thus, we will feed this information to the FeatureTracker so that each of the splits know that they
      // originated from the same DataFrame and so that they remember its features.
      SyncableDataFramePaths.getPath(m) match {
        case Some(path) => splits.foreach(spl => SyncableDataFramePaths.setPath(spl, path))
        case None => {}
      }
      splits
    }
  }
}

object SyncableDataFrame extends SyncableDataFrame {
  /**
    * Caches the number of rows in a given DataFrame. A cache is used because counting the number of rows in a DataFrame
    * requires a sequential scan of the DataFrame, which can be expensive.
    *
    * The key is the DataFrame and the value is the number of rows in the DataFrame.
    */
  private def rowCountForDf =  mutable.Map[DataFrame, Long]()

  /**
    * Convert a Spark DataFrame into a modeldb.DataFrame.
    * @param df - The Spark DataFrame.
    * @param mdbs - The syncer (used for the id mapping).
    * @return A modeldb.DataFrame representing the Spark DataFrame.
    */
  def apply(df: DataFrame)(implicit mdbs: Option[ModelDbSyncer]): modeldb.DataFrame = {
    val id = mdbs.get.id(df).getOrElse(-1)
    val tag = mdbs.get.tag(df).getOrElse("")

    // If this dataframe already has an ID, the columns are already stored on the server, so we leave them empty.
    val columns = if (id != -1) {
      Seq[modeldb.DataFrameColumn]()
    } else {
      df.schema.map(field => modeldb.DataFrameColumn(field.name, field.dataType.simpleString))
    }

    // If the server has seen this DataFrame or if the ModelDBSyncer is configured not to count the rows, don't
    // count the rows.
    val numRows = if (id != -1 || !mdbs.get.shouldCountRows) {
      -1
    } else if (rowCountForDf.contains(df)) {
      rowCountForDf(df).toInt
    } else {
      val count = df.count // This is a performance intensive operation.
      rowCountForDf.put(df, count)
      count.toInt
    }

    val modeldbDf = modeldb.DataFrame(
      id,
      columns,
      numRows,
      tag=tag,
      filepath = SyncableDataFramePaths.getPath(df)
    )

    modeldbDf
  }
}