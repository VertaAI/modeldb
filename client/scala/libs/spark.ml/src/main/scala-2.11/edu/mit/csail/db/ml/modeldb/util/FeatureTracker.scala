package edu.mit.csail.db.ml.modeldb.util

import org.apache.spark.sql.DataFrame

import scala.collection.mutable

private case class Transformation(oldDf: DataFrame,
                                  newDf: DataFrame,
                                  inputCols: Seq[String],
                                  outputCols: Seq[String])

object FeatureTracker {
  private val transformationForDf = mutable.HashMap[DataFrame, Transformation]()

  def registerTransform(oldDf: DataFrame, newDf: DataFrame, inputCols: Seq[String], outputCols: Seq[String]): Unit =
    transformationForDf.put(newDf, Transformation(oldDf, newDf, inputCols, outputCols))


  def originalFeatures(df: DataFrame, features: Seq[String], depth: Int = Int.MaxValue): Seq[String] = {
    var currDf = df
    val featureSet = mutable.HashSet[String](features:_*)
    var depthSoFar = 0


    while (transformationForDf.contains(currDf) && depthSoFar < depth) {
      depthSoFar += 1
      val transformation = transformationForDf(currDf)
      transformation.outputCols.foreach(col => featureSet.remove(col))
      transformation.inputCols.foreach(col => featureSet.add(col))
      currDf = transformation.oldDf
    }

    featureSet.toSeq
  }

  def highestAncestor(df: DataFrame): DataFrame = {
    var currDf = df

    while (transformationForDf.contains(currDf)) {
      currDf = transformationForDf(currDf).oldDf
    }

    currDf
  }
}