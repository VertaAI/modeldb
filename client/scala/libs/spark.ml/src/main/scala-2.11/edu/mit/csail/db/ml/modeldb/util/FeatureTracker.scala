package org.apache.spark.ml

import org.apache.spark.ml.classification.OneVsRestModel
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.tuning.CrossValidatorModel
import org.apache.spark.sql._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * This class contains logic for getting the names of features in a given DataFrame.
  */
class FeatureTracker {
  /**
    * This map goes from DataFrame to a sequence of feature names in the DataFrame.
    */
  private val featuresForDf = new mutable.HashMap[DataFrame, Seq[String]]()

  /**
    * Indicate that the given DataFrame has the given features.
    * @param df - The DataFrame.
    * @param features - The features in the DataFrame.
    */
  def setFeaturesForDf(df: DataFrame, features: Seq[String]): Unit =
    if (features.nonEmpty) featuresForDf.put(df, features)

  /**
    * Get the features in a given DataFrame.
    * @param df - The DataFrame.
    * @return The features in the given DataFrame. If the DataFrame is unknown, None is returned.
    */
  def getFeaturesForDf(df: DataFrame): Option[Seq[String]] =
    featuresForDf.get(df)

  /**
    * Indicate that the two given DataFrames have the same features.
    * @param oldDf - The original DataFrame.
    * @param newDf - The new DataFrame. This will be marked with the same features as the original DataFrame.
    */
  def copyFeatures(oldDf: DataFrame, newDf: DataFrame): Unit =
    setFeaturesForDf(newDf, featuresForDf.getOrElseUpdate(oldDf, Seq.empty))

  /**
    * Apply a function to a Transformer after considering specific model types. For a regular Transformer, apply
    * the function directly. For PipelineModels, apply the function to each stage and concatenate the results. For
    * OneVsRestModels, apply the function to the first model. For CrossValidatorModels, apply the function to the
    * best model.
    * @param transformer - The transformer being considered.
    * @param fn - The function to apply.
    * @return THe resulting string sequence produced after applying the function.
    */
  private def handleSpecialModels(transformer: Transformer)
                                 (fn: Transformer => Seq[String]): Seq[String] = transformer match {
    case pm: PipelineModel => pm.stages.flatMap(fn)
    case ovr: OneVsRestModel => fn(ovr.models.head)
    case cv: CrossValidatorModel => fn(cv.bestModel)
    case _ => fn(transformer)
  }

  /**
    * Get the names of the feature columns in the given DataFrame. If the DataFrame is unknown, try to determine the
    * features by looking at the Transformer.
    * @param df - The DataFrame.
    * @param transformer - The Transformer.
    * @return The feature columns of the DataFrame. If the DataFrame is not known (i.e. not present in the cache), then
    *         the feature columns will be inferred by looking at the Transformer.
    */
  def getFeatureCols(df: DataFrame, transformer: Transformer): Seq[String] =
    getFeaturesForDf(df).getOrElse(getInputCols(transformer))

  /**
    * Get the input columns of a Transformer.
    * @param transformer - The Transformer.
    * @return The input columns (based on HasInputCol, HasInputCols, and HasFeaturesCol) of the Transformer.
    */
  def getInputCols(transformer: Transformer): Seq[String] = handleSpecialModels(transformer) { tr =>
    val inputCols = ArrayBuffer[String]()
    tr match {
      case x: HasFeaturesCol => inputCols.append(x.getFeaturesCol)
      case _ =>
    }

    tr match {
      case x: HasInputCols => inputCols.appendAll(x.getInputCols)
      case _ =>
    }

    tr match {
      case x: HasInputCol => inputCols.append(x.getInputCol)
      case _ =>
    }

    inputCols
  }

  /**
    * Get the label columns of a Transformer.
    * @param transformer - The Transformer.
    * @return The label columns (based on HasLabelCol) of the Transformer.
    */
  def getLabelColumns(transformer: Transformer): Seq[String] = handleSpecialModels(transformer) { tr =>
    val labelCols = ArrayBuffer[String]()

    tr match {
      case x: HasLabelCol => labelCols.append(x.getLabelCol)
      case _ =>
    }

    labelCols
  }

  /**
    * Get the output columns of a Transformer.
    * @param transformer - The Transformer.
    * @return The output columns (based on HasOutputCol, HasPredictionCol, and HasRawPredictionCol) of the Transformer.
    */
  def getOutputCols(transformer: Transformer): Seq[String] = handleSpecialModels(transformer) { tr =>
    val outputCols = ArrayBuffer[String]()
    tr match {
      case x: HasOutputCol => outputCols.append(x.getOutputCol)
      case _ =>
    }

    tr match {
      case x: HasPredictionCol => outputCols.append(x.getPredictionCol)
      case _ =>
    }

    tr match {
      case x: HasRawPredictionCol => outputCols.append(x.getRawPredictionCol)
      case _ =>
    }

    outputCols
  }
}