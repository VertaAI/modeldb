package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.MetricEvent
import org.apache.spark.ml.{Model, Transformer}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql.DataFrame

// TODO: We should see if this is still necessary. We may be able to remove it because proper MetricEvent
// logging happens with evaluateSync. Also, The MulticlassMetrics class is part of the old Spark RDD API, rather than
// the new Spark DataFrame API.
/**
  * Represents metrics that can be computed and synced to the ModelDB.
  */
object SyncableMetrics {
  /**
    * Create a Spark MulticlassMetrics object from the given data and model
    * and log MetricEvents to the ModelDB.
    *
    * @param model - The Model that is being evaluated.
    * @param df - The DataFrame we are evaluating.
    * @param labelCol - The column in the DataFrame above that contains the actual labels.
    * @param predictionCol - The column that Model will produce when it transforms
    *                      the DataFrame.
    * @param mdbs - The ModelDB Syncer.
    * @return The MulticlassMetrics object that is produced.
    */
  def ComputeMulticlassMetrics(model: Transformer,
                               df: DataFrame,
                               labelCol: String,
                               predictionCol: String)
                              (implicit mdbs: Option[ModelDbSyncer]): MulticlassMetrics = {
    // We need to convert this into an RDD because that's what MulticlassMetrics
    // expects.
    val rdd = df.select(df.col(predictionCol), df.col(labelCol)).rdd.map{ (row) =>
      val (predicted: Double, actual: Double) =
        (row(0).toString.toDouble, row(1).toString.toDouble)
      (predicted, actual)
    }

    // Create the object.
    val metrics = new MulticlassMetrics(rdd)

    // We compute three metrics and log them to the ModelDB.
    val metricMap = Map[String, Double](
      "precision" -> metrics.precision,
      "recall" -> metrics.recall,
      "fMeasure" -> metrics.fMeasure
    )

    // Create a MetricEvent for each.
    metricMap.foreach { case (name, value) =>
        mdbs.get.buffer(new MetricEvent(df, model, labelCol, predictionCol, name, value.toFloat))
    }
    metrics
  }
}