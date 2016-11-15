package org.apache.spark.ml

import org.apache.spark.ml.classification.OneVsRestModel
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.tuning.CrossValidatorModel
import org.apache.spark.sql._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class FeatureTracker {
  private val featuresForDf = new mutable.HashMap[DataFrame, Seq[String]]()

  def setFeaturesForDf(df: DataFrame, features: Seq[String]): Unit =
    if (features.nonEmpty) featuresForDf.put(df, features)

  def getFeaturesForDf(df: DataFrame): Option[Seq[String]] =
    featuresForDf.get(df)

  def copyFeatures(oldDf: DataFrame, newDf: DataFrame): Unit =
    setFeaturesForDf(newDf, featuresForDf.getOrElseUpdate(oldDf, Seq.empty))

  private def handleSpecialModels(transformer: Transformer)
                                 (fn: Transformer => Seq[String]): Seq[String] = transformer match {
    case pm: PipelineModel => pm.stages.flatMap(fn)
    case ovr: OneVsRestModel => fn(ovr.models.head)
    case cv: CrossValidatorModel => fn(cv.bestModel)
    case _ => fn(transformer)
  }

  def getFeatureCols(df: DataFrame, transformer: Transformer): Seq[String] =
    getFeaturesForDf(df).getOrElse(getInputCols(transformer))

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


  def getLabelColumns(transformer: Transformer): Seq[String] = handleSpecialModels(transformer) { tr =>
    val labelCols = ArrayBuffer[String]()

    tr match {
      case x: HasLabelCol => labelCols.append(x.getLabelCol)
      case _ =>
    }

    labelCols
  }

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