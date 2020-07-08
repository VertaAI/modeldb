// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class HydratedServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def findHydratedDatasetVersionsAsync(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryDatasetVersionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasetVersions, ModeldbAdvancedQueryDatasetVersionsResponse]("POST", basePath + s"/hydratedData/findHydratedDatasetVersions", __query.toMap, body, ModeldbAdvancedQueryDatasetVersionsResponse.fromJson)
  }

  def findHydratedDatasetVersions(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryDatasetVersionsResponse] = Await.result(findHydratedDatasetVersionsAsync(body), Duration.Inf)

  def findHydratedDatasetsAsync(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasets, ModeldbAdvancedQueryDatasetsResponse]("POST", basePath + s"/hydratedData/findHydratedDatasets", __query.toMap, body, ModeldbAdvancedQueryDatasetsResponse.fromJson)
  }

  def findHydratedDatasets(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryDatasetsResponse] = Await.result(findHydratedDatasetsAsync(body), Duration.Inf)

  def findHydratedDatasetsByOrganizationAsync(body: ModeldbFindHydratedDatasetsByOrganization)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindHydratedDatasetsByOrganization, ModeldbAdvancedQueryDatasetsResponse]("POST", basePath + s"/hydratedData/findHydratedDatasetsByOrganization", __query.toMap, body, ModeldbAdvancedQueryDatasetsResponse.fromJson)
  }

  def findHydratedDatasetsByOrganization(body: ModeldbFindHydratedDatasetsByOrganization)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryDatasetsResponse] = Await.result(findHydratedDatasetsByOrganizationAsync(body), Duration.Inf)

  def findHydratedDatasetsByTeamAsync(body: ModeldbFindHydratedDatasetsByTeam)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindHydratedDatasetsByTeam, ModeldbAdvancedQueryDatasetsResponse]("POST", basePath + s"/hydratedData/findHydratedDatasetsByTeam", __query.toMap, body, ModeldbAdvancedQueryDatasetsResponse.fromJson)
  }

  def findHydratedDatasetsByTeam(body: ModeldbFindHydratedDatasetsByTeam)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryDatasetsResponse] = Await.result(findHydratedDatasetsByTeamAsync(body), Duration.Inf)

  def findHydratedExperimentRunsAsync(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperimentRuns, ModeldbAdvancedQueryExperimentRunsResponse]("POST", basePath + s"/hydratedData/findHydratedExperimentRuns", __query.toMap, body, ModeldbAdvancedQueryExperimentRunsResponse.fromJson)
  }

  def findHydratedExperimentRuns(body: ModeldbFindExperimentRuns)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryExperimentRunsResponse] = Await.result(findHydratedExperimentRunsAsync(body), Duration.Inf)

  def findHydratedExperimentsAsync(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryExperimentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperiments, ModeldbAdvancedQueryExperimentsResponse]("POST", basePath + s"/hydratedData/findHydratedExperiments", __query.toMap, body, ModeldbAdvancedQueryExperimentsResponse.fromJson)
  }

  def findHydratedExperiments(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryExperimentsResponse] = Await.result(findHydratedExperimentsAsync(body), Duration.Inf)

  def findHydratedProjectsAsync(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindProjects, ModeldbAdvancedQueryProjectsResponse]("POST", basePath + s"/hydratedData/findHydratedProjects", __query.toMap, body, ModeldbAdvancedQueryProjectsResponse.fromJson)
  }

  def findHydratedProjects(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryProjectsResponse] = Await.result(findHydratedProjectsAsync(body), Duration.Inf)

  def findHydratedProjectsByOrganizationAsync(body: ModeldbFindHydratedProjectsByOrganization)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindHydratedProjectsByOrganization, ModeldbAdvancedQueryProjectsResponse]("POST", basePath + s"/hydratedData/findHydratedProjectsByOrganization", __query.toMap, body, ModeldbAdvancedQueryProjectsResponse.fromJson)
  }

  def findHydratedProjectsByOrganization(body: ModeldbFindHydratedProjectsByOrganization)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryProjectsResponse] = Await.result(findHydratedProjectsByOrganizationAsync(body), Duration.Inf)

  def findHydratedProjectsByTeamAsync(body: ModeldbFindHydratedProjectsByTeam)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindHydratedProjectsByTeam, ModeldbAdvancedQueryProjectsResponse]("POST", basePath + s"/hydratedData/findHydratedProjectsByTeam", __query.toMap, body, ModeldbAdvancedQueryProjectsResponse.fromJson)
  }

  def findHydratedProjectsByTeam(body: ModeldbFindHydratedProjectsByTeam)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryProjectsResponse] = Await.result(findHydratedProjectsByTeamAsync(body), Duration.Inf)

  def findHydratedProjectsByUserAsync(body: ModeldbFindHydratedProjectsByUser)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindHydratedProjectsByUser, ModeldbAdvancedQueryProjectsResponse]("POST", basePath + s"/hydratedData/findHydratedProjectsByUser", __query.toMap, body, ModeldbAdvancedQueryProjectsResponse.fromJson)
  }

  def findHydratedProjectsByUser(body: ModeldbFindHydratedProjectsByUser)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryProjectsResponse] = Await.result(findHydratedProjectsByUserAsync(body), Duration.Inf)

  def findHydratedPublicDatasetsAsync(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasets, ModeldbAdvancedQueryDatasetsResponse]("POST", basePath + s"/hydratedData/findHydratedPublicDatasets", __query.toMap, body, ModeldbAdvancedQueryDatasetsResponse.fromJson)
  }

  def findHydratedPublicDatasets(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryDatasetsResponse] = Await.result(findHydratedPublicDatasetsAsync(body), Duration.Inf)

  def findHydratedPublicProjectsAsync(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindProjects, ModeldbAdvancedQueryProjectsResponse]("POST", basePath + s"/hydratedData/findHydratedPublicProjects", __query.toMap, body, ModeldbAdvancedQueryProjectsResponse.fromJson)
  }

  def findHydratedPublicProjects(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryProjectsResponse] = Await.result(findHydratedPublicProjectsAsync(body), Duration.Inf)

  def getHydratedDatasetByNameAsync(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedDatasetByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedDatasetByNameResponse]("GET", basePath + s"/hydratedData/getHydratedDatasetByName", __query.toMap, body, ModeldbGetHydratedDatasetByNameResponse.fromJson)
  }

  def getHydratedDatasetByName(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedDatasetByNameResponse] = Await.result(getHydratedDatasetByNameAsync(name, workspace_name), Duration.Inf)

  def getHydratedDatasetsByProjectIdAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedDatasetsByProjectIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedDatasetsByProjectIdResponse]("GET", basePath + s"/hydratedData/getHydratedDatasetsByProjectId", __query.toMap, body, ModeldbGetHydratedDatasetsByProjectIdResponse.fromJson)
  }

  def getHydratedDatasetsByProjectId(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedDatasetsByProjectIdResponse] = Await.result(getHydratedDatasetsByProjectIdAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def getHydratedExperimentRunByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedExperimentRunByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedExperimentRunByIdResponse]("GET", basePath + s"/hydratedData/getHydratedExperimentRunById", __query.toMap, body, ModeldbGetHydratedExperimentRunByIdResponse.fromJson)
  }

  def getHydratedExperimentRunById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedExperimentRunByIdResponse] = Await.result(getHydratedExperimentRunByIdAsync(id), Duration.Inf)

  def getHydratedExperimentRunsInProjectAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedExperimentRunsByProjectIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedExperimentRunsByProjectIdResponse]("GET", basePath + s"/hydratedData/getHydratedExperimentRunsInProject", __query.toMap, body, ModeldbGetHydratedExperimentRunsByProjectIdResponse.fromJson)
  }

  def getHydratedExperimentRunsInProject(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedExperimentRunsByProjectIdResponse] = Await.result(getHydratedExperimentRunsInProjectAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def getHydratedExperimentsByProjectIdAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedExperimentsByProjectIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedExperimentsByProjectIdResponse]("GET", basePath + s"/hydratedData/getHydratedExperimentsByProjectId", __query.toMap, body, ModeldbGetHydratedExperimentsByProjectIdResponse.fromJson)
  }

  def getHydratedExperimentsByProjectId(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedExperimentsByProjectIdResponse] = Await.result(getHydratedExperimentsByProjectIdAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def getHydratedProjectByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedProjectByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedProjectByIdResponse]("GET", basePath + s"/hydratedData/getHydratedProjectById", __query.toMap, body, ModeldbGetHydratedProjectByIdResponse.fromJson)
  }

  def getHydratedProjectById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedProjectByIdResponse] = Await.result(getHydratedProjectByIdAsync(id), Duration.Inf)

  def getHydratedProjectsAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedProjectsResponse]("GET", basePath + s"/hydratedData/getHydratedProjects", __query.toMap, body, ModeldbGetHydratedProjectsResponse.fromJson)
  }

  def getHydratedProjects(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedProjectsResponse] = Await.result(getHydratedProjectsAsync(ascending, page_limit, page_number, sort_key, workspace_name), Duration.Inf)

  def getHydratedPublicProjectsAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetHydratedProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetHydratedProjectsResponse]("GET", basePath + s"/hydratedData/getHydratedPublicProjects", __query.toMap, body, ModeldbGetHydratedProjectsResponse.fromJson)
  }

  def getHydratedPublicProjects(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetHydratedProjectsResponse] = Await.result(getHydratedPublicProjectsAsync(ascending, page_limit, page_number, sort_key, workspace_name), Duration.Inf)

  def getTopHydratedExperimentRunsAsync(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, project_id: Option[String]=None, sort_key: Option[String]=None, top_k: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (experiment_id.isDefined) __query.update("experiment_id", client.toQuery(experiment_id.get))
    if (experiment_run_ids.isDefined) __query.update("experiment_run_ids", client.toQuery(experiment_run_ids.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (top_k.isDefined) __query.update("top_k", client.toQuery(top_k.get))
    if (ids_only.isDefined) __query.update("ids_only", client.toQuery(ids_only.get))
    val body: String = null
    return client.request[String, ModeldbAdvancedQueryExperimentRunsResponse]("GET", basePath + s"/hydratedData/getTopHydratedExperimentRuns", __query.toMap, body, ModeldbAdvancedQueryExperimentRunsResponse.fromJson)
  }

  def getTopHydratedExperimentRuns(ascending: Option[Boolean]=None, experiment_id: Option[String]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, project_id: Option[String]=None, sort_key: Option[String]=None, top_k: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryExperimentRunsResponse] = Await.result(getTopHydratedExperimentRunsAsync(ascending, experiment_id, experiment_run_ids, ids_only, project_id, sort_key, top_k), Duration.Inf)

  def sortHydratedExperimentRunsAsync(ascending: Option[Boolean]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbAdvancedQueryExperimentRunsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (experiment_run_ids.isDefined) __query.update("experiment_run_ids", client.toQuery(experiment_run_ids.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (ids_only.isDefined) __query.update("ids_only", client.toQuery(ids_only.get))
    val body: String = null
    return client.request[String, ModeldbAdvancedQueryExperimentRunsResponse]("GET", basePath + s"/hydratedData/sortHydratedExperimentRuns", __query.toMap, body, ModeldbAdvancedQueryExperimentRunsResponse.fromJson)
  }

  def sortHydratedExperimentRuns(ascending: Option[Boolean]=None, experiment_run_ids: Option[List[String]]=None, ids_only: Option[Boolean]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbAdvancedQueryExperimentRunsResponse] = Await.result(sortHydratedExperimentRunsAsync(ascending, experiment_run_ids, ids_only, sort_key), Duration.Inf)

}
