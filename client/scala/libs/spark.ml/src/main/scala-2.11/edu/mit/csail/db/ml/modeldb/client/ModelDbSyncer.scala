package edu.mit.csail.db.ml.modeldb.client

import java.util.UUID

import com.twitter.finagle.Thrift
import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client.SyncingStrategy.SyncingStrategy
import edu.mit.csail.db.ml.modeldb.client.event.{ExperimentEvent, ExperimentRunEvent, ModelDbEvent, ProjectEvent}
import org.apache.spark.ml.FeatureTracker
import edu.mit.csail.db.ml.modeldb.evaluation.Timer
import modeldb.ModelDBService.FutureIface
import modeldb._
import org.apache.spark.ml.classification.LogisticRegressionModel
import org.apache.spark.ml.regression.LinearRegressionModel
import org.apache.spark.ml.util.MLReader
import org.apache.spark.ml.{SyncableCrossValidator, SyncableEstimator, SyncablePipeline, Transformer}
import org.apache.spark.sql.DataFrame

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Represents the strategy used by the ModelDb when deciding how to sync.
  * Eager means that it will sync right after an event is buffered.
  * Manual means it will not sync until the user calls the sync() function.
  */
object SyncingStrategy extends Enumeration {
  type SyncingStrategy = Value
  val Eager, Manual = Value
}

abstract class ProjectConfig(val id: Int)
case class ExistingProject(override val id: Int) extends ProjectConfig(id)
case class NewOrExistingProject(name: String, author: String, description: String) extends ProjectConfig(-1)

abstract class ExperimentConfig(val id: Int)
case class ExistingExperiment(override val id: Int) extends ExperimentConfig(-1)
case class DefaultExperiment() extends ExperimentConfig(-1)
case class NewOrExistingExperiment(name: String, description: String) extends ExperimentConfig(-1)

abstract class ExperimentRunConfig(val id: Int)
case class ExistingExperimentRun(override val id: Int) extends ExperimentRunConfig(id)
case class NewExperimentRun(description: String="") extends ExperimentRunConfig(-1)

/**
  * This is the Syncer that is responsible for storing events in the ModelDB.
  *
  * The shouldCountRows parameter is a boolean indicating whether ModelDB should count the number of rows in each
  * DataFrame and store the count in the database. Counting the number of rows requires a full sequential scan of the
  * DataFrame, which is a performance intensive operation. If shouldCountRows is set to true, then the rows will
  * be counted and stored in the database. if shouldCountRows is set to false, then ModelDB will simply store -1 as
  * the number of rows in each DataFrame. By default, we count the number of rows.
  */
