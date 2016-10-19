package edu.mit.csail.db.ml.modeldb.client

import modeldb.{LinearModel, LinearModelTerm}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.classification.LogisticRegressionModel
import org.apache.spark.ml.regression.LinearRegressionModel

object SyncableLinearModel {
  private def toLinearModel(lrm: LinearRegressionModel): LinearModel = {
    val normal = lrm.getSolver == "normal"

    // Get the training summary.
    val trainingSummary = if (lrm.hasSummary) Some(lrm.summary) else None

    // Get the intercept term.
    val interceptTerm = if (lrm.getFitIntercept)
      trainingSummary match {
        case Some(summary) => Some(LinearModelTerm(
          lrm.intercept,
          if (normal) Some(summary.tValues.last) else None,
          if (normal) Some(summary.coefficientStandardErrors.last) else None,
          if (normal) Some(summary.pValues.last) else None
        ))
        case None => Some(LinearModelTerm(lrm.intercept))
      }
    else
      None

    // Get the feature terms.
    val featureTerms = lrm.coefficients.toArray.zipWithIndex.map(pair => {
      val (coeff, index) = (pair._1, pair._2)
      trainingSummary match {
        case Some(summary) => LinearModelTerm(
          coeff,
          if (normal) Some(summary.tValues(index)) else None,
          if (normal) Some(summary.coefficientStandardErrors(index)) else None,
          if (normal) Some(summary.pValues(index)) else None
        )
        case None => LinearModelTerm(coeff)
      }
    })

    trainingSummary match {
      case Some(summary) => LinearModel(
        interceptTerm = interceptTerm,
        featureTerms = featureTerms,
        objectiveHistory = Some(summary.objectiveHistory),
        rmse = if (normal) Some(summary.rootMeanSquaredError) else None,
        explainedVariance = if (normal)  Some(summary.explainedVariance) else None,
        r2 = if (normal) Some(summary.r2) else None
      )
      case None => LinearModel(interceptTerm = interceptTerm, featureTerms = featureTerms)
    }
  }

  private def toLinearModel(lrm: LogisticRegressionModel): LinearModel = {
    // Get the training summary.
    val trainingSummary = if (lrm.hasSummary) Some(lrm.summary) else None

    // Get the intercept term.
    val interceptTerm = if (lrm.getFitIntercept) Some(LinearModelTerm(lrm.intercept)) else None

    // Get the feature terms.
    val featureTerms = lrm.coefficients.toArray.map(coeff => LinearModelTerm(coeff))

    trainingSummary match {
      case Some(summary) => LinearModel(
        interceptTerm = interceptTerm,
        featureTerms = featureTerms,
        objectiveHistory = Some(summary.objectiveHistory)
      )
      case None => LinearModel(interceptTerm = interceptTerm, featureTerms = featureTerms)
    }
  }

  def apply(x: Transformer): Option[LinearModel] = {
    x match {
      case linReg: LinearRegressionModel => Some(toLinearModel(linReg))
      case logReg: LogisticRegressionModel => Some(toLinearModel(logReg))
      case _ => None
    }
  }
}
