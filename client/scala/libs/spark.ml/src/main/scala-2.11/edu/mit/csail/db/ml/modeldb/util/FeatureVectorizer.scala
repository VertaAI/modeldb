package edu.mit.csail.db.ml.modeldb.util

import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.event.TransformEvent
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

/**
  * Contains logic for combining columns of a DataFrame into a feature vector.
  *
  * This is a convenience class that lets us perform common pre-processing steps on a DataFrame.
  */
object FeatureVectorizer {
  /**
    * Name of the indexed variant of a column,
    */
  def indexed(col: String): String = s"indexed_$col"

  /**
    * Name of the vector variant of a column.
    */
  def vec(col: String): String = s"vec_$col"

  /**
    * Name of the converted variant (i.e. prediction converted back to one of the labels) of a column.
    */
  def converted(col: String): String = s"converted_$col"

  /**
    * Name of the scaled variant of the column.
    */
  def scaled(col: String): String = s"scaled_$col"

  /**
    * Create a feature vector from the given columns. This will be done by string indexing and one-hot encoding
    * all the categorical columns and then concatenating them together with the numerical columns.
    * @param origDf - The input DataFrame.
    * @param categorialCols - Columns with categorical data.
    * @param numericalCols - Columns with numerical data.
    * @param labelCol - The label column, it will be string indexed.
    * @param predictionCol - The prediction column, the label converter will be generated to convert this from a
    *                      Double back into a String label.
    * @param featuresCol - The column that should contain the feature vectors.
    * @param scaledCols - The columns that should be scaled.
    * @param scaler - If you'd like to scale the numerical columns, provide (withStd boolean, withMean boolean).
    * @return (The output dataframe, An array where the ith index is the name of the ith entry of a feature vector,
    *         if a label column was given, this is a label converter that, when applied to a dataframe, will convert the
    *         prediction column back into String labels).
    */
  def apply(origDf: DataFrame,
            categorialCols: Array[String] = Array(),
            numericalCols: Array[String] = Array(),
            labelCol: String = "",
            predictionCol: String = "prediction",
            featuresCol: String = "features",
            scaledCols: Array[String] = Array(),
            scaler: Option[Tuple2[Boolean, Boolean]] = None)
           (implicit sqlContext: SQLContext): (DataFrame, Array[String], Option[IndexToString]) = {

    var df = origDf

    // First string index and one-hot encode all the categorical cols.
    val indexers = categorialCols.map(col =>
      new StringIndexer()
        .setInputCol(col)
        .setOutputCol(indexed(col))
    )

    val oneHotEncoders = categorialCols.map(col =>
      new OneHotEncoder()
        .setInputCol(indexed(col))
        .setOutputCol(vec(col))
        .setDropLast(false)
    )

    // Now we'll scale the numerical columns.

    // First, get scaling parameters.
    val (withStd, withMean) = scaler.getOrElse((false, false))

    // We'll assemble all the numeric columns into a vector and then scale that vector.
    // We can't put this into a Pipeline because our StandardScaler requires dense vectors, but our VectorAssembler
    // produces sparse vectors. Converting from sparse to dense requires a UDF, which does not work in a Pipeline.
    val actualNumericalCols = if (scaler.isDefined) {
      val SCALED_ASSEMBLED = "scaledAssembled"

      val assembler =  new VectorAssembler()
        .setInputCols(scaledCols)
        .setOutputCol(SCALED_ASSEMBLED)

      val scaler = new StandardScaler()
        .setInputCol(SCALED_ASSEMBLED)
        .setOutputCol(scaled(SCALED_ASSEMBLED))
        .setWithMean(withMean)
        .setWithStd(withStd)

      val assembledDf = assembler.transformSync(origDf)
      val denseAssembledDf = sqlContext.createDataFrame(assembledDf.rdd.map { row =>
        Row.fromSeq(row.toSeq.dropRight(1) ++ Array(row.toSeq.last.asInstanceOf[Vector].toDense))
      }, StructType(assembledDf.schema))
      if (ModelDbSyncer.syncer.isDefined)
        ModelDbSyncer.syncer.get.buffer(TransformEvent(assembler, assembledDf, denseAssembledDf))

      val scalerModel = scaler.fitSync(denseAssembledDf)
      val scaledDf = scalerModel.transformSync(denseAssembledDf)
      df = sqlContext.createDataFrame(scaledDf.rdd.map { row =>
        Row.fromSeq(row.toSeq.dropRight(1) ++ Array(row.toSeq.last.asInstanceOf[Vector].toSparse))
      }, StructType(scaledDf.schema))
      if (ModelDbSyncer.syncer.isDefined)
        ModelDbSyncer.syncer.get.buffer(TransformEvent(scalerModel, scaledDf, df))

      numericalCols.diff(scaledCols) ++ scaledCols
    } else
      numericalCols

    // Now index the label column if one has been given.
    val labelIndexer =
    if (labelCol != "")
      Some(new StringIndexer()
        .setInputCol(labelCol)
        .setOutputCol(indexed(labelCol)))
    else
      None

    // Create a vector assembler that will create the features column.
    val vectorAssembler = new VectorAssembler()
      .setInputCols(categorialCols.map(col => vec(col)) ++ actualNumericalCols)
      .setOutputCol(featuresCol)

    // Create and fit the pipeline.
    val pipeline = new Pipeline()
      .setStages(indexers ++ oneHotEncoders ++ Array(vectorAssembler) ++ labelIndexer)

    val pipelineModel = pipeline.fitSync(df)

    // Transform the data.
    val resultDf = pipelineModel.transformSync(df)

    // Create a label converter if there's a label column.
    val labelConverter = if (labelCol != "")
      Some(new IndexToString()
        .setInputCol(predictionCol)
        .setOutputCol(converted(predictionCol))
        .setLabels(pipelineModel.stages.last.asInstanceOf[StringIndexerModel].labels))
    else
      None

    val featureVectorNames = pipelineModel
      .stages
      .slice(0, indexers.length)
      .flatMap(t => t.asInstanceOf[StringIndexerModel].labels.map(lab => s"is$lab")) ++
      actualNumericalCols

    if (ModelDbSyncer.syncer.isDefined)
      ModelDbSyncer.syncer.get.featureTracker.setFeaturesForDf(resultDf, featureVectorNames)

    // Return the result.
    (resultDf, featureVectorNames, labelConverter)
  }
}
