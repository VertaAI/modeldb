package edu.mit.csail.db.ml.modeldb.client

import java.util.UUID

import com.twitter.finagle.Thrift
import com.twitter.finagle.transport.Transport
import com.twitter.util.{Await, Duration}
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

/**
  * Represents a project configuration for the syncer to use.
  * @param id - The ID of a project.
  */
abstract class ProjectConfig(val id: Int)

/**
  * Indicates that the syncer should use an existing project with the given ID.
  * @param id - The ID of a project.
  */
case class ExistingProject(override val id: Int) extends ProjectConfig(id)

/**
  * Indicates that the syncer should create a new project.
  * @param name - The name of the new project.
  * @param author - The author of the new project.
  * @param description - The description of the new project.
  */
case class NewOrExistingProject(name: String, author: String, description: String) extends ProjectConfig(-1)

/**
  * Indicates that the project is unspecified.
  */
private case class UnspecifiedProject() extends ProjectConfig(-1)

/**
  * Represents an experiment configuration for the syncer to use.
  * @param id - The ID of an experiment.
  */
abstract class ExperimentConfig(val id: Int)

/**
  * Indicates that the syncer should use an existing experiment with the given ID.
  * @param id - The ID of an experiment.
  */
case class ExistingExperiment(override val id: Int) extends ExperimentConfig(-1)

/**
  * Indicates that the experiment is not specified.
  */
private case class UnspecifiedExperiment() extends ExperimentConfig(-1)

/**
  * Indicates that the syncer use the default experiment in its project.
  */
case class DefaultExperiment() extends ExperimentConfig(-1)

/**
  * Indicates that the syncer should create a new experiment in its project.
  * @param name - The name of the new experiment.
  * @param description - The description of the new experiment.
  */
case class NewOrExistingExperiment(name: String, description: String) extends ExperimentConfig(-1)

/**
  * Represents the configuration of the experiment run used by the syncer.
  * @param id - The ID of the experiment run.
  */
abstract class ExperimentRunConfig(val id: Int)

/**
  * Indicates that the syncer will use an existing experiment run with the given ID.
  * @param id - The ID of the experiment run.
  */
case class ExistingExperimentRun(override val id: Int) extends ExperimentRunConfig(id)

/**
  * Indicates that the syncer will create a new experiment run.
  * @param description - The description of the experiment run.
  */
case class NewExperimentRun(description: String="") extends ExperimentRunConfig(-1)

/**
  * This is the syncer that is responsible for storing events in the ModelDB.
  *
  * @param hostPortPair - The hostname and port of the ModelDB Server Thrift API.
  * @param syncingStrategy - Describes the frequency with which buffered events are flushed to the server.
  * @param projectConfig - The desired project configuration.
  * @param experimentConfig - The desired experiment configuration.
  * @param experimentRunConfig - The desired experiment run configuration.
  * @param shouldCountRows - A boolean indicating whether ModelDB should count the number of rows in each
  * DataFrame and store the count in the database. Counting the number of rows requires a full sequential scan of the
  * DataFrame, which is a performance intensive operation. If shouldCountRows is set to true, then the rows will
  * be counted and stored in the database. if shouldCountRows is set to false, then ModelDB will simply store -1 as
  * the number of rows in each DataFrame. By default, we count the number of rows.
  * @param shouldStoreGSCVE - A boolean indicating whether intermediate FitEvents/TransformEvents/MetricEvents should
  *                         be stored for a GridSearchCrossValidationEvent. Disabling this improves performance.
  * @param shouldStoreSpecificModels - Whether specific models (e.g. TreeModel, LinearModel) should be stored wherever
  *                                  applicable. Disabling this improves performance.
  */
class ModelDbSyncer(var hostPortPair: Option[(String, Int)] = Some("localhost", 6543),
                    var syncingStrategy: SyncingStrategy = SyncingStrategy.Eager,
                    var projectConfig: ProjectConfig = new UnspecifiedProject,
                    var experimentConfig: ExperimentConfig = new UnspecifiedExperiment,
                    var experimentRunConfig: ExperimentRunConfig = new NewExperimentRun,
                    var shouldCountRows: Boolean = false,
                    var shouldStoreGSCVE: Boolean = false,
                    var shouldStoreSpecificModels: Boolean = false) {

  /**
    * Configure this syncer with the configuration from JSON.
    * @param conf - The JSON configuration. You can create this by doing SyncerConfig(pathToFile).
    */
  def this(conf: SyncerConfigJson) {
    this(
      hostPortPair = Some(conf.thrift.host, conf.thrift.port),
      syncingStrategy = conf.syncingStrategy match {
        case "eager" => SyncingStrategy.Eager
        case "manual" => SyncingStrategy.Manual
        case _ => SyncingStrategy.Eager
      },
      projectConfig = NewOrExistingProject(
        conf.project.name,
        conf.project.author,
        conf.project.description
      ),
      experimentConfig = NewOrExistingExperiment(
        conf.experiment.name,
        conf.experiment.description
      ),
      experimentRunConfig = NewExperimentRun(
        conf.experimentRun.description
      ),
      shouldCountRows = conf.shouldCountRows,
      shouldStoreGSCVE = conf.shouldStoreGSCVE,
      shouldStoreSpecificModels = conf.shouldStoreSpecificModels
    )
  }

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
    case Some((host, port)) =>  Some(Thrift.client.
      configured(Transport.Liveness(keepAlive=Some(true),readTimeout = Duration.Top, writeTimeout = Duration.Top)).
      newIface[ModelDBService.FutureIface](s"$host:$port"))
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


  /**
    * Associates object and ID.
    * @param obj - object.
    * @param id - ID.
    * @return The syncer.
    */
  def associateObjectAndId(obj: Any, id: Int): ModelDbSyncer = {
    objectIdMappings.putVK(obj, id)
    this
  }

  /**
    * Associates object and ID.
    * @param id - The ID.
    * @param obj - The object.
    * @return The syncer.
    */
  def associateObjectAndId(id: Int, obj: Any): ModelDbSyncer = associateObjectAndId(obj, id)

  /**
    * Get the ID of a given object.
    * @param obj - The object.
    * @return The ID of the given object, or None if the object cannot be found.
    */
  def id(obj: Any): Option[Int] = objectIdMappings.getV(obj)

  /**
    * Get the object associated with the given ID.
    * @param id -  The ID.
    * @return The object assocaited with the given ID, or None if there's no object with the given ID.
    */
  def objectForId(id: Int): Option[Any] = objectIdMappings.getK(id)

  /**
    * Associate an object with a tag.
    * @param obj - The object.
    * @param tag - The tag, which is a short textual description to associate with the object.
    * @return The syncer.
    */
  def associateObjectAndTag(obj: Any, tag: String): ModelDbSyncer = {
    objectTagMappings.putVK(obj, tag)
    this
  }

  /**
    * Associate an object with a tag.
    * @param tag - The tag, which is a short textual description to associate with the object.
    * @param obj - The object.
    * @return The syncer.
    */
  def associateObjectAndTag(tag: String, obj: Any): ModelDbSyncer = associateObjectAndTag(obj, tag)

  /**
    * Get the tag associated with a given object.
    * @param obj - The object.
    * @return The tag associated with the object, or None if the object has no tag.
    */
  def tag(obj: Any): Option[String] = objectTagMappings.getV(obj)

  /**
    * Get the object associated with the given tag.
    * @param tag - The tag.
    * @return The object associated with the tag, or None if the object has no tag.
    */
  def objectForTag(tag: String): Option[Any] = objectTagMappings.getK(tag)

  /**
    * Buffer event in the syncer. When it's done syncing, execute the postSync function.
    * @param event - The event to buffer.
    * @param postSync - The function execute after syncing. It takes as its argument both the syncer and the event
    *                 that was synced.
    */
  def buffer(event: ModelDbEvent, postSync: (ModelDbSyncer, ModelDbEvent) => Unit): Unit = {
    buffered.append(BufferEntry(event, postSync))
    if (syncingStrategy == SyncingStrategy.Eager)
      sync()
  }

  /**
    * Buffer an event in the syncer.
    * @param event - The event to buffer.
    */
  def buffer(event: ModelDbEvent): Unit = buffer(event, (a, b) => {})

  /**
    * Load the Transformer, with the given ID, from the server and cast it to type T.
    * @param id - The ID of the Transformer.
    * @param reader - The reader that is responsible for reading from the model filesystem and converting the data
    *               into an object of type T.
    * @tparam T - The type of the Transformer.
    * @return The deserialized Transformer, or None if there is no serialized model file for the Transformer with the
    *         given ID.
    */
  def load[T](id: Int, reader: MLReader[T]): Option[T] = {
    val path = Await.result(client.get.pathForTransformer(id))
    if (path == null)
      None
    else
      Some(reader.load(path))
  }

  /**
    * Store all events in the buffer on the ModelDB server.
    */
  def sync(): Unit = {
    // NOTE: This function assumes that we have a single thread.
    // Copy the buffered elements into a separate buffer and clear the original events.
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

  // Set up the project.
  var project = projectConfig match {
    case ExistingProject(id) => modeldb.Project(id, "", "", "")
    case np: NewOrExistingProject => modeldb.Project(np.id, np.name, np.author, np.description)
    case UnspecifiedProject() => modeldb.Project(-1, "", "", "")
  }

  // If it's a new project, buffer a ProjectEvent.
  projectConfig match {
    case np: NewOrExistingProject =>
      this.buffer(ProjectEvent(project))
      this.sync()
  }

  // Set up the experiment.
  var experiment = experimentConfig match {
    case ExistingExperiment(id) => modeldb.Experiment(id, -1, "", "")
    case de: DefaultExperiment => modeldb.Experiment(-1, project.id, "", "", true)
    case ne: NewOrExistingExperiment => modeldb.Experiment(ne.id, project.id, ne.name, ne.description)
    case UnspecifiedExperiment() => modeldb.Experiment(-1, -1, "", "")
  }

  // If it's a new experiment, buffer an ExperimentEvent.
  experimentConfig match {
    case NewOrExistingExperiment(_, _) | DefaultExperiment() =>
      this.buffer(ExperimentEvent(experiment))
      this.sync()
  }

  // Se up the experiment run.
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

  // If it's a new experiment run, buffer an ExperimentRunEvent.
  experimentRunConfig match {
    case ner: NewExperimentRun =>
      this.buffer(ExperimentRunEvent(experimentRun))
      this.sync()
  }

  /**
    * Gets the common ancestor of two DataFrames (see ModelDB.thrift - getCommonAncestor).
    * @param df1Id - ID of the first DataFrame.
    * @param df2Id - ID of the second DataFrame.
    * @return The common ancestor DataFrame.
    */
  def getCommonAncestor(df1Id: Int, df2Id: Int): CommonAncestor = Await.result(client.get.getCommonAncestor(df1Id, df2Id))

  /**
    * Get or create a filepath to the given Transformer (see ModelDB.thrift - getFilePath).
    * @param t - The Transformer.
    * @param desiredFileName - The desired filename to use if the Transformer does not already have a filepath and if
    *                       the name is not already used.
    * @return The filepath of the Transformer.
    */
  def getFilepath(t: Transformer, desiredFileName: String): String = {
    val st = SyncableTransformer.apply(t)
    Await.result(client.get.getFilePath(st, experimentRun.id, desiredFileName))
  }

  /**
    * @param items - The objects whose IDs we seek.
    * @return None if any of the given objects does not have an ID. Otherwise, returns the IDs of the objects.
    */
  private def idsOrNone(items: Object*): Option[Seq[Int]] = {
    val ids = items.map(id)
    if (ids.exists(_.isEmpty))
      None
    else
      Some(ids.map(_.get))
  }

  /**
    * Get the common ancestor of two DataFrames (see ModelDB.thrift - getCommonAncestor).
    * @param df1 - The first DataFrame. Must have an ID.
    * @param df2 - The second DataFrame. Must have an ID.
    * @return The common ancestor of the two DataFrames.
    */
  def getCommonAncestor(df1: DataFrame, df2: DataFrame): CommonAncestor = {
    val (df1Id, df2Id) = (id(df1), id(df2))
    if (df1Id.isEmpty || df2Id.isEmpty)
      CommonAncestor(None, -1, -1)
    else
      getCommonAncestor(df1Id.get, df2Id.get)
  }

  /**
    * Get the common ancestor DataFrame of two models (see ModelDB.thrift - getCommonAncestorForModels).
    * @param m1Id - The ID of the first model.
    * @param m2Id - The ID of the second model.
    * @return The common ancestor DataFrame of the two models.
    */
  def getCommonAncestorDf(m1Id: Int, m2Id: Int): CommonAncestor =
    Await.result(client.get.getCommonAncestorForModels(m1Id, m2Id))

  /**
    * Get the common ancestor of two models (see ModelDB.thrift - getCommonAncestorForModels).
    * @param m1 - The first model. Must have an ID.
    * @param m2 - The second model. Must have an ID.
    * @return THe common ancestor of the two models.
    */
  def getCommonAncestorDf(m1: Transformer, m2: Transformer): CommonAncestor = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CommonAncestor(None, -1, -1)
    else
      getCommonAncestorDf(m1Id.get, m2Id.get)
  }

  /**
    * Get the number of rows in the DataFrames used to train each of the given models (specified by ID)
    * (see ModelDB.thrift - getTrainingRowsCounts).
    * @param mids - The IDs of the models.
    * @return The number of rows in the DataFrames used to train each of the given models. A -1 is given if the
    *         number of rows is not stored in the database.
    */
  def getDatasetSizes(mids: Int*): Seq[Int] = Await.result(client.get.getTrainingRowsCounts(mids))

  /**
    * Get the number of rows in the DataFrames used to train each of the given models
    * (see ModelDB.thrift - getTrainingRowsCounts).
    * @param models - The models. They must have IDs.
    * @return The number of rows in the DataFrames used to train each of the given models. A -1 is given if the
    *         number of rows is not stored in the database.
    */
  def getDatasetSizes(models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(getDatasetSizes(ids:_*))
    case None => None
  }

  /**
    * Compare the features used in the two given models.
    * @param m1Id - The ID of the first model.
    * @param m2Id - The ID of the second model.
    * @return The comparison of features between the two models.
    */
  def compareFeatures(m1Id: Int, m2Id: Int): CompareFeaturesResponse =
    Await.result(client.get.compareFeatures(m1Id, m2Id))

  /**
    * Compare the features used in the two given models.
    * @param m1 - The first model. Must have an ID.
    * @param m2 - the second model. Must have an ID.
    * @return The comparison of features.
    */
  def compareFeatures(m1: Transformer, m2: Transformer): CompareFeaturesResponse = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CompareFeaturesResponse()
    else
      compareFeatures(m1Id.get, m2Id.get)
  }

  /**
    * Compare the hyperparameters between two models. (see ModelDB.thrift - compareHyperparameters).
    * @param m1Id - The ID of the first model.
    * @param m2Id - The ID of the second model.
    * @return The comparison of hyperparameters.
    */
  def compareHyperparameters(m1Id: Int, m2Id: Int): CompareHyperParametersResponse =
    Await.result(client.get.compareHyperparameters(m1Id, m2Id))

  /**
    * Compare the hyperparameters between two models. (see ModelDB.thrift - compareHyperparameters).
    * @param m1 - The first model. Must have an ID.
    * @param m2 - The second model. Must have an ID.
    * @return The comparison of hyperparameters.
    */
  def compareHyperparameters(m1: Transformer, m2: Transformer): CompareHyperParametersResponse = {
    val (m1Id, m2Id) = (id(m1), id(m2))
    if (m1Id.isEmpty || m2Id.isEmpty)
      CompareHyperParametersResponse()
    else
      compareHyperparameters(m1Id.get, m2Id.get)
  }

  /**
    * Groups the given models by problem type (see ModelDB.thrift - groupByProblemType).
    * @param modelIds - The IDs of the models.
    * @return A map that goes from problem type to the IDs of the models that have the given problem type.
    */
  def groupModels(modelIds: Int*): scala.collection.Map[ProblemType, Seq[Int]] =
    Await.result(client.get.groupByProblemType(modelIds))

  /**
    * Groups the given models by problem type (see ModelDB.thrift - groupByProblemType).
    * @param models - The models. Each one must have an ID.
    * @return A map that goes from problem type to the IDs of the models that have the given problem type.
    */
  def groupModels(models: Transformer*): Option[scala.collection.Map[ProblemType, Seq[Int]]] =
    idsOrNone(models:_*) match {
      case Some(ids) => Some(groupModels(ids:_*))
      case None => None
    }

  /**
    * Find models similar to the given model (see ModelDB.thrift - similarModels).
    * @param modelId - The ID of the given model.
    * @param numModels - The maximum number of similar models to find.
    * @param compMetrics - The comparison metrics (most important first, least important last) used to compare
    *                    model similarity.
    * @return The IDs of the models that are most similar to the model with ID modelId. The most similar model is
    *         first.
    */
  def similarModels(modelId: Int, numModels: Int, compMetrics: ModelCompMetric*): Seq[Int] =
    Await.result(client.get.similarModels(modelId, compMetrics, numModels))

  /**
    * Find models similar to the given model (see ModelDB.thrift - similarModels).
    * @param model - The model.
    * @param numModels - The maximum number of similar models to find.
    * @param compMetrics - The comparison metrics (most important first, least important last) used to compare
    *                    model similarity.
    * @return The IDs of the models that are most similar to the model with ID modelId. The most similar model is
    *         first. None is returned if the given model does not have an ID.
    */
  def similarModels(model: Transformer, numModels: Int, compMetrics: ModelCompMetric*): Option[Seq[Int]] =
    id(model) match {
      case Some(modelId) => Some(similarModels(modelId, numModels, compMetrics:_*))
      case None => None
    }

  /**
    * Get the names of the features, ranked from most important to least important, of the model with the given ID.
    * The model must be a linear model (see ModelDB.thrift - linearModelFeatureImportances).
    * @param linearModelId - The ID of a model. It must be a linear model.
    * @return The names of the features, ranked from most important to least important, of the model with the given ID.
    */
  // TODO: I think the linear model assumption is no longer required.
  def featureImportance(linearModelId: Int): Seq[String] =
    Await.result(client.get.linearModelFeatureImportances(linearModelId))

  // Convenience function for getting the feature names (ranked by importance) of a linear model or returning None
  // if the given linear model does not have a valid ID.
  private def featureImportanceHelper(x: Object): Option[Seq[String]] = id(x) match {
    case Some(lrmId) => Some(featureImportance(lrmId))
    case None => None
  }

  /**
    * Like featureImportance(linearModelId), but takes in a LogisticRegressionModel.
    * @param x - The model.
    * @return None if the given model does not have an ID. Otherwise, returns the same thing as
    *         featureImportance(linearModelId).
    */
  def featureImportance(x: LogisticRegressionModel): Option[Seq[String]] = featureImportanceHelper(x)

  /**
    * Like featureImportance(linearModelId), but takes in a LinearRegressionModel.
    * @param x - The model.
    * @return None if the given model does not have an ID. Otherwise, returns the same thing as
    *         featureImportance(linearModelId).
    */
  def featureImportance(x: LinearRegressionModel): Option[Seq[String]] = featureImportanceHelper(x)

  /**
    * Compare feature importances for the two given linear models
    * (see ModelDB.thrift - compareLinearModelFeatureImportances).
    * @param linearModel1Id - The ID of the first model.
    * @param linearModel2Id - The ID of the second model.
    * @return A comparison of feature importances between the two models.
    */
  def featureImportance(linearModel1Id: Int, linearModel2Id: Int) =
    Await.result(client.get.compareLinearModelFeatureImportances(linearModel1Id, linearModel2Id))

  // A convenience function for comparing feature importances.
  private def featureImportanceHelper(x: Object, y: Object): Option[Seq[FeatureImportanceComparison]] = id(x) match {
    case Some(lrm1Id) => id(y) match {
      case Some(lrm2Id) => Some(featureImportance(lrm1Id, lrm2Id))
      case None => None
    }
    case None => None
  }

  /**
    * Compare feature importances of two models. Returns None if either model does not have an ID. Otherwise returns
    * the same return type as featureImportance(linearModel1Id: Int, linearModel2Id: Int).
    */
  def featureImportance(x: LogisticRegressionModel, y: LogisticRegressionModel) = featureImportanceHelper(x, y)

  /**
    * Compare feature importances of two models. Returns None if either model does not have an ID. Otherwise returns
    * the same return type as featureImportance(linearModel1Id: Int, linearModel2Id: Int).
    */
  def featureImportance(x: LinearRegressionModel, y: LinearRegressionModel) = featureImportanceHelper(x, y)

  /**
    * Compute the number of iterations to convergence for each of the given models (see ModelDB.thrift -
    * iterationsUntilConvergence).
    * @param tolerance - The tolerance level (see ModelDB.thrift for more info).
    * @param modelIds - The IDs of the models whose convergence times we seek.
    * @return For each model, the number of iterations until the model converged. -1 if the model did not converge.
    */
  def convergenceTimes(tolerance: Double, modelIds: Int*): Seq[Int] =
    Await.result(client.get.iterationsUntilConvergence(modelIds, tolerance))

  /**
    * Like convergenceTimes(tolerance: Double, modelIds: Int*), but takes in Transformers rather than IDs. Returns the
    * same value as the method that takes in IDs unless any of the Transformers does not have an ID. In that case, None
    * is returned.
    */
  def convergenceTimes(tolerance: Double, models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(convergenceTimes(tolerance, ids:_*))
    case None => None
  }

  /**
    * Rank the given models according to a ranking metric (see ModelDB.thrift - rankModels).
    * @param rankMetric - The metric to use for ranking the model.
    * @param modelIds - The IDs of the models to rank.
    * @return The ranked sequence of model IDs. The most highly ranked model is first.
    */
  def rankModels(rankMetric: ModelRankMetric, modelIds: Int*): Seq[Int] =
    Await.result(client.get.rankModels(modelIds, rankMetric))

  /**
    * Like the above method, but operates on Transformers. If any of the Transformers does not have an ID, None is
    * returned.
    */
  def rankModels(rankMetric: ModelRankMetric, models: Transformer*): Option[Seq[Int]] = idsOrNone(models:_*) match {
    case Some(ids) => Some(rankModels(rankMetric, ids:_*))
    case None => None
  }

  /**
    * Computes the t-statistic based confidence interval for the given model at the given significance level.
    * (see ModelDB.thrift - confidenceIntervals).
    * @param modelId - The ID of the model. This must be a linear model.
    * @param significanceLevel - The significance level.
    * @return The confidence interval.
    */
  def confidenceInterval(modelId: Int, significanceLevel: Double): Seq[ConfidenceInterval] =
    Await.result(client.get.confidenceIntervals(modelId, significanceLevel))

  /**
    * Like the above method, but operates on a Transformer. If the Transformer does not have an ID,
    * None will be returned.
    */
  def confidenceInterval(model: Transformer, significanceLevel: Double): Option[Seq[ConfidenceInterval]] =
    id(model) match {
      case Some(modelId) => Some(confidenceInterval(modelId, significanceLevel))
      case None => None
    }

  /**
    * Get the models that use all of the given features (see ModelDB.thrift - modelsWithFeatures).
    * @param features - The names of some features.
    * @return The IDs of all the models that use the given features.
    */
  def modelsWithFeatures(features: String*): Seq[Int] = Await.result(client.get.modelsWithFeatures(features))

  /**
    * Get the IDs of all the models that are derived from the DataFrame with the given ID.
    * (see ModelDB.thrift - modelsDerivedFromDataFrame).
    * @param dfId - The ID of a DataFrame.
    * @return The IDs of all the models that derived from the DataFrame with the given ID.
    */
  def modelsDerivedFromDataFrame(dfId: Int): Seq[Int] = Await.result(client.get.modelsDerivedFromDataFrame(dfId))

  /**
    * Like the above, but operates on a DataFrame instead of an ID. If the DataFrame does not have an ID, None is
    * returned.
    */
  def modelsDerivedFromDataFrame(df: DataFrame): Option[Seq[Int]] = id(df) match {
    case Some(dfId) => Some(modelsDerivedFromDataFrame(dfId))
    case None => None
  }

  /**
    * Get the original features used by the given model (see ModelDB.thrift - originalFeatures).
    * @param modelId - The ID of a model.
    * @return The original features used by the model.
    */
  def originalFeatures(modelId: Int): Seq[String] = Await.result(client.get.originalFeatures(modelId))

  /**
    * Like the above, but operates on a Transformer. If the Transformer does not have an ID, None will be returned.
    */
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