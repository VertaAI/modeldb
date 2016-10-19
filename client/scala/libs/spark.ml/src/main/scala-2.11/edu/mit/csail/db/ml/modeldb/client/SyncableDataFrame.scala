package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.sql.DataFrame
import java.util.Random

import edu.mit.csail.db.ml.modeldb.client.event.RandomSplitEvent


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

      mdbs.get.buffer(new RandomSplitEvent(
        m,
        weights,
        seed,
        splits
      ))

      mdbs.get.getFeaturesForDf(m) match {
        case Some(featureVectorNames) => splits.foreach(df => mdbs.get.setFeaturesForDf(df, featureVectorNames))
        case None => {}
      }
      splits
    }
  }
}

object SyncableDataFrame extends SyncableDataFrame {
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

    // Similar to above, we only compute the number of rows in the dataframe if the server has not seen this dataframe.
    val numRows = if (id != -1) {
      -1
    } else {
      df.count.toInt
    }

    val modeldbDf = modeldb.DataFrame(
      id,
      columns,
      numRows,
      tag=tag
    )
    modeldbDf
  }
}