package edu.mit.csail.db.ml.modeldb.util

import org.apache.spark.sql.DataFrame

/**
  * Represents a transformation that turns an old DataFrame into a new DataFrame.
  * @param oldDf - The original DataFrame.
  * @param newDf - The DataFrame produced by the transformation.
  * @param inputCols - The input columns used by the transformation.
  * @param outputCols - The output columns produced by the transformation.
  */
private case class Transformation(oldDf: DataFrame,
                                  newDf: DataFrame,
                                  inputCols: Seq[String],
                                  outputCols: Seq[String])