class ModelDbSyncer(hostPortPair: Option[(String, Int)] = Some("localhost", 6543),
                    syncingStrategy: SyncingStrategy = SyncingStrategy.Eager,
                    projectConfig: ProjectConfig,
                    experimentConfig: ExperimentConfig = new DefaultExperiment,
                    experimentRunConfig: ExperimentRunConfig,
                    val shouldCountRows: Boolean = true,
                    val shouldStoreGSCVE: Boolean = true,
                    val shouldStoreSpecificModels: Boolean = true) {
  /**
    * This is a helper class that will constitute the entries in the buffer.
    * @param event - The event in the buffer.
    * @param postSync - The function to execute after the event has been sycned to the ModelDB.
    */
  case class BufferEntry(event: ModelDbEvent, postSync: (ModelDbSyncer, ModelDbEvent) => Unit)

  /**
    * As we get events, we buffer them in this ArrayBuffer.
    */
  var buffered: ArrayBuffer[BufferEntry] = ArrayBuffer()

  /**
    * This is the Thrift client that is responsible for talking to the ModelDB server.
    */
  private val client: Option[FutureIface] = hostPortPair match {
    case Some((host, port)) => Some(Thrift.client.newIface[ModelDBService.FutureIface](s"$host:$port"))
    case None => None
  }

  /**
    * We keep a mapping between objects and their IDs.
    */
  val objectIdMappings = new TwoWayMapping[Int, Any]

  /**
    * We keep a mapping between objects and their tags. Tags are short, user-given names.
    */
  val objectTagMappings = new TwoWayMapping[String, Any]

  /**
    * We will map DataFrames to the contents of their feature vectors.
    */
  val featureTracker = new FeatureTracker

  // The two functions below associate an object and an ID.
  def associateObjectAndId(obj: Any, id: Int): ModelDbSyncer = {
    objectIdMappings.putVK(obj, id)
    this
  }
  def associateObjectAndId(obj: Int, id: Any): ModelDbSyncer = associateObjectAndId(id, obj)

  // Get the ID for an object.
  def id(obj: Any): Option[Int] = objectIdMappings.getV(obj)

  // Get the object for an ID.
  def objectForId(id: Int): Option[Any] = objectIdMappings.getK(id)

  // The two functions below associate an object with a tag.
  def associateObjectAndTag(obj: Any, tag: String): ModelDbSyncer = {
    objectTagMappings.putVK(obj, tag)
    this
  }
  def associateObjectAndTag(tag: String, obj: Any): ModelDbSyncer = associateObjectAndTag(obj, tag)

  // Get the tag for an object.
  def tag(obj: Any): Option[String] = objectTagMappings.getV(obj)

  // Get the object for a given tag.
  def objectForTag(tag: String): Option[Any] = objectTagMappings.getK(tag)

  /**
    * Buffer event in the ModelDB. When it's done syncing, execute the postSync function.
    * @param event - The event to buffer.
    * @param postSync - The function execute after syncing.
    */
  def buffer(event: ModelDbEvent, postSync: (ModelDbSyncer, ModelDbEvent) => Unit): Unit = {
    buffered.append(BufferEntry(event, postSync))
    if (syncingStrategy == SyncingStrategy.Eager)
      sync()
  }

  // This variant of buffer() uses an no-op for its post-sync function.
  def buffer(event: ModelDbEvent): Unit = buffer(event, (a, b) => {})

  // Load Transformer with given ID and cast it to type T.
  def load[T](id: Int, reader: MLReader[T]): Option[T] = {
    val path = Await.result(client.get.pathForTransformer(id))
    if (path == null)
      None
    else
      Some(reader.load(path))
  }

  // This does tail recursion optimization so we don't have stack overflows.
  def sync(): Unit = {
    // NOTE: This function assumes that we have a single thread.
    // TODO: Do we need to make this multithreaded?
    // Copy the buffered elements into a separate buffer.
    val entriesToSync = buffered.map(item => item)
    buffered.clear()

    // Sync all the elements in the new buffer.
    entriesToSync.foreach(ent =>
      Timer.time("Syncing " + ent.event.getClass.getSimpleName)(ent.event.sync(client.get, Some(this)))
    )

    // Now execute the callbacks.
    entriesToSync.foreach(entry => entry.postSync(this, entry.event))

    // Now all the entries in entriesToSync have been synced and their callbacks have been executed.
    // So, we can clear them (not strictly necessary, because GC will clean this local variable eventually).
    entriesToSync.clear()

    // Some of the callbacks may have buffered other entries, so recursively call sync.
    if (buffered.nonEmpty)
      sync()
  }

  var project = projectConfig match {
    case ExistingProject(id) => modeldb.Project(id, "", "", "")
    case np: NewOrExistingProject => modeldb.Project(np.id, np.name, np.author, np.description)
  }

  // TODO: should we add an experiment event anyway?
  projectConfig match {
    case np: NewOrExistingProject =>
      this.buffer(ProjectEvent(project))
      this.sync() // this will also 
  }

  var experiment = experimentConfig match {
    case ExistingExperiment(id) => modeldb.Experiment(id, -1, "", "")
    case de: DefaultExperiment => modeldb.Experiment(-1, project.id, "", "", true)
    case ne: NewOrExistingExperiment => modeldb.Experiment(ne.id, project.id, ne.name, ne.description)
  }

  experimentConfig match {
    case NewOrExistingExperiment(_, _) | DefaultExperiment() =>
      this.buffer(ExperimentEvent(experiment))
      this.sync()
  }

  var experimentRun = experimentRunConfig match {
    case ExistingExperimentRun(id) => modeldb.ExperimentRun(id, experiment.id, "")
    case ner: NewExperimentRun =>
      modeldb.ExperimentRun(ner.id,
        experiment.id,
        if (ner.description == "")
          "Experiment " + UUID.randomUUID()
        else
          ner.description
      )
  }

  experimentRunConfig match {
    case ner: NewExperimentRun =>
      this.buffer(ExperimentRunEvent(experimentRun))
      this.sync()
  }

  // Model functions.
  def getCommonAncestor(df1Id: Int, df2Id: Int): CommonAncestor = Await.result(client.get.getCommonAncestor(df1Id, df2Id))

  private def idsOrNone(items: Object*): Option[Seq[Int]] = {
    val ids = items.map(id)
    if (ids.exists(_.isEmpty))
      None
    else
      Some(ids.map(_.get))
  }

  def getCommonAncestor(df1: DataFrame, df2: DataFrame): CommonAncestor = {
    val (df1Id, df2Id) = (id(df1), id(df2))
    if (df1Id.isEmpty || df2Id.isEmpty)
      CommonAncestor(None, -1, -1)
    else
      getCommonAncestor(df1Id.get, df2Id.get)
  }

  def getCommonAncestorDf(m1Id: Int, m2Id: Int): CommonAncestor =
    Await.result(client.get.getCommonAncestorForModels(m1Id, m2Id))

  def getCommonAncestorDf(m1: Transformer, m2: Transformer): CommonAncestor = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CommonAncestor(None, -1, -1)
    else
      getCommonAncestorDf(m1Id.get, m2Id.get)
  }

  def getDatasetSizes(mids: Int*): Seq[Int] = Await.result(client.get.getTrainingRowsCounts(mids))

  def getDatasetSizes(models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(getDatasetSizes(ids:_*))
    case None => None
  }

  def compareFeatures(m1Id: Int, m2Id: Int): CompareFeaturesResponse =
    Await.result(client.get.compareFeatures(m1Id, m2Id))

  def compareFeatures(m1: Transformer, m2: Transformer): CompareFeaturesResponse = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CompareFeaturesResponse()
    else
      compareFeatures(m1Id.get, m2Id.get)
  }

  def compareHyperparameters(m1Id: Int, m2Id: Int): CompareHyperParametersResponse =
    Await.result(client.get.compareHyperparameters(m1Id, m2Id))

  def compareHyperparameters(m1: Transformer, m2: Transformer): CompareHyperParametersResponse = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CompareHyperParametersResponse()
    else
      compareHyperparameters(m1Id.get, m2Id.get)
  }

  def groupModels(modelIds: Int*): scala.collection.Map[ProblemType, Seq[Int]] =
    Await.result(client.get.groupByProblemType(modelIds))

  def groupModels(models: Transformer*): Option[scala.collection.Map[ProblemType, Seq[Int]]] =
    idsOrNone(models:_*) match {
      case Some(ids) => Some(groupModels(ids:_*))
      case None => None
    }

  def similarModels(modelId: Int, numModels: Int, compMetrics: ModelCompMetric*): Seq[Int] =
    Await.result(client.get.similarModels(modelId, compMetrics, numModels))

  def similarModels(model: Transformer, numModels: Int, compMetrics: ModelCompMetric*): Option[Seq[Int]] =
    id(model) match {
      case Some(modelId) => Some(similarModels(modelId, numModels, compMetrics:_*))
      case None => None
    }

  def featureImportance(linearModelId: Int): Seq[String] =
    Await.result(client.get.linearModelFeatureImportances(linearModelId))
  private def featureImportanceHelper(x: Object): Option[Seq[String]] = id(x) match {
    case Some(lrmId) => Some(featureImportance(lrmId))
    case None => None
  }
  def featureImportance(x: LogisticRegressionModel): Option[Seq[String]] = featureImportanceHelper(x)
  def featureImportance(x: LinearRegressionModel): Option[Seq[String]] = featureImportanceHelper(x)
  def featureImportance(linearModel1Id: Int, linearModel2Id: Int) =
    Await.result(client.get.compareLinearModelFeatureImportances(linearModel1Id, linearModel2Id))
  private def featureImportanceHelper(x: Object, y: Object): Option[Seq[FeatureImportanceComparison]] = id(x) match {
    case Some(lrm1Id) => id(y) match {
      case Some(lrm2Id) => Some(featureImportance(lrm1Id, lrm2Id))
      case None => None
    }
    case None => None
  }
  def featureImportance(x: LogisticRegressionModel, y: LogisticRegressionModel) = featureImportanceHelper(x, y)
  def featureImportance(x: LinearRegressionModel, y: LinearRegressionModel) = featureImportanceHelper(x, y)

  def convergenceTimes(tolerance: Double, modelIds: Int*): Seq[Int] =
    Await.result(client.get.iterationsUntilConvergence(modelIds, tolerance))

  def convergenceTimes(tolerance: Double, models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(convergenceTimes(tolerance, ids:_*))
    case None => None
  }

  def rankModels(rankMetric: ModelRankMetric, modelIds: Int*): Seq[Int] =
    Await.result(client.get.rankModels(modelIds, rankMetric))

  def rankModels(rankMetric: ModelRankMetric, models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(rankModels(rankMetric, ids:_*))
    case None => None
  }

  def confidenceInterval(modelId: Int, significanceLevel: Double): Seq[ConfidenceInterval] =
    Await.result(client.get.confidenceIntervals(modelId, significanceLevel))

  def confidenceInterval(model: Transformer, significanceLevel: Double): Option[Seq[ConfidenceInterval]] =
    id(model) match {
      case Some(modelId) => Some(confidenceInterval(modelId, significanceLevel))
      case None => None
    }

  def modelsWithFeatures(features: String*): Seq[Int] = Await.result(client.get.modelsWithFeatures(features))

  def modelsDerivedFromDataFrame(dfId: Int): Seq[Int] = Await.result(client.get.modelsDerivedFromDataFrame(dfId))
  def modelsDerivedFromDataFrame(df: DataFrame): Option[Seq[Int]] = id(df) match {
    case Some(dfId) => Some(modelsDerivedFromDataFrame(dfId))
    case None => None
  }

  def originalFeatures(modelId: Int): Seq[String] = Await.result(client.get.originalFeatures(modelId))
  def originalFeatures(t: Transformer): Option[Seq[String]] = id(t) match {
    case Some(modelId) => Some(originalFeatures(modelId))
    case None => None
  }
}

/**
  * By adding all these traits to the ModelDBSyncer object, we get all the
  * implicit classes easily.
  */
object ModelDbSyncer
  extends SyncableDataFrame
    with SyncablePipeline
    with SyncableCrossValidator
    with SyncableEstimator
    with SyncableTransformer
    with Annotater
    with Taggable
    with SyncableDataFramePaths
    with SyncableEvaluator {
  implicit var syncer: Option[ModelDbSyncer] = None

  // Allow the user to configure the syncer.
  def setSyncer(mdbs: ModelDbSyncer) = syncer = Some(mdbs)
}