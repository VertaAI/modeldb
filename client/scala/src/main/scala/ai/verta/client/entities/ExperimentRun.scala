package ai.verta.client.entities

import java.io._
import java.security.MessageDigest
import java.time.{Instant, LocalDateTime}
import java.util.TimeZone

import ai.verta.client.entities.subobjects._
import ai.verta.client.entities.utils.{KVHandler, ValueType}
import ai.verta.swagger._public.modeldb.model._
import ai.verta.swagger.client.ClientSet
import ai.verta.repository._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

/** Represents a machine learning Experiment Run.
 *
 *  This class provides read/write functionality for Experiment Run metadata.
 *
 *  There should not be a need to instantiate this class directly; please use experiment run's getOrCreateExperimentRun.
 */
class ExperimentRun(val clientSet: ClientSet, val expt: Experiment, val run: ModeldbExperimentRun) extends Taggable {
  /** Return a set-like object of type Tags, representing the tags associated with ExperimentRun
   *  Provide an alternative interface to get/del/add Tags methods
   *  @return the tags set
   */
  def tags()(implicit ec: ExecutionContext) = new Tags(clientSet, ec, this)

  /** Gets all tags from this Experiment Run
   *  @return list of all the tags of this run, if succeeds
   */
  override def getTags()(implicit ec: ExecutionContext): Try[List[String]] = {
    clientSet.experimentRunService.ExperimentRunService_getExperimentRunTags(run.id)
      .map(r => r.tags.getOrElse(Nil))
  }

  /** Delete multiple tags of this Experiment Run.
   *  If the run does not have any tag in the list, that tag will be ignored
   *  @param tags tags
   *  @return whether the attempt succeeds
   */
  override def delTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.experimentRunService.ExperimentRunService_deleteExperimentRunTags(ModeldbDeleteExperimentRunTags(
      id = run.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }

  /** Logs multiple tags to this Experiment Run
   *  @param tags tags
   *  @return whether the attempt succeeds
   */
  override def addTags(tags: List[String])(implicit ec: ExecutionContext): Try[Unit] = {
    clientSet.experimentRunService.ExperimentRunService_addExperimentRunTags(ModeldbAddExperimentRunTags(
      id = run.id,
      tags = Some(tags)
    ))
      .map(_ => {})
  }

  // TODO: add overwrite
  /** Return a map-like object of type Hyperparameters, representing the hyperparameters associated with ExperimentRun.
   *  Provide an alternative interface to get/log hyperparameters
   *  @return the hyperparameters map
   */
  def hyperparameters()(implicit ec: ExecutionContext) = new Hyperparameters(clientSet, ec, this)

  /** Logs potentially multiple hyperparameters to this Experiment Run
   *  @param vals Hyperparameters
   */
  def logHyperparameters(vals: Map[String, ValueType])(implicit ec: ExecutionContext): Try[Unit] = {
    val valsList = utils.KVHandler.mapToKVList(vals)
    if (valsList.isFailure) Failure(valsList.failed.get) else
      clientSet.experimentRunService.ExperimentRunService_logHyperparameters(ModeldbLogHyperparameters(
        id = run.id,
        hyperparameters = valsList.toOption
      )).map(_ => {})
  }

  /** Logs a hyperparameter to this Experiment Run
   *  @param key Name of the hyperparameter
   *  @param value Value of the hyperparameter (String, Double, or (Big)Int)
   */
  def logHyperparameter(key: String, value: ValueType)(implicit ec: ExecutionContext) =
    logHyperparameters(Map(key -> value))

  /** Gets all hyperparameters from this Experiment Run
   *  @return Names and values of all hyperparameters
   */
  def getHyperparameters()(implicit ec: ExecutionContext): Try[Map[String, ValueType]] = {
    clientSet.experimentRunService.ExperimentRunService_getHyperparameters(
      id = run.id
    )
      .flatMap(r => {
        if (r.hyperparameters.isEmpty)
          Success(Map[String, ValueType]())
        else
          utils.KVHandler.kvListToMap(r.hyperparameters.get)
      })
  }

  /** Gets the hyperparameter with name key from this Experiment Run
   *  @param key Name of the hyperparameter
   *  @return Value of the hyperparameter
   */
  def getHyperparameter(key: String)(implicit ec: ExecutionContext) =
    getHyperparameters().map(_.get(key))

  // TODO: add overwrite
  /** Return a map-like object of type Metrics, representing the metrics associated with ExperimentRun
   *  Provide an alternative interface to get/log metrics
   *  @return the metrics map
   */
  def metrics()(implicit ec: ExecutionContext) = new Metrics(clientSet, ec, this)

  /** Logs potentially multiple metrics to this Experiment Run.
   *  @param metrics Metrics
   */
  def logMetrics(vals: Map[String, ValueType])(implicit ec: ExecutionContext): Try[Unit] = {
    val valsList = utils.KVHandler.mapToKVList(vals)
    if (valsList.isFailure) Failure(valsList.failed.get) else
      clientSet.experimentRunService.ExperimentRunService_logMetrics(ModeldbLogMetrics(
        id = run.id,
        metrics = valsList.toOption
      )).map(_ => {})
  }

  /** Logs a metric to this Experiment Run.
   *  If the metadatum of interest might recur, logObservation() should be used instead
   *  @param key Name of the metric
   *  @param value Value of the metric
   */
  def logMetric(key: String, value: ValueType)(implicit ec: ExecutionContext) =
    logMetrics(Map(key -> value))

  /** Gets all metrics from this Experiment Run
   *  @param key Name of the metric
   *  @return Names and values of all metrics
   */
  def getMetrics()(implicit ec: ExecutionContext): Try[Map[String, ValueType]] = {
    clientSet.experimentRunService.ExperimentRunService_getMetrics(
      id = run.id
    )
      .flatMap(r => {
        if (r.metrics.isEmpty)
          Success(Map[String, ValueType]())
        else
          utils.KVHandler.kvListToMap(r.metrics.get)
      })
  }

  /** Gets the metric with name key from this Experiment Run
   *  @param key Name of the metric
   *  @return Value of the metric
   */
  def getMetric(key: String)(implicit ec: ExecutionContext) =
    getMetrics().map(_.get(key))

  // TODO: add overwrite
  /** Return a map-like object of type Attributes, representing the attributes associated with ExperimentRun.
   *  Provide an alternative interface to get/log attributes methods
   *  @return the attributes map
   */
  def attributes()(implicit ec: ExecutionContext) = new Attributes(clientSet, ec, this)

  /** Logs potentially multiple attributes to this Experiment Run
   *  @param vals Attributes name and value (String, Int, or Double)
   */
  def logAttributes(vals: Map[String, ValueType])(implicit ec: ExecutionContext): Try[Unit] = {
    val valsList = utils.KVHandler.mapToKVList(vals)
    if (valsList.isFailure) Failure(valsList.failed.get) else
      clientSet.experimentRunService.ExperimentRunService_logAttributes(ModeldbLogAttributes(
        id = run.id,
        attributes = valsList.toOption
      )).map(_ => {})
  }

  /** Logs an attribute to this Experiment Run.
   *  @param key Name of the attribute
   *  @param value Value of the attribute. Could be String, Int, or Double
   */
  def logAttribute(key: String, value: ValueType)(implicit ec: ExecutionContext) =
    logAttributes(Map(key -> value))

  /** Gets multiple attributes from this Experiment Run
   *  @param keys Names of the attributes. If not passed, get all attributes.
   *  @return Values of the attributes (String, Int, or Double)
   */
  def getAttributes(keys: List[String] = Nil)(implicit ec: ExecutionContext): Try[Map[String, ValueType]] = {
    clientSet.experimentRunService.ExperimentRunService_getExperimentRunAttributes(
      id = run.id,
      attribute_keys = Some(keys),
      get_all = Some(keys.isEmpty)
    )
      .flatMap(r => {
        if (r.attributes.isEmpty)
          Success(Map[String, ValueType]())
        else
          utils.KVHandler.kvListToMap(r.attributes.get)
      })
  }

  /** Gets the attribute with name key from this Experiment Run
   *  @param key Name of the attribute
   *  @return Value of the attribute (String, Int, or Double)
   */
  def getAttribute(key: String)(implicit ec: ExecutionContext) =
    getAttributes(List(key)).map(_.get(key))

  /** Logs an observation to this Experiment Run
   *  @param key Name of the observation
   *  @param value Value of the observation
   *  @param timestamp Unix timestamp. If not provided, the current time will be used.
   */
  def logObservation(key: String, value: ValueType, timestamp: LocalDateTime = null)(implicit ec: ExecutionContext) = {
    val ts = if (timestamp == null) LocalDateTime.now() else timestamp

    val convertedValue = KVHandler.convertFromValueType(value, s"unknown type for observation ${key}: ${value.toString} (${value.getClass.toString})")
    convertedValue.flatMap(newValue => {
      clientSet.experimentRunService.ExperimentRunService_logObservation(ModeldbLogObservation(
        id = run.id,
        observation = Some(ModeldbObservation(
          attribute = Some(CommonKeyValue(
            key = Some(key),
            value = Some(newValue)
          )),
          timestamp = Some(ts.atZone(TimeZone.getTimeZone("UTC").toZoneId).toInstant.toEpochMilli)
        ))
      ))
    })
      .map(_ => {})
  }

  /** Gets the observation series with name key from this Experiment Run
   *  @param key Name of observation series
   *  @return Values of observation series
   */
  def getObservation(key: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.ExperimentRunService_getObservations(id = run.id, observation_key = Some(key))
      .map(res => {
        res.observations.map(obs => {
          obs.map(o => {
            (
              LocalDateTime.ofInstant(Instant.ofEpochMilli(o.timestamp.get.toLong), TimeZone.getTimeZone("UTC").toZoneId),
              KVHandler.convertToValueType(o.attribute.get.value.get, s"unknown type for observation ${key}: ${o.attribute.get.value.get.toString} (${o.attribute.get.value.get.getClass.toString})").get
            )
          })
        }).getOrElse(Nil)
      })
  }

  /** Gets all observations from this Experiment Run.
   *  @return Names and values of all observation series
   */
  def getObservations()(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.ExperimentRunService_getExperimentRunById(run.id)
      .flatMap(runResp => Try {
        val observations = runResp.experiment_run.get.observations
        val obsMap = new mutable.HashMap[String, List[(LocalDateTime, ValueType)]]()
        observations.get.foreach(o => {
          val ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(o.timestamp.get.toLong), TimeZone.getTimeZone("UTC").toZoneId)
          val key = o.attribute.get.key.get
          val value = KVHandler.convertToValueType(o.attribute.get.value.get, s"unknown type for observation $key: ${o.attribute.get.value.get.toString} (${o.attribute.get.value.get.getClass.toString})").get
          obsMap.update(key, (ts, value) :: obsMap.getOrElse(key, Nil))
        })
        obsMap.map(el => {
          (el._1, el._2.sortBy(_._1.atZone(TimeZone.getTimeZone("UTC").toZoneId).toInstant().toEpochMilli))
        }).toMap
      })
  }

  /** Logs an serializable artifact object to this Experiment Run
   *  @param key Name of the artifact
   *  @param obj Serializable object
   */
  def logArtifactObj[T <: Serializable](key: String, obj: T)(implicit ec: ExecutionContext) = {
    val arr = new ByteArrayOutputStream()
    val stream = new ObjectOutputStream(arr)
    stream.writeObject(obj)
    stream.close
    logArtifact(key, new ByteArrayInputStream(arr.toByteArray))
  }

  private def streamHash(stream: InputStream)(implicit ec: ExecutionContext): (Array[Byte], Int) = {
    // TODO: make into a future
    val len = 1024 * 1024
    val bytes = Array.fill[Byte](len)(0)
    var offset = 0
    val hasher = MessageDigest.getInstance("SHA-256")
    var break = false
    while (!break) {
      val bytesRead = stream.read(bytes, offset, len)
      if (bytesRead == 0)
        break = true
      else {
        hasher.update(bytes.slice(0, bytesRead))
        offset += bytesRead
        if (bytesRead < len)
          break = true
      }
    }

    stream.reset()
    return (hasher.digest(), offset)
  }

  /** Logs an artifact in the form of a stream of bytes to this Experiment Run
   *  @param key Name of the artifact
   *  @param stream Input stream
   */
  def logArtifact(key: String, stream: InputStream)(implicit ec: ExecutionContext) = {
    val hashResult = streamHash(stream)
    val artifactHash = hashResult._1
    val artifactPath = artifactHash + "/" + key

    clientSet.experimentRunService.ExperimentRunService_logArtifact(ModeldbLogArtifact(
      id = run.id,
      artifact = Some(CommonArtifact(
        key = Some(key),
        path = Some(artifactPath),
        path_only = Some(false),
        artifact_type = Some(ArtifactTypeEnumArtifactType.BLOB),
        filename_extension = None
      ))
    ))
      .flatMap(_ => {
        clientSet.experimentRunService.ExperimentRunService_getUrlForArtifact(ModeldbGetUrlForArtifact(
          id = run.id,
          key = Some(key),
          method = Some("PUT"),
          artifact_type = Some(ArtifactTypeEnumArtifactType.BLOB)
        ))
      })
      .flatMap(r => {
        Await.result(clientSet.client.requestRaw("PUT", r.url.get, null, Map("Content-Length" -> hashResult._2.toString), stream), Duration.Inf)
      })
      .map(_ => {})
  }

  /** Gets the artifact with name key from this Experiment Run
   *  @param key Name of the artifact
   *  @return Serializable artifact object
   */
  def getArtifactObj(key: String)(implicit ec: ExecutionContext) =
    getArtifact(key)
      .map(stream => {
        val stream2 = new ObjectInputStream(stream)
        val obj = stream2.readObject()
        stream2.close
        stream.close
        obj
      })

  /** Gets an artifact in the form of a stream of bytes to this Experiment Run
   *  @param key Name of the artifact
   *  @return The output stream
   */
  def getArtifact(key: String)(implicit ec: ExecutionContext) = {
    clientSet.experimentRunService.ExperimentRunService_getUrlForArtifact(ModeldbGetUrlForArtifact(
      id = run.id,
      key = Some(key),
      method = Some("GET"),
      artifact_type = Some(ArtifactTypeEnumArtifactType.BLOB)
    ))
      .flatMap(r => {
        Await.result(
          clientSet.client.requestRaw("GET", r.url.get, null, null, null)
            .map(resp => {
              resp match {
                case Success(response) => {
                  val arr = new ByteArrayInputStream(response.body)
                  Success(arr)
                }
                case Failure(x) => Failure(x)
              }
            }),
          Duration.Inf
        )
      })
  }

  /** Associate a Commit with this Experiment Run
   *  @param commit Verta commit
   *  @param keyPaths (optional) A mapping between descriptive keys and paths of particular interest within commit. This can be useful for, say, highlighting a particular file as the training dataset used for this Experiment Run.
   *  @return whether the log attempt succeeds
   */
  def logCommit(commit: Commit, keyPaths: Option[Map[String, String]] = None)(implicit ec: ExecutionContext) = {
    commit.checkSaved("Commit must be saved before it can be associated to a run").flatMap(_ => {
      // convert the path to correct format for query
      // split it, then wrapped with VertamodeldbLocation
      val keyLocationMap = keyPaths.map(
        _.mapValues(location => VertamodeldbLocation(Some(location.split("/").toList)))
      )

      clientSet.experimentRunService.ExperimentRunService_logVersionedInput(
        body = ModeldbLogVersionedInput(
          id = run.id, versioned_inputs = Some(ModeldbVersioningEntry(
            commit = commit.id,
            key_location_map = keyLocationMap,
            repository_id = Some(commit.repoId)
          ))
        )
      )
    }).map(_ => ())
  }

  /** Gets the Commit associated with this Experiment Run
   *  @return ExperimentRunCommit instance, containing the commit and key-path map.
   */
  def getCommit()(implicit ec: ExecutionContext): Try[ExperimentRunCommit] = {
    clientSet.experimentRunService.ExperimentRunService_getVersionedInputs(id = run.id).flatMap(response =>
      if (response.versioned_inputs.isEmpty || response.versioned_inputs.get.commit.isEmpty)
        Failure(new IllegalStateException("No commit is associated with this experiment run"))
      else {
        val versioningEntry = response.versioned_inputs.get
        val keyPaths = versioningEntry.key_location_map.map(
          _.map(pair => (pair._1, pair._2.location.get.mkString("/")))
        )

        clientSet.versioningService.VersioningService_GetRepository2(id_repo_id = versioningEntry.repository_id.get)
          .map(r => new Repository(clientSet, r.repository.get))
          .flatMap(_.getCommitById(versioningEntry.commit.get))
          .map(commit => ExperimentRunCommit(commit, keyPaths))
      }
    )
  }
}
