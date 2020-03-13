// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class ExperimentRunServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addExperimentRunAttributesAsync(body: ModeldbAddExperimentRunAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunAttributes, ModeldbAddExperimentRunAttributesResponse]("POST", basePath + s"/experiment-run/addExperimentRunAttributes", __query, body, ModeldbAddExperimentRunAttributesResponse.fromJson)
  }

  def addExperimentRunAttributes(body: ModeldbAddExperimentRunAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunAttributesResponse] = Await.result(addExperimentRunAttributesAsync(body), Duration.Inf)

  def addExperimentRunTagAsync(body: ModeldbAddExperimentRunTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunTagResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunTag, ModeldbAddExperimentRunTagResponse]("POST", basePath + s"/experiment-run/addExperimentRunTag", __query, body, ModeldbAddExperimentRunTagResponse.fromJson)
  }

  def addExperimentRunTag(body: ModeldbAddExperimentRunTag)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunTagResponse] = Await.result(addExperimentRunTagAsync(body), Duration.Inf)

  def addExperimentRunTagsAsync(body: ModeldbAddExperimentRunTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunTags, ModeldbAddExperimentRunTagsResponse]("POST", basePath + s"/experiment-run/addExperimentRunTags", __query, body, ModeldbAddExperimentRunTagsResponse.fromJson)
  }

  def addExperimentRunTags(body: ModeldbAddExperimentRunTags)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunTagsResponse] = Await.result(addExperimentRunTagsAsync(body), Duration.Inf)

  def createExperimentRunAsync(body: ModeldbCreateExperimentRun)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateExperimentRunResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateExperimentRun, ModeldbCreateExperimentRunResponse]("POST", basePath + s"/experiment-run/createExperimentRun", __query, body, ModeldbCreateExperimentRunResponse.fromJson)
  }

  def createExperimentRun(body: ModeldbCreateExperimentRun)(implicit ec: ExecutionContext): Try[ModeldbCreateExperimentRunResponse] = Await.result(createExperimentRunAsync(body), Duration.Inf)

  def deleteArtifactAsync(body: ModeldbDeleteArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteArtifact, ModeldbDeleteArtifactResponse]("DELETE", basePath + s"/experiment-run/deleteArtifact", __query, body, ModeldbDeleteArtifactResponse.fromJson)
  }

  def deleteArtifact(body: ModeldbDeleteArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteArtifactResponse] = Await.result(deleteArtifactAsync(body), Duration.Inf)

  def deleteExperimentRunAsync(body: ModeldbDeleteExperimentRun)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRun, ModeldbDeleteExperimentRunResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRun", __query, body, ModeldbDeleteExperimentRunResponse.fromJson)
  }

  def deleteExperimentRun(body: ModeldbDeleteExperimentRun)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunResponse] = Await.result(deleteExperimentRunAsync(body), Duration.Inf)

  def deleteExperimentRunAttributesAsync(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "delete_all" -> client.toQuery(delete_all)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteExperimentRunAttributesResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunAttributes", __query, body, ModeldbDeleteExperimentRunAttributesResponse.fromJson)
  }

  def deleteExperimentRunAttributes(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunAttributesResponse] = Await.result(deleteExperimentRunAttributesAsync(id, attribute_keys, delete_all), Duration.Inf)

  def deleteExperimentRunTagAsync(body: ModeldbDeleteExperimentRunTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunTagResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRunTag, ModeldbDeleteExperimentRunTagResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunTag", __query, body, ModeldbDeleteExperimentRunTagResponse.fromJson)
  }

  def deleteExperimentRunTag(body: ModeldbDeleteExperimentRunTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunTagResponse] = Await.result(deleteExperimentRunTagAsync(body), Duration.Inf)

  def deleteExperimentRunTagsAsync(body: ModeldbDeleteExperimentRunTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRunTags, ModeldbDeleteExperimentRunTagsResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunTags", __query, body, ModeldbDeleteExperimentRunTagsResponse.fromJson)
  }

  def deleteExperimentRunTags(body: ModeldbDeleteExperimentRunTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunTagsResponse] = Await.result(deleteExperimentRunTagsAsync(body), Duration.Inf)

  def deleteExperimentRunsAsync(body: ModeldbDeleteExperimentRuns)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRuns, ModeldbDeleteExperimentRunsResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRuns", __query, body, ModeldbDeleteExperimentRunsResponse.fromJson)
  }

  def deleteExperimentRuns(body: ModeldbDeleteExperimentRuns)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunsResponse] = Await.result(deleteExperimentRunsAsync(body), Duration.Inf)

  def findExperimentRunsAsync(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Future[Try[ModeldbFindExperimentRunsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperimentRuns, ModeldbFindExperimentRunsResponse]("POST", basePath + s"/experiment-run/findExperimentRuns", __query, body, ModeldbFindExperimentRunsResponse.fromJson)
  }

  def findExperimentRuns(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Try[ModeldbFindExperimentRunsResponse] = Await.result(findExperimentRunsAsync(body), Duration.Inf)

  def getArtifactsAsync(id: String, key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "key" -> client.toQuery(key)
    )
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/experiment-run/getArtifacts", __query, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def getArtifacts(id: String, key: String)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(getArtifactsAsync(id, key), Duration.Inf)

  def getChildrenExperimentRunsAsync(experiment_run_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetChildrenExperimentRunsResponse]] = {
    val __query = Map[String,String](
      "experiment_run_id" -> client.toQuery(experiment_run_id),
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetChildrenExperimentRunsResponse]("GET", basePath + s"/experiment-run/getChildrenExperimentRuns", __query, body, ModeldbGetChildrenExperimentRunsResponse.fromJson)
  }

  def getChildrenExperimentRuns(experiment_run_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetChildrenExperimentRunsResponse] = Await.result(getChildrenExperimentRunsAsync(experiment_run_id, page_number, page_limit, ascending, sort_key), Duration.Inf)

  def getDatasetsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetDatasetsResponse]("GET", basePath + s"/experiment-run/getDatasets", __query, body, ModeldbGetDatasetsResponse.fromJson)
  }

  def getDatasets(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetsResponse] = Await.result(getDatasetsAsync(id), Duration.Inf)

  def getExperimentRunAttributesAsync(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "get_all" -> client.toQuery(get_all)
    )
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/experiment-run/getAttributes", __query, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getExperimentRunAttributes(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getExperimentRunAttributesAsync(id, attribute_keys, get_all), Duration.Inf)

  def getExperimentRunByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunByIdResponse]("GET", basePath + s"/experiment-run/getExperimentRunById", __query, body, ModeldbGetExperimentRunByIdResponse.fromJson)
  }

  def getExperimentRunById(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByIdResponse] = Await.result(getExperimentRunByIdAsync(id), Duration.Inf)

  def getExperimentRunByNameAsync(name: String, experiment_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByNameResponse]] = {
    val __query = Map[String,String](
      "name" -> client.toQuery(name),
      "experiment_id" -> client.toQuery(experiment_id)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunByNameResponse]("GET", basePath + s"/experiment-run/getExperimentRunByName", __query, body, ModeldbGetExperimentRunByNameResponse.fromJson)
  }

  def getExperimentRunByName(name: String, experiment_id: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByNameResponse] = Await.result(getExperimentRunByNameAsync(name, experiment_id), Duration.Inf)

  def getExperimentRunCodeVersionAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunCodeVersionResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunCodeVersionResponse]("GET", basePath + s"/experiment-run/getExperimentRunCodeVersion", __query, body, ModeldbGetExperimentRunCodeVersionResponse.fromJson)
  }

  def getExperimentRunCodeVersion(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunCodeVersionResponse] = Await.result(getExperimentRunCodeVersionAsync(id), Duration.Inf)

  def getExperimentRunTagsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/experiment-run/getExperimentRunTags", __query, body, ModeldbGetTagsResponse.fromJson)
  }

  def getExperimentRunTags(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getExperimentRunTagsAsync(id), Duration.Inf)

  def getExperimentRunsByDatasetVersionIdAsync(datset_version_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsByDatasetVersionIdResponse]] = {
    val __query = Map[String,String](
      "datset_version_id" -> client.toQuery(datset_version_id),
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsByDatasetVersionIdResponse]("GET", basePath + s"/experiment-run/getExperimentRunsByDatasetVersionId", __query, body, ModeldbGetExperimentRunsByDatasetVersionIdResponse.fromJson)
  }

  def getExperimentRunsByDatasetVersionId(datset_version_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsByDatasetVersionIdResponse] = Await.result(getExperimentRunsByDatasetVersionIdAsync(datset_version_id, page_number, page_limit, ascending, sort_key), Duration.Inf)

  def getExperimentRunsInExperimentAsync(experiment_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsInExperimentResponse]] = {
    val __query = Map[String,String](
      "experiment_id" -> client.toQuery(experiment_id),
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsInExperimentResponse]("GET", basePath + s"/experiment-run/getExperimentRunsInExperiment", __query, body, ModeldbGetExperimentRunsInExperimentResponse.fromJson)
  }

  def getExperimentRunsInExperiment(experiment_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsInExperimentResponse] = Await.result(getExperimentRunsInExperimentAsync(experiment_id, page_number, page_limit, ascending, sort_key), Duration.Inf)

  def getExperimentRunsInProjectAsync(project_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsInProjectResponse]] = {
    val __query = Map[String,String](
      "project_id" -> client.toQuery(project_id),
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsInProjectResponse]("GET", basePath + s"/experiment-run/getExperimentRunsInProject", __query, body, ModeldbGetExperimentRunsInProjectResponse.fromJson)
  }

  def getExperimentRunsInProject(project_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsInProjectResponse] = Await.result(getExperimentRunsInProjectAsync(project_id, page_number, page_limit, ascending, sort_key), Duration.Inf)

  def getHyperparametersAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHyperparametersResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetHyperparametersResponse]("GET", basePath + s"/experiment-run/getHyperparameters", __query, body, ModeldbGetHyperparametersResponse.fromJson)
  }

  def getHyperparameters(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetHyperparametersResponse] = Await.result(getHyperparametersAsync(id), Duration.Inf)

  def getJobIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetJobIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetJobIdResponse]("GET", basePath + s"/experiment-run/getJobId", __query, body, ModeldbGetJobIdResponse.fromJson)
  }

  def getJobId(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetJobIdResponse] = Await.result(getJobIdAsync(id), Duration.Inf)

  def getMetricsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetMetricsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetMetricsResponse]("GET", basePath + s"/experiment-run/getMetrics", __query, body, ModeldbGetMetricsResponse.fromJson)
  }

  def getMetrics(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetMetricsResponse] = Await.result(getMetricsAsync(id), Duration.Inf)

  def getObservationsAsync(id: String, observation_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetObservationsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "observation_key" -> client.toQuery(observation_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetObservationsResponse]("GET", basePath + s"/experiment-run/getObservations", __query, body, ModeldbGetObservationsResponse.fromJson)
  }

  def getObservations(id: String, observation_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetObservationsResponse] = Await.result(getObservationsAsync(id, observation_key), Duration.Inf)

  def getTopExperimentRunsAsync(project_id: String, experiment_id: String, experiment_run_ids: List[String], sort_key: String, ascending: Boolean, top_k: BigInt, ids_only: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbTopExperimentRunsSelectorResponse]] = {
    val __query = Map[String,String](
      "project_id" -> client.toQuery(project_id),
      "experiment_id" -> client.toQuery(experiment_id),
      "experiment_run_ids" -> client.toQuery(experiment_run_ids),
      "sort_key" -> client.toQuery(sort_key),
      "ascending" -> client.toQuery(ascending),
      "top_k" -> client.toQuery(top_k),
      "ids_only" -> client.toQuery(ids_only)
    )
    val body: String = null
    return client.request[String, ModeldbTopExperimentRunsSelectorResponse]("GET", basePath + s"/experiment-run/getTopExperimentRuns", __query, body, ModeldbTopExperimentRunsSelectorResponse.fromJson)
  }

  def getTopExperimentRuns(project_id: String, experiment_id: String, experiment_run_ids: List[String], sort_key: String, ascending: Boolean, top_k: BigInt, ids_only: Boolean)(implicit ec: ExecutionContext): Try[ModeldbTopExperimentRunsSelectorResponse] = Await.result(getTopExperimentRunsAsync(project_id, experiment_id, experiment_run_ids, sort_key, ascending, top_k, ids_only), Duration.Inf)

  def getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/experiment-run/getUrlForArtifact", __query, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(getUrlForArtifactAsync(body), Duration.Inf)

  def getVersionedInputsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetVersionedInputResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetVersionedInputResponse]("GET", basePath + s"/experiment-run/getVersionedInput", __query, body, ModeldbGetVersionedInputResponse.fromJson)
  }

  def getVersionedInputs(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetVersionedInputResponse] = Await.result(getVersionedInputsAsync(id), Duration.Inf)

  def logArtifactAsync(body: ModeldbLogArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbLogArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogArtifact, ModeldbLogArtifactResponse]("POST", basePath + s"/experiment-run/logArtifact", __query, body, ModeldbLogArtifactResponse.fromJson)
  }

  def logArtifact(body: ModeldbLogArtifact)(implicit ec: ExecutionContext): Try[ModeldbLogArtifactResponse] = Await.result(logArtifactAsync(body), Duration.Inf)

  def logArtifactsAsync(body: ModeldbLogArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogArtifactsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogArtifacts, ModeldbLogArtifactsResponse]("POST", basePath + s"/experiment-run/logArtifacts", __query, body, ModeldbLogArtifactsResponse.fromJson)
  }

  def logArtifacts(body: ModeldbLogArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogArtifactsResponse] = Await.result(logArtifactsAsync(body), Duration.Inf)

  def logAttributeAsync(body: ModeldbLogAttribute)(implicit ec: ExecutionContext): Future[Try[ModeldbLogAttributeResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogAttribute, ModeldbLogAttributeResponse]("POST", basePath + s"/experiment-run/logAttribute", __query, body, ModeldbLogAttributeResponse.fromJson)
  }

  def logAttribute(body: ModeldbLogAttribute)(implicit ec: ExecutionContext): Try[ModeldbLogAttributeResponse] = Await.result(logAttributeAsync(body), Duration.Inf)

  def logAttributesAsync(body: ModeldbLogAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbLogAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogAttributes, ModeldbLogAttributesResponse]("POST", basePath + s"/experiment-run/logAttributes", __query, body, ModeldbLogAttributesResponse.fromJson)
  }

  def logAttributes(body: ModeldbLogAttributes)(implicit ec: ExecutionContext): Try[ModeldbLogAttributesResponse] = Await.result(logAttributesAsync(body), Duration.Inf)

  def logDatasetAsync(body: ModeldbLogDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbLogDatasetResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogDataset, ModeldbLogDatasetResponse]("POST", basePath + s"/experiment-run/logDataset", __query, body, ModeldbLogDatasetResponse.fromJson)
  }

  def logDataset(body: ModeldbLogDataset)(implicit ec: ExecutionContext): Try[ModeldbLogDatasetResponse] = Await.result(logDatasetAsync(body), Duration.Inf)

  def logDatasetsAsync(body: ModeldbLogDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbLogDatasetsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogDatasets, ModeldbLogDatasetsResponse]("POST", basePath + s"/experiment-run/logDatasets", __query, body, ModeldbLogDatasetsResponse.fromJson)
  }

  def logDatasets(body: ModeldbLogDatasets)(implicit ec: ExecutionContext): Try[ModeldbLogDatasetsResponse] = Await.result(logDatasetsAsync(body), Duration.Inf)

  def logExperimentRunCodeVersionAsync(body: ModeldbLogExperimentRunCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentRunCodeVersionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentRunCodeVersion, ModeldbLogExperimentRunCodeVersionResponse]("POST", basePath + s"/experiment-run/logExperimentRunCodeVersion", __query, body, ModeldbLogExperimentRunCodeVersionResponse.fromJson)
  }

  def logExperimentRunCodeVersion(body: ModeldbLogExperimentRunCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentRunCodeVersionResponse] = Await.result(logExperimentRunCodeVersionAsync(body), Duration.Inf)

  def logHyperparameterAsync(body: ModeldbLogHyperparameter)(implicit ec: ExecutionContext): Future[Try[ModeldbLogHyperparameterResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogHyperparameter, ModeldbLogHyperparameterResponse]("POST", basePath + s"/experiment-run/logHyperparameter", __query, body, ModeldbLogHyperparameterResponse.fromJson)
  }

  def logHyperparameter(body: ModeldbLogHyperparameter)(implicit ec: ExecutionContext): Try[ModeldbLogHyperparameterResponse] = Await.result(logHyperparameterAsync(body), Duration.Inf)

  def logHyperparametersAsync(body: ModeldbLogHyperparameters)(implicit ec: ExecutionContext): Future[Try[ModeldbLogHyperparametersResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogHyperparameters, ModeldbLogHyperparametersResponse]("POST", basePath + s"/experiment-run/logHyperparameters", __query, body, ModeldbLogHyperparametersResponse.fromJson)
  }

  def logHyperparameters(body: ModeldbLogHyperparameters)(implicit ec: ExecutionContext): Try[ModeldbLogHyperparametersResponse] = Await.result(logHyperparametersAsync(body), Duration.Inf)

  def logJobIdAsync(id: String, job_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbLogJobIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "job_id" -> client.toQuery(job_id)
    )
    val body: String = null
    return client.request[String, ModeldbLogJobIdResponse]("GET", basePath + s"/experiment-run/logJobId", __query, body, ModeldbLogJobIdResponse.fromJson)
  }

  def logJobId(id: String, job_id: String)(implicit ec: ExecutionContext): Try[ModeldbLogJobIdResponse] = Await.result(logJobIdAsync(id, job_id), Duration.Inf)

  def logMetricAsync(body: ModeldbLogMetric)(implicit ec: ExecutionContext): Future[Try[ModeldbLogMetricResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogMetric, ModeldbLogMetricResponse]("POST", basePath + s"/experiment-run/logMetric", __query, body, ModeldbLogMetricResponse.fromJson)
  }

  def logMetric(body: ModeldbLogMetric)(implicit ec: ExecutionContext): Try[ModeldbLogMetricResponse] = Await.result(logMetricAsync(body), Duration.Inf)

  def logMetricsAsync(body: ModeldbLogMetrics)(implicit ec: ExecutionContext): Future[Try[ModeldbLogMetricsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogMetrics, ModeldbLogMetricsResponse]("POST", basePath + s"/experiment-run/logMetrics", __query, body, ModeldbLogMetricsResponse.fromJson)
  }

  def logMetrics(body: ModeldbLogMetrics)(implicit ec: ExecutionContext): Try[ModeldbLogMetricsResponse] = Await.result(logMetricsAsync(body), Duration.Inf)

  def logObservationAsync(body: ModeldbLogObservation)(implicit ec: ExecutionContext): Future[Try[ModeldbLogObservationResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogObservation, ModeldbLogObservationResponse]("POST", basePath + s"/experiment-run/logObservation", __query, body, ModeldbLogObservationResponse.fromJson)
  }

  def logObservation(body: ModeldbLogObservation)(implicit ec: ExecutionContext): Try[ModeldbLogObservationResponse] = Await.result(logObservationAsync(body), Duration.Inf)

  def logObservationsAsync(body: ModeldbLogObservations)(implicit ec: ExecutionContext): Future[Try[ModeldbLogObservationsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogObservations, ModeldbLogObservationsResponse]("POST", basePath + s"/experiment-run/logObservations", __query, body, ModeldbLogObservationsResponse.fromJson)
  }

  def logObservations(body: ModeldbLogObservations)(implicit ec: ExecutionContext): Try[ModeldbLogObservationsResponse] = Await.result(logObservationsAsync(body), Duration.Inf)

  def logVersionedInputAsync(body: ModeldbLogVersionedInput)(implicit ec: ExecutionContext): Future[Try[ModeldbLogVersionedInputResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogVersionedInput, ModeldbLogVersionedInputResponse]("POST", basePath + s"/experiment-run/logVersionedInput", __query, body, ModeldbLogVersionedInputResponse.fromJson)
  }

  def logVersionedInput(body: ModeldbLogVersionedInput)(implicit ec: ExecutionContext): Try[ModeldbLogVersionedInputResponse] = Await.result(logVersionedInputAsync(body), Duration.Inf)

  def setParentExperimentRunIdAsync(body: ModeldbSetParentExperimentRunId)(implicit ec: ExecutionContext): Future[Try[ModeldbSetParentExperimentRunIdResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetParentExperimentRunId, ModeldbSetParentExperimentRunIdResponse]("POST", basePath + s"/experiment-run/setParentExperimentRunId", __query, body, ModeldbSetParentExperimentRunIdResponse.fromJson)
  }

  def setParentExperimentRunId(body: ModeldbSetParentExperimentRunId)(implicit ec: ExecutionContext): Try[ModeldbSetParentExperimentRunIdResponse] = Await.result(setParentExperimentRunIdAsync(body), Duration.Inf)

  def sortExperimentRunsAsync(experiment_run_ids: List[String], sort_key: String, ascending: Boolean, ids_only: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbSortExperimentRunsResponse]] = {
    val __query = Map[String,String](
      "experiment_run_ids" -> client.toQuery(experiment_run_ids),
      "sort_key" -> client.toQuery(sort_key),
      "ascending" -> client.toQuery(ascending),
      "ids_only" -> client.toQuery(ids_only)
    )
    val body: String = null
    return client.request[String, ModeldbSortExperimentRunsResponse]("GET", basePath + s"/experiment-run/sortExperimentRuns", __query, body, ModeldbSortExperimentRunsResponse.fromJson)
  }

  def sortExperimentRuns(experiment_run_ids: List[String], sort_key: String, ascending: Boolean, ids_only: Boolean)(implicit ec: ExecutionContext): Try[ModeldbSortExperimentRunsResponse] = Await.result(sortExperimentRunsAsync(experiment_run_ids, sort_key, ascending, ids_only), Duration.Inf)

  def updateExperimentRunDescriptionAsync(body: ModeldbUpdateExperimentRunDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentRunDescriptionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentRunDescription, ModeldbUpdateExperimentRunDescriptionResponse]("POST", basePath + s"/experiment-run/updateExperimentRunDescription", __query, body, ModeldbUpdateExperimentRunDescriptionResponse.fromJson)
  }

  def updateExperimentRunDescription(body: ModeldbUpdateExperimentRunDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentRunDescriptionResponse] = Await.result(updateExperimentRunDescriptionAsync(body), Duration.Inf)

  def updateExperimentRunNameAsync(body: ModeldbUpdateExperimentRunName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentRunNameResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentRunName, ModeldbUpdateExperimentRunNameResponse]("POST", basePath + s"/experiment-run/updateExperimentRunName", __query, body, ModeldbUpdateExperimentRunNameResponse.fromJson)
  }

  def updateExperimentRunName(body: ModeldbUpdateExperimentRunName)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentRunNameResponse] = Await.result(updateExperimentRunNameAsync(body), Duration.Inf)

}
