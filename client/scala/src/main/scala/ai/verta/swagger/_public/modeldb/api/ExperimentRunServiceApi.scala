// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class ExperimentRunServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def ExperimentRunService_ListBlobExperimentRunsAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbListBlobExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, ModeldbListBlobExperimentRunsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/path/runs", __query.toMap, body, ModeldbListBlobExperimentRunsRequestResponse.fromJson)
  }

  def ExperimentRunService_ListBlobExperimentRuns(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[ModeldbListBlobExperimentRunsRequestResponse] = Await.result(ExperimentRunService_ListBlobExperimentRunsAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, location, pagination_page_limit, pagination_page_number, repository_id_repo_id), Duration.Inf)

  def ExperimentRunService_ListBlobExperimentRuns2Async(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbListBlobExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (location.isDefined) __query.update("location", client.toQuery(location.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, ModeldbListBlobExperimentRunsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/path/runs", __query.toMap, body, ModeldbListBlobExperimentRunsRequestResponse.fromJson)
  }

  def ExperimentRunService_ListBlobExperimentRuns2(commit_sha: String, repository_id_repo_id: BigInt, location: Option[List[String]]=None, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbListBlobExperimentRunsRequestResponse] = Await.result(ExperimentRunService_ListBlobExperimentRuns2Async(commit_sha, repository_id_repo_id, location, pagination_page_limit, pagination_page_number, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ExperimentRunService_ListCommitExperimentRunsAsync(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbListCommitExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_repo_id.isDefined) __query.update("repository_id.repo_id", client.toQuery(repository_id_repo_id.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (repository_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_workspace_name\"")
    if (repository_id_named_id_name == null) throw new Exception("Missing required parameter \"repository_id_named_id_name\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, ModeldbListCommitExperimentRunsRequestResponse]("GET", basePath + s"/versioning/workspaces/$repository_id_named_id_workspace_name/repositories/$repository_id_named_id_name/commits/$commit_sha/runs", __query.toMap, body, ModeldbListCommitExperimentRunsRequestResponse.fromJson)
  }

  def ExperimentRunService_ListCommitExperimentRuns(commit_sha: String, repository_id_named_id_name: String, repository_id_named_id_workspace_name: String, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_repo_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[ModeldbListCommitExperimentRunsRequestResponse] = Await.result(ExperimentRunService_ListCommitExperimentRunsAsync(commit_sha, repository_id_named_id_name, repository_id_named_id_workspace_name, pagination_page_limit, pagination_page_number, repository_id_repo_id), Duration.Inf)

  def ExperimentRunService_ListCommitExperimentRuns2Async(commit_sha: String, repository_id_repo_id: BigInt, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbListCommitExperimentRunsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (repository_id_named_id_name.isDefined) __query.update("repository_id.named_id.name", client.toQuery(repository_id_named_id_name.get))
    if (repository_id_named_id_workspace_name.isDefined) __query.update("repository_id.named_id.workspace_name", client.toQuery(repository_id_named_id_workspace_name.get))
    if (pagination_page_number.isDefined) __query.update("pagination.page_number", client.toQuery(pagination_page_number.get))
    if (pagination_page_limit.isDefined) __query.update("pagination.page_limit", client.toQuery(pagination_page_limit.get))
    if (repository_id_repo_id == null) throw new Exception("Missing required parameter \"repository_id_repo_id\"")
    if (commit_sha == null) throw new Exception("Missing required parameter \"commit_sha\"")
    val body: String = null
    return client.request[String, ModeldbListCommitExperimentRunsRequestResponse]("GET", basePath + s"/versioning/repositories/$repository_id_repo_id/commits/$commit_sha/runs", __query.toMap, body, ModeldbListCommitExperimentRunsRequestResponse.fromJson)
  }

  def ExperimentRunService_ListCommitExperimentRuns2(commit_sha: String, repository_id_repo_id: BigInt, pagination_page_limit: Option[BigInt]=None, pagination_page_number: Option[BigInt]=None, repository_id_named_id_name: Option[String]=None, repository_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbListCommitExperimentRunsRequestResponse] = Await.result(ExperimentRunService_ListCommitExperimentRuns2Async(commit_sha, repository_id_repo_id, pagination_page_limit, pagination_page_number, repository_id_named_id_name, repository_id_named_id_workspace_name), Duration.Inf)

  def ExperimentRunService_addExperimentRunAttributesAsync(body: ModeldbAddExperimentRunAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunAttributes, ModeldbAddExperimentRunAttributesResponse]("POST", basePath + s"/experiment-run/addExperimentRunAttributes", __query.toMap, body, ModeldbAddExperimentRunAttributesResponse.fromJson)
  }

  def ExperimentRunService_addExperimentRunAttributes(body: ModeldbAddExperimentRunAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunAttributesResponse] = Await.result(ExperimentRunService_addExperimentRunAttributesAsync(body), Duration.Inf)

  def ExperimentRunService_addExperimentRunTagAsync(body: ModeldbAddExperimentRunTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunTag, ModeldbAddExperimentRunTagResponse]("POST", basePath + s"/experiment-run/addExperimentRunTag", __query.toMap, body, ModeldbAddExperimentRunTagResponse.fromJson)
  }

  def ExperimentRunService_addExperimentRunTag(body: ModeldbAddExperimentRunTag)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunTagResponse] = Await.result(ExperimentRunService_addExperimentRunTagAsync(body), Duration.Inf)

  def ExperimentRunService_addExperimentRunTagsAsync(body: ModeldbAddExperimentRunTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentRunTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentRunTags, ModeldbAddExperimentRunTagsResponse]("POST", basePath + s"/experiment-run/addExperimentRunTags", __query.toMap, body, ModeldbAddExperimentRunTagsResponse.fromJson)
  }

  def ExperimentRunService_addExperimentRunTags(body: ModeldbAddExperimentRunTags)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentRunTagsResponse] = Await.result(ExperimentRunService_addExperimentRunTagsAsync(body), Duration.Inf)

  def ExperimentRunService_commitArtifactPartAsync(body: ModeldbCommitArtifactPart)(implicit ec: ExecutionContext): Future[Try[ModeldbCommitArtifactPartResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCommitArtifactPart, ModeldbCommitArtifactPartResponse]("POST", basePath + s"/experiment-run/commitArtifactPart", __query.toMap, body, ModeldbCommitArtifactPartResponse.fromJson)
  }

  def ExperimentRunService_commitArtifactPart(body: ModeldbCommitArtifactPart)(implicit ec: ExecutionContext): Try[ModeldbCommitArtifactPartResponse] = Await.result(ExperimentRunService_commitArtifactPartAsync(body), Duration.Inf)

  def ExperimentRunService_commitMultipartArtifactAsync(body: ModeldbCommitMultipartArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbCommitMultipartArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCommitMultipartArtifact, ModeldbCommitMultipartArtifactResponse]("POST", basePath + s"/experiment-run/commitMultipartArtifact", __query.toMap, body, ModeldbCommitMultipartArtifactResponse.fromJson)
  }

  def ExperimentRunService_commitMultipartArtifact(body: ModeldbCommitMultipartArtifact)(implicit ec: ExecutionContext): Try[ModeldbCommitMultipartArtifactResponse] = Await.result(ExperimentRunService_commitMultipartArtifactAsync(body), Duration.Inf)

  def ExperimentRunService_createExperimentRunAsync(body: ModeldbCreateExperimentRun)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateExperimentRunResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateExperimentRun, ModeldbCreateExperimentRunResponse]("POST", basePath + s"/experiment-run/createExperimentRun", __query.toMap, body, ModeldbCreateExperimentRunResponse.fromJson)
  }

  def ExperimentRunService_createExperimentRun(body: ModeldbCreateExperimentRun)(implicit ec: ExecutionContext): Try[ModeldbCreateExperimentRunResponse] = Await.result(ExperimentRunService_createExperimentRunAsync(body), Duration.Inf)

  def ExperimentRunService_deleteArtifactAsync(body: ModeldbDeleteArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteArtifact, ModeldbDeleteArtifactResponse]("DELETE", basePath + s"/experiment-run/deleteArtifact", __query.toMap, body, ModeldbDeleteArtifactResponse.fromJson)
  }

  def ExperimentRunService_deleteArtifact(body: ModeldbDeleteArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteArtifactResponse] = Await.result(ExperimentRunService_deleteArtifactAsync(body), Duration.Inf)

  def ExperimentRunService_deleteExperimentRunAsync(body: ModeldbDeleteExperimentRun)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRun, ModeldbDeleteExperimentRunResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRun", __query.toMap, body, ModeldbDeleteExperimentRunResponse.fromJson)
  }

  def ExperimentRunService_deleteExperimentRun(body: ModeldbDeleteExperimentRun)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunResponse] = Await.result(ExperimentRunService_deleteExperimentRunAsync(body), Duration.Inf)

  def ExperimentRunService_deleteExperimentRunAttributesAsync(body: ModeldbDeleteExperimentRunAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRunAttributes, ModeldbDeleteExperimentRunAttributesResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunAttributes", __query.toMap, body, ModeldbDeleteExperimentRunAttributesResponse.fromJson)
  }

  def ExperimentRunService_deleteExperimentRunAttributes(body: ModeldbDeleteExperimentRunAttributes)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunAttributesResponse] = Await.result(ExperimentRunService_deleteExperimentRunAttributesAsync(body), Duration.Inf)

  def ExperimentRunService_deleteExperimentRunTagAsync(body: ModeldbDeleteExperimentRunTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRunTag, ModeldbDeleteExperimentRunTagResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunTag", __query.toMap, body, ModeldbDeleteExperimentRunTagResponse.fromJson)
  }

  def ExperimentRunService_deleteExperimentRunTag(body: ModeldbDeleteExperimentRunTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunTagResponse] = Await.result(ExperimentRunService_deleteExperimentRunTagAsync(body), Duration.Inf)

  def ExperimentRunService_deleteExperimentRunTagsAsync(body: ModeldbDeleteExperimentRunTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRunTags, ModeldbDeleteExperimentRunTagsResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRunTags", __query.toMap, body, ModeldbDeleteExperimentRunTagsResponse.fromJson)
  }

  def ExperimentRunService_deleteExperimentRunTags(body: ModeldbDeleteExperimentRunTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunTagsResponse] = Await.result(ExperimentRunService_deleteExperimentRunTagsAsync(body), Duration.Inf)

  def ExperimentRunService_deleteExperimentRunsAsync(body: ModeldbDeleteExperimentRuns)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentRuns, ModeldbDeleteExperimentRunsResponse]("DELETE", basePath + s"/experiment-run/deleteExperimentRuns", __query.toMap, body, ModeldbDeleteExperimentRunsResponse.fromJson)
  }

  def ExperimentRunService_deleteExperimentRuns(body: ModeldbDeleteExperimentRuns)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentRunsResponse] = Await.result(ExperimentRunService_deleteExperimentRunsAsync(body), Duration.Inf)

  def ExperimentRunService_deleteHyperparametersAsync(body: ModeldbDeleteHyperparameters)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteHyperparametersResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteHyperparameters, ModeldbDeleteHyperparametersResponse]("DELETE", basePath + s"/experiment-run/deleteHyperparameters", __query.toMap, body, ModeldbDeleteHyperparametersResponse.fromJson)
  }

  def ExperimentRunService_deleteHyperparameters(body: ModeldbDeleteHyperparameters)(implicit ec: ExecutionContext): Try[ModeldbDeleteHyperparametersResponse] = Await.result(ExperimentRunService_deleteHyperparametersAsync(body), Duration.Inf)

  def ExperimentRunService_deleteMetricsAsync(body: ModeldbDeleteMetrics)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteMetricsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteMetrics, ModeldbDeleteMetricsResponse]("DELETE", basePath + s"/experiment-run/deleteMetrics", __query.toMap, body, ModeldbDeleteMetricsResponse.fromJson)
  }

  def ExperimentRunService_deleteMetrics(body: ModeldbDeleteMetrics)(implicit ec: ExecutionContext): Try[ModeldbDeleteMetricsResponse] = Await.result(ExperimentRunService_deleteMetricsAsync(body), Duration.Inf)

  def ExperimentRunService_deleteObservationsAsync(body: ModeldbDeleteObservations)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteObservationsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteObservations, ModeldbDeleteObservationsResponse]("DELETE", basePath + s"/experiment-run/deleteObservations", __query.toMap, body, ModeldbDeleteObservationsResponse.fromJson)
  }

  def ExperimentRunService_deleteObservations(body: ModeldbDeleteObservations)(implicit ec: ExecutionContext): Try[ModeldbDeleteObservationsResponse] = Await.result(ExperimentRunService_deleteObservationsAsync(body), Duration.Inf)

  def ExperimentRunService_findExperimentRunsAsync(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Future[Try[ModeldbFindExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperimentRuns, ModeldbFindExperimentRunsResponse]("POST", basePath + s"/experiment-run/findExperimentRuns", __query.toMap, body, ModeldbFindExperimentRunsResponse.fromJson)
  }

  def ExperimentRunService_findExperimentRuns(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Try[ModeldbFindExperimentRunsResponse] = Await.result(ExperimentRunService_findExperimentRunsAsync(body), Duration.Inf)

  def ExperimentRunService_getArtifactsAsync(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/experiment-run/getArtifacts", __query.toMap, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def ExperimentRunService_getArtifacts(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(ExperimentRunService_getArtifactsAsync(id, key), Duration.Inf)

  def ExperimentRunService_getChildrenExperimentRunsAsync(ascending: Option[Boolean]=None, experiment_run_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetChildrenExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (experiment_run_id.isDefined) __query.update("experiment_run_id", client.toQuery(experiment_run_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetChildrenExperimentRunsResponse]("GET", basePath + s"/experiment-run/getChildrenExperimentRuns", __query.toMap, body, ModeldbGetChildrenExperimentRunsResponse.fromJson)
  }

  def ExperimentRunService_getChildrenExperimentRuns(ascending: Option[Boolean]=None, experiment_run_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetChildrenExperimentRunsResponse] = Await.result(ExperimentRunService_getChildrenExperimentRunsAsync(ascending, experiment_run_id, page_limit, page_number, sort_key), Duration.Inf)

  def ExperimentRunService_getCommittedArtifactPartsAsync(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetCommittedArtifactPartsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ModeldbGetCommittedArtifactPartsResponse]("GET", basePath + s"/experiment-run/getCommittedArtifactParts", __query.toMap, body, ModeldbGetCommittedArtifactPartsResponse.fromJson)
  }

  def ExperimentRunService_getCommittedArtifactParts(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetCommittedArtifactPartsResponse] = Await.result(ExperimentRunService_getCommittedArtifactPartsAsync(id, key), Duration.Inf)

  def ExperimentRunService_getDatasetsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetsResponse]("GET", basePath + s"/experiment-run/getDatasets", __query.toMap, body, ModeldbGetDatasetsResponse.fromJson)
  }

  def ExperimentRunService_getDatasets(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetsResponse] = Await.result(ExperimentRunService_getDatasetsAsync(id), Duration.Inf)

  def ExperimentRunService_getExperimentRunAttributesAsync(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/experiment-run/getAttributes", __query.toMap, body, ModeldbGetAttributesResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunAttributes(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(ExperimentRunService_getExperimentRunAttributesAsync(attribute_keys, get_all, id), Duration.Inf)

  def ExperimentRunService_getExperimentRunByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunByIdResponse]("GET", basePath + s"/experiment-run/getExperimentRunById", __query.toMap, body, ModeldbGetExperimentRunByIdResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByIdResponse] = Await.result(ExperimentRunService_getExperimentRunByIdAsync(id), Duration.Inf)

  def ExperimentRunService_getExperimentRunByNameAsync(experiment_id: Option[String]=None, name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (experiment_id.isDefined) __query.update("experiment_id", client.toQuery(experiment_id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunByNameResponse]("GET", basePath + s"/experiment-run/getExperimentRunByName", __query.toMap, body, ModeldbGetExperimentRunByNameResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunByName(experiment_id: Option[String]=None, name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByNameResponse] = Await.result(ExperimentRunService_getExperimentRunByNameAsync(experiment_id, name), Duration.Inf)

  def ExperimentRunService_getExperimentRunCodeVersionAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunCodeVersionResponse]("GET", basePath + s"/experiment-run/getExperimentRunCodeVersion", __query.toMap, body, ModeldbGetExperimentRunCodeVersionResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunCodeVersion(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunCodeVersionResponse] = Await.result(ExperimentRunService_getExperimentRunCodeVersionAsync(id), Duration.Inf)

  def ExperimentRunService_getExperimentRunTagsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/experiment-run/getExperimentRunTags", __query.toMap, body, ModeldbGetTagsResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunTags(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(ExperimentRunService_getExperimentRunTagsAsync(id), Duration.Inf)

  def ExperimentRunService_getExperimentRunsByDatasetVersionIdAsync(ascending: Option[Boolean]=None, dataset_version_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsByDatasetVersionIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_version_id.isDefined) __query.update("dataset_version_id", client.toQuery(dataset_version_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsByDatasetVersionIdResponse]("GET", basePath + s"/experiment-run/getExperimentRunsByDatasetVersionId", __query.toMap, body, ModeldbGetExperimentRunsByDatasetVersionIdResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunsByDatasetVersionId(ascending: Option[Boolean]=None, dataset_version_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsByDatasetVersionIdResponse] = Await.result(ExperimentRunService_getExperimentRunsByDatasetVersionIdAsync(ascending, dataset_version_id, page_limit, page_number, sort_key), Duration.Inf)

  def ExperimentRunService_getExperimentRunsInExperimentAsync(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsInExperimentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (experiment_id.isDefined) __query.update("experiment_id", client.toQuery(experiment_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsInExperimentResponse]("GET", basePath + s"/experiment-run/getExperimentRunsInExperiment", __query.toMap, body, ModeldbGetExperimentRunsInExperimentResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunsInExperiment(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsInExperimentResponse] = Await.result(ExperimentRunService_getExperimentRunsInExperimentAsync(ascending, experiment_id, page_limit, page_number, sort_key), Duration.Inf)

  def ExperimentRunService_getExperimentRunsInProjectAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunsInProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentRunsInProjectResponse]("GET", basePath + s"/experiment-run/getExperimentRunsInProject", __query.toMap, body, ModeldbGetExperimentRunsInProjectResponse.fromJson)
  }

  def ExperimentRunService_getExperimentRunsInProject(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunsInProjectResponse] = Await.result(ExperimentRunService_getExperimentRunsInProjectAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def ExperimentRunService_getHyperparametersAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHyperparametersResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetHyperparametersResponse]("GET", basePath + s"/experiment-run/getHyperparameters", __query.toMap, body, ModeldbGetHyperparametersResponse.fromJson)
  }

  def ExperimentRunService_getHyperparameters(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHyperparametersResponse] = Await.result(ExperimentRunService_getHyperparametersAsync(id), Duration.Inf)

  def ExperimentRunService_getJobIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetJobIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetJobIdResponse]("GET", basePath + s"/experiment-run/getJobId", __query.toMap, body, ModeldbGetJobIdResponse.fromJson)
  }

  def ExperimentRunService_getJobId(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetJobIdResponse] = Await.result(ExperimentRunService_getJobIdAsync(id), Duration.Inf)

  def ExperimentRunService_getMetricsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetMetricsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetMetricsResponse]("GET", basePath + s"/experiment-run/getMetrics", __query.toMap, body, ModeldbGetMetricsResponse.fromJson)
  }

  def ExperimentRunService_getMetrics(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetMetricsResponse] = Await.result(ExperimentRunService_getMetricsAsync(id), Duration.Inf)

  def ExperimentRunService_getObservationsAsync(id: Option[String]=None, observation_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetObservationsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (observation_key.isDefined) __query.update("observation_key", client.toQuery(observation_key.get))
    val body: String = null
    return client.request[String, ModeldbGetObservationsResponse]("GET", basePath + s"/experiment-run/getObservations", __query.toMap, body, ModeldbGetObservationsResponse.fromJson)
  }

  def ExperimentRunService_getObservations(id: Option[String]=None, observation_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetObservationsResponse] = Await.result(ExperimentRunService_getObservationsAsync(id, observation_key), Duration.Inf)

  def ExperimentRunService_getTopExperimentRunsAsync(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, project_id: Option[String]=None, sort_key: Option[String]=None, top_k: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbTopExperimentRunsSelectorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (experiment_id.isDefined) __query.update("experiment_id", client.toQuery(experiment_id.get))
    if (experiment_run_ids.isDefined) __query.update("experiment_run_ids", client.toQuery(experiment_run_ids.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (top_k.isDefined) __query.update("top_k", client.toQuery(top_k.get))
    if (ids_only.isDefined) __query.update("ids_only", client.toQuery(ids_only.get))
    val body: String = null
    return client.request[String, ModeldbTopExperimentRunsSelectorResponse]("GET", basePath + s"/experiment-run/getTopExperimentRuns", __query.toMap, body, ModeldbTopExperimentRunsSelectorResponse.fromJson)
  }

  def ExperimentRunService_getTopExperimentRuns(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, project_id: Option[String]=None, sort_key: Option[String]=None, top_k: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[ModeldbTopExperimentRunsSelectorResponse] = Await.result(ExperimentRunService_getTopExperimentRunsAsync(ascending, experiment_id, experiment_run_ids, ids_only, project_id, sort_key, top_k), Duration.Inf)

  def ExperimentRunService_getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/experiment-run/getUrlForArtifact", __query.toMap, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def ExperimentRunService_getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(ExperimentRunService_getUrlForArtifactAsync(body), Duration.Inf)

  def ExperimentRunService_getVersionedInputsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetVersionedInputResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetVersionedInputResponse]("GET", basePath + s"/experiment-run/getVersionedInput", __query.toMap, body, ModeldbGetVersionedInputResponse.fromJson)
  }

  def ExperimentRunService_getVersionedInputs(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetVersionedInputResponse] = Await.result(ExperimentRunService_getVersionedInputsAsync(id), Duration.Inf)

  def ExperimentRunService_logArtifactAsync(body: ModeldbLogArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbLogArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogArtifact, ModeldbLogArtifactResponse]("POST", basePath + s"/experiment-run/logArtifact", __query.toMap, body, ModeldbLogArtifactResponse.fromJson)
  }

  def ExperimentRunService_logArtifact(body: ModeldbLogArtifact)(implicit ec: ExecutionContext): Try[ModeldbLogArtifactResponse] = Await.result(ExperimentRunService_logArtifactAsync(body), Duration.Inf)

  def ExperimentRunService_logArtifactsAsync(body: ModeldbLogArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogArtifacts, ModeldbLogArtifactsResponse]("POST", basePath + s"/experiment-run/logArtifacts", __query.toMap, body, ModeldbLogArtifactsResponse.fromJson)
  }

  def ExperimentRunService_logArtifacts(body: ModeldbLogArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogArtifactsResponse] = Await.result(ExperimentRunService_logArtifactsAsync(body), Duration.Inf)

  def ExperimentRunService_logAttributeAsync(body: ModeldbLogAttribute)(implicit ec: ExecutionContext): Future[Try[ModeldbLogAttributeResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogAttribute, ModeldbLogAttributeResponse]("POST", basePath + s"/experiment-run/logAttribute", __query.toMap, body, ModeldbLogAttributeResponse.fromJson)
  }

  def ExperimentRunService_logAttribute(body: ModeldbLogAttribute)(implicit ec: ExecutionContext): Try[ModeldbLogAttributeResponse] = Await.result(ExperimentRunService_logAttributeAsync(body), Duration.Inf)

  def ExperimentRunService_logAttributesAsync(body: ModeldbLogAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbLogAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogAttributes, ModeldbLogAttributesResponse]("POST", basePath + s"/experiment-run/logAttributes", __query.toMap, body, ModeldbLogAttributesResponse.fromJson)
  }

  def ExperimentRunService_logAttributes(body: ModeldbLogAttributes)(implicit ec: ExecutionContext): Try[ModeldbLogAttributesResponse] = Await.result(ExperimentRunService_logAttributesAsync(body), Duration.Inf)

  def ExperimentRunService_logDatasetAsync(body: ModeldbLogDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbLogDatasetResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogDataset, ModeldbLogDatasetResponse]("POST", basePath + s"/experiment-run/logDataset", __query.toMap, body, ModeldbLogDatasetResponse.fromJson)
  }

  def ExperimentRunService_logDataset(body: ModeldbLogDataset)(implicit ec: ExecutionContext): Try[ModeldbLogDatasetResponse] = Await.result(ExperimentRunService_logDatasetAsync(body), Duration.Inf)

  def ExperimentRunService_logDatasetsAsync(body: ModeldbLogDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbLogDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogDatasets, ModeldbLogDatasetsResponse]("POST", basePath + s"/experiment-run/logDatasets", __query.toMap, body, ModeldbLogDatasetsResponse.fromJson)
  }

  def ExperimentRunService_logDatasets(body: ModeldbLogDatasets)(implicit ec: ExecutionContext): Try[ModeldbLogDatasetsResponse] = Await.result(ExperimentRunService_logDatasetsAsync(body), Duration.Inf)

  def ExperimentRunService_logExperimentRunCodeVersionAsync(body: ModeldbLogExperimentRunCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentRunCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentRunCodeVersion, ModeldbLogExperimentRunCodeVersionResponse]("POST", basePath + s"/experiment-run/logExperimentRunCodeVersion", __query.toMap, body, ModeldbLogExperimentRunCodeVersionResponse.fromJson)
  }

  def ExperimentRunService_logExperimentRunCodeVersion(body: ModeldbLogExperimentRunCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentRunCodeVersionResponse] = Await.result(ExperimentRunService_logExperimentRunCodeVersionAsync(body), Duration.Inf)

  def ExperimentRunService_logHyperparameterAsync(body: ModeldbLogHyperparameter)(implicit ec: ExecutionContext): Future[Try[ModeldbLogHyperparameterResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogHyperparameter, ModeldbLogHyperparameterResponse]("POST", basePath + s"/experiment-run/logHyperparameter", __query.toMap, body, ModeldbLogHyperparameterResponse.fromJson)
  }

  def ExperimentRunService_logHyperparameter(body: ModeldbLogHyperparameter)(implicit ec: ExecutionContext): Try[ModeldbLogHyperparameterResponse] = Await.result(ExperimentRunService_logHyperparameterAsync(body), Duration.Inf)

  def ExperimentRunService_logHyperparametersAsync(body: ModeldbLogHyperparameters)(implicit ec: ExecutionContext): Future[Try[ModeldbLogHyperparametersResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogHyperparameters, ModeldbLogHyperparametersResponse]("POST", basePath + s"/experiment-run/logHyperparameters", __query.toMap, body, ModeldbLogHyperparametersResponse.fromJson)
  }

  def ExperimentRunService_logHyperparameters(body: ModeldbLogHyperparameters)(implicit ec: ExecutionContext): Try[ModeldbLogHyperparametersResponse] = Await.result(ExperimentRunService_logHyperparametersAsync(body), Duration.Inf)

  def ExperimentRunService_logJobIdAsync(id: Option[String]=None, job_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbLogJobIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (job_id.isDefined) __query.update("job_id", client.toQuery(job_id.get))
    val body: String = null
    return client.request[String, ModeldbLogJobIdResponse]("GET", basePath + s"/experiment-run/logJobId", __query.toMap, body, ModeldbLogJobIdResponse.fromJson)
  }

  def ExperimentRunService_logJobId(id: Option[String]=None, job_id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbLogJobIdResponse] = Await.result(ExperimentRunService_logJobIdAsync(id, job_id), Duration.Inf)

  def ExperimentRunService_logMetricAsync(body: ModeldbLogMetric)(implicit ec: ExecutionContext): Future[Try[ModeldbLogMetricResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogMetric, ModeldbLogMetricResponse]("POST", basePath + s"/experiment-run/logMetric", __query.toMap, body, ModeldbLogMetricResponse.fromJson)
  }

  def ExperimentRunService_logMetric(body: ModeldbLogMetric)(implicit ec: ExecutionContext): Try[ModeldbLogMetricResponse] = Await.result(ExperimentRunService_logMetricAsync(body), Duration.Inf)

  def ExperimentRunService_logMetricsAsync(body: ModeldbLogMetrics)(implicit ec: ExecutionContext): Future[Try[ModeldbLogMetricsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogMetrics, ModeldbLogMetricsResponse]("POST", basePath + s"/experiment-run/logMetrics", __query.toMap, body, ModeldbLogMetricsResponse.fromJson)
  }

  def ExperimentRunService_logMetrics(body: ModeldbLogMetrics)(implicit ec: ExecutionContext): Try[ModeldbLogMetricsResponse] = Await.result(ExperimentRunService_logMetricsAsync(body), Duration.Inf)

  def ExperimentRunService_logObservationAsync(body: ModeldbLogObservation)(implicit ec: ExecutionContext): Future[Try[ModeldbLogObservationResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogObservation, ModeldbLogObservationResponse]("POST", basePath + s"/experiment-run/logObservation", __query.toMap, body, ModeldbLogObservationResponse.fromJson)
  }

  def ExperimentRunService_logObservation(body: ModeldbLogObservation)(implicit ec: ExecutionContext): Try[ModeldbLogObservationResponse] = Await.result(ExperimentRunService_logObservationAsync(body), Duration.Inf)

  def ExperimentRunService_logObservationsAsync(body: ModeldbLogObservations)(implicit ec: ExecutionContext): Future[Try[ModeldbLogObservationsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogObservations, ModeldbLogObservationsResponse]("POST", basePath + s"/experiment-run/logObservations", __query.toMap, body, ModeldbLogObservationsResponse.fromJson)
  }

  def ExperimentRunService_logObservations(body: ModeldbLogObservations)(implicit ec: ExecutionContext): Try[ModeldbLogObservationsResponse] = Await.result(ExperimentRunService_logObservationsAsync(body), Duration.Inf)

  def ExperimentRunService_logVersionedInputAsync(body: ModeldbLogVersionedInput)(implicit ec: ExecutionContext): Future[Try[ModeldbLogVersionedInputResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogVersionedInput, ModeldbLogVersionedInputResponse]("POST", basePath + s"/experiment-run/logVersionedInput", __query.toMap, body, ModeldbLogVersionedInputResponse.fromJson)
  }

  def ExperimentRunService_logVersionedInput(body: ModeldbLogVersionedInput)(implicit ec: ExecutionContext): Try[ModeldbLogVersionedInputResponse] = Await.result(ExperimentRunService_logVersionedInputAsync(body), Duration.Inf)

  def ExperimentRunService_setParentExperimentRunIdAsync(body: ModeldbSetParentExperimentRunId)(implicit ec: ExecutionContext): Future[Try[ModeldbSetParentExperimentRunIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetParentExperimentRunId, ModeldbSetParentExperimentRunIdResponse]("POST", basePath + s"/experiment-run/setParentExperimentRunId", __query.toMap, body, ModeldbSetParentExperimentRunIdResponse.fromJson)
  }

  def ExperimentRunService_setParentExperimentRunId(body: ModeldbSetParentExperimentRunId)(implicit ec: ExecutionContext): Try[ModeldbSetParentExperimentRunIdResponse] = Await.result(ExperimentRunService_setParentExperimentRunIdAsync(body), Duration.Inf)

  def ExperimentRunService_sortExperimentRunsAsync(ascending: Option[Boolean]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbSortExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (experiment_run_ids.isDefined) __query.update("experiment_run_ids", client.toQuery(experiment_run_ids.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (ids_only.isDefined) __query.update("ids_only", client.toQuery(ids_only.get))
    val body: String = null
    return client.request[String, ModeldbSortExperimentRunsResponse]("GET", basePath + s"/experiment-run/sortExperimentRuns", __query.toMap, body, ModeldbSortExperimentRunsResponse.fromJson)
  }

  def ExperimentRunService_sortExperimentRuns(ascending: Option[Boolean]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbSortExperimentRunsResponse] = Await.result(ExperimentRunService_sortExperimentRunsAsync(ascending, experiment_run_ids, ids_only, sort_key), Duration.Inf)

  def ExperimentRunService_updateExperimentRunDescriptionAsync(body: ModeldbUpdateExperimentRunDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentRunDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentRunDescription, ModeldbUpdateExperimentRunDescriptionResponse]("POST", basePath + s"/experiment-run/updateExperimentRunDescription", __query.toMap, body, ModeldbUpdateExperimentRunDescriptionResponse.fromJson)
  }

  def ExperimentRunService_updateExperimentRunDescription(body: ModeldbUpdateExperimentRunDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentRunDescriptionResponse] = Await.result(ExperimentRunService_updateExperimentRunDescriptionAsync(body), Duration.Inf)

  def ExperimentRunService_updateExperimentRunNameAsync(body: ModeldbUpdateExperimentRunName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentRunNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentRunName, ModeldbUpdateExperimentRunNameResponse]("POST", basePath + s"/experiment-run/updateExperimentRunName", __query.toMap, body, ModeldbUpdateExperimentRunNameResponse.fromJson)
  }

  def ExperimentRunService_updateExperimentRunName(body: ModeldbUpdateExperimentRunName)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentRunNameResponse] = Await.result(ExperimentRunService_updateExperimentRunNameAsync(body), Duration.Inf)

}
