package edu.mit.csail.db.ml.modeldb.util

import org.apache.spark.sql.DataFrame

import scala.collection.mutable

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

/**
  * Suppose that we have a DataFrame with feature "num". Now suppose that we transform this DataFrame to produce a new
  * DataFrame where we compute feature "scaledNum" from "num". Then, suppose we transform this new DataFrame and
  * produce a new column "squaredNum" from "scaledNum".
  *
  * A data scientist may be interested in knowing that "squaredNum" originated from "num". This FeatureTracker allows us
  * to compute the original feature if we have logged all the intermediate transformations.
  */
object FeatureTracker {
  private val transformationForDf = mutable.HashMap[DataFrame, Transformation]()

  /**
    * Indicate that a transformation occurred.
    */
  def registerTransform(oldDf: DataFrame, newDf: DataFrame, inputCols: Seq[String], outputCols: Seq[String]): Unit =
    transformationForDf.put(newDf, Transformation(oldDf, newDf, inputCols, outputCols))


  /**
    * Get the original feature set that produced the given set of features in the given DataFrame.
    * @param df - The DataFrame.
    * @param features - The set of features.
    * @param depth - The depth of ancestors we should consider (e.g. 1 = parents, 2 = parents and grandparents, 3 =
    *              parents and grandparents and great-grandparents). By default, this is Int.MaxValue.
    * @return The original feature set.
    */
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

  /**
    * Get the topmost (or highest) ancestor that the given DataFrame is descended from. The topmost ancestor is a
    * DataFrame that was not produced by any Transformation and that produced the given DataFrame after applying a
    * number of Transformations.
    * @param df - The descendant DataFrame whose ancestor we seek.
    * @return The highest ancestor DataFrame.
    */
  def highestAncestor(df: DataFrame): DataFrame = {
    var currDf = df

    while (transformationForDf.contains(currDf)) {
      currDf = transformationForDf(currDf).oldDf
    }

    currDf
  }
}