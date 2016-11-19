package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.MetricEvent
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.evaluation.{BinaryClassificationEvaluator, Evaluator, MulticlassClassificationEvaluator, RegressionEvaluator}
import org.apache.spark.sql.DataFrame

trait SyncableEvaluator {
  implicit class EvaluateSync(e: Evaluator) {
    def evaluateSync(df: DataFrame, m: Transformer)(implicit mdbs: Option[ModelDbSyncer]): Double = {
      val metricVal = e.evaluate(df)
      if (mdbs.isDefined) {
        val (metricName, labelCol, predictionCol) = SyncableEvaluator.getMetricNameLabelColPredictionCol(e)
        mdbs.get.buffer(MetricEvent(df, m, labelCol, predictionCol, metricName, metricVal.toFloat))
      }
      metricVal
    }
  }
}

object SyncableEvaluator {
  def getMetricNameLabelColPredictionCol(eval: Evaluator): (String, String, String) = eval match {
    case e: RegressionEvaluator => (e.getMetricName, e.getLabelCol, e.getPredictionCol)
    case e: BinaryClassificationEvaluator => (e.getMetricName, e.getLabelCol, e.getRawPredictionCol)
    case e: MulticlassClassificationEvaluator => (e.getMetricName, e.getLabelCol, e.getPredictionCol)
    case _ => ("Unknown metric", "Unknown label column", "Unknown prediction column")
  }
}
