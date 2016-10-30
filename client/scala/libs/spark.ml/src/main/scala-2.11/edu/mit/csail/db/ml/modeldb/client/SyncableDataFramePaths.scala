package edu.mit.csail.db.ml.modeldb.client

import org.apache.spark.sql.{DataFrame, DataFrameReader}

import scala.collection.mutable

trait SyncableDataFramePaths {
  /**
    * Implicit class for DataFrameReader. Basically, whenever the user the user reads a DataFrame, we associate
    * the DataFrame from the path it was read from.
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
  private val pathForDf = mutable.HashMap[DataFrame, String]()
  def getPath(df: DataFrame): Option[String] = pathForDf.get(df)
  def setPath(df: DataFrame, path: String): Unit = pathForDf.put(df, path)
}
