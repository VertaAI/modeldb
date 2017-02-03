package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.sql.{DataFrame, DataFrameReader}

import scala.collection.mutable

trait SyncableDataFramePaths {
  /**
    * Implicit class for DataFrameReader. Basically, whenever the user the user reads a DataFrame, we associate
    * the DataFrame and the path it was read from.
    *
    * These basically provide *Sync variations of the load methods in DataFrameReader (e.g. jsonSync for json,
    * parquetSync for parquet).
    */
  implicit class DataFrameReaderSync(dfr: DataFrameReader) {
    def loadSync(path: String): DataFrame = {
      val df = dfr.load(path)
      SyncableDataFramePaths.setPath(df, path)
      df
    }

    def jsonSync(path: String): DataFrame = {
      val df = dfr.json(path)
      SyncableDataFramePaths.setPath(df, path)
      df
    }

    def csvSync(path: String): DataFrame = {
      val df = dfr.csv(path)
      SyncableDataFramePaths.setPath(df, path)
      df
    }

    def parquetSync(path: String): DataFrame = {
      val df = dfr.parquet(path)
      SyncableDataFramePaths.setPath(df, path)
      df
    }

    def textSync(path: String): DataFrame = {
      val df = dfr.text(path)
      SyncableDataFramePaths.setPath(df, path)
      df
    }

    def textFileSync(path: String): DataFrame = {
      val df = dfr.textFile(path).toDF()
      SyncableDataFramePaths.setPath(df, path)
      df
    }
  }
}

object SyncableDataFramePaths {
  /**
    * A cache that maps from DataFrame to the path that it was loaded from.
    */
  private val pathForDf = mutable.HashMap[DataFrame, String]()

  /**
    * Get the path that a given DataFrame was loaded from.
    * @param df - The DataFrame.
    * @return The path or None if the DataFrame does not have a path.
    */
  def getPath(df: DataFrame): Option[String] = pathForDf.get(df)

  /**
    * Set the path that a given DataFrame was loaded from.
    * @param df - The DataFrame.
    * @param path - The path.
    */
  def setPath(df: DataFrame, path: String): Unit = pathForDf.put(df, path)

  /**
    * Clear all mappings from DataFrame to path.
    */
  def clear(): Unit = pathForDf.clear()
}
