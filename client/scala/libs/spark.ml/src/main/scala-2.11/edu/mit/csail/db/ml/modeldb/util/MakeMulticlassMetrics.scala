package edu.mit.csail.db.ml.modeldb.util

import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql.DataFrame

object MakeMulticlassMetrics {
  /**
    * Creates a MulticlassMetrics object for the given DataFrame.
    * @param df - The DataFrame.
    * @param labelCol - The column containing the labels.
    * @param predictionCol - The column containing the predictions.
    * @return The MulticlassMetrics.
    */
  def apply(df: DataFrame, labelCol: String, predictionCol: String): MulticlassMetrics = {
    val predAndLabels = df.select(predictionCol, labelCol).rdd.map(row => (row.getDouble(0), row.getDouble(1)))
    new MulticlassMetrics(predAndLabels)
  }
}