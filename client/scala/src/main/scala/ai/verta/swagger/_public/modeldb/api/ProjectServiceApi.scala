// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class ProjectServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addProjectAttributesAsync(body: ModeldbAddProjectAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectAttributes, ModeldbAddProjectAttributesResponse]("POST", basePath + s"/project/addProjectAttributes", __query.toMap, body, ModeldbAddProjectAttributesResponse.fromJson)
  }

  def addProjectAttributes(body: ModeldbAddProjectAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddProjectAttributesResponse] = Await.result(addProjectAttributesAsync(body), Duration.Inf)

  def addProjectTagAsync(body: ModeldbAddProjectTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectTag, ModeldbAddProjectTagResponse]("POST", basePath + s"/project/addProjectTag", __query.toMap, body, ModeldbAddProjectTagResponse.fromJson)
  }

  def addProjectTag(body: ModeldbAddProjectTag)(implicit ec: ExecutionContext): Try[ModeldbAddProjectTagResponse] = Await.result(addProjectTagAsync(body), Duration.Inf)

  def addProjectTagsAsync(body: ModeldbAddProjectTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectTags, ModeldbAddProjectTagsResponse]("POST", basePath + s"/project/addProjectTags", __query.toMap, body, ModeldbAddProjectTagsResponse.fromJson)
  }

  def addProjectTags(body: ModeldbAddProjectTags)(implicit ec: ExecutionContext): Try[ModeldbAddProjectTagsResponse] = Await.result(addProjectTagsAsync(body), Duration.Inf)

  def createProjectAsync(body: ModeldbCreateProject)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateProject, ModeldbCreateProjectResponse]("POST", basePath + s"/project/createProject", __query.toMap, body, ModeldbCreateProjectResponse.fromJson)
  }

  def createProject(body: ModeldbCreateProject)(implicit ec: ExecutionContext): Try[ModeldbCreateProjectResponse] = Await.result(createProjectAsync(body), Duration.Inf)

  def deepCopyProjectAsync(body: ModeldbDeepCopyProject)(implicit ec: ExecutionContext): Future[Try[ModeldbDeepCopyProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeepCopyProject, ModeldbDeepCopyProjectResponse]("POST", basePath + s"/project/deepCopyProject", __query.toMap, body, ModeldbDeepCopyProjectResponse.fromJson)
  }

  def deepCopyProject(body: ModeldbDeepCopyProject)(implicit ec: ExecutionContext): Try[ModeldbDeepCopyProjectResponse] = Await.result(deepCopyProjectAsync(body), Duration.Inf)

  def deleteArtifactAsync(body: ModeldbDeleteProjectArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectArtifact, ModeldbDeleteProjectArtifactResponse]("DELETE", basePath + s"/project/deleteArtifact", __query.toMap, body, ModeldbDeleteProjectArtifactResponse.fromJson)
  }

  def deleteArtifact(body: ModeldbDeleteProjectArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectArtifactResponse] = Await.result(deleteArtifactAsync(body), Duration.Inf)

  def deleteProjectAsync(body: ModeldbDeleteProject)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProject, ModeldbDeleteProjectResponse]("DELETE", basePath + s"/project/deleteProject", __query.toMap, body, ModeldbDeleteProjectResponse.fromJson)
  }

  def deleteProject(body: ModeldbDeleteProject)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectResponse] = Await.result(deleteProjectAsync(body), Duration.Inf)

  def deleteProjectAttributesAsync(body: ModeldbDeleteProjectAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectAttributes, ModeldbDeleteProjectAttributesResponse]("DELETE", basePath + s"/project/deleteProjectAttributes", __query.toMap, body, ModeldbDeleteProjectAttributesResponse.fromJson)
  }

  def deleteProjectAttributes(body: ModeldbDeleteProjectAttributes)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectAttributesResponse] = Await.result(deleteProjectAttributesAsync(body), Duration.Inf)

  def deleteProjectTagAsync(body: ModeldbDeleteProjectTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectTag, ModeldbDeleteProjectTagResponse]("DELETE", basePath + s"/project/deleteProjectTag", __query.toMap, body, ModeldbDeleteProjectTagResponse.fromJson)
  }

  def deleteProjectTag(body: ModeldbDeleteProjectTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectTagResponse] = Await.result(deleteProjectTagAsync(body), Duration.Inf)

  def deleteProjectTagsAsync(body: ModeldbDeleteProjectTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectTags, ModeldbDeleteProjectTagsResponse]("DELETE", basePath + s"/project/deleteProjectTags", __query.toMap, body, ModeldbDeleteProjectTagsResponse.fromJson)
  }

  def deleteProjectTags(body: ModeldbDeleteProjectTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectTagsResponse] = Await.result(deleteProjectTagsAsync(body), Duration.Inf)

  def deleteProjectsAsync(body: ModeldbDeleteProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjects, ModeldbDeleteProjectsResponse]("DELETE", basePath + s"/project/deleteProjects", __query.toMap, body, ModeldbDeleteProjectsResponse.fromJson)
  }

  def deleteProjects(body: ModeldbDeleteProjects)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectsResponse] = Await.result(deleteProjectsAsync(body), Duration.Inf)

  def findProjectsAsync(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbFindProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindProjects, ModeldbFindProjectsResponse]("POST", basePath + s"/project/findProjects", __query.toMap, body, ModeldbFindProjectsResponse.fromJson)
  }

  def findProjects(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Try[ModeldbFindProjectsResponse] = Await.result(findProjectsAsync(body), Duration.Inf)

  def getArtifactsAsync(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/project/getArtifacts", __query.toMap, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def getArtifacts(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(getArtifactsAsync(id, key), Duration.Inf)

  def getProjectAttributesAsync(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/project/getProjectAttributes", __query.toMap, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getProjectAttributes(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getProjectAttributesAsync(attribute_keys, get_all, id), Duration.Inf)

  def getProjectByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectByIdResponse]("GET", basePath + s"/project/getProjectById", __query.toMap, body, ModeldbGetProjectByIdResponse.fromJson)
  }

  def getProjectById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectByIdResponse] = Await.result(getProjectByIdAsync(id), Duration.Inf)

  def getProjectByNameAsync(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectByNameResponse]("GET", basePath + s"/project/getProjectByName", __query.toMap, body, ModeldbGetProjectByNameResponse.fromJson)
  }

  def getProjectByName(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectByNameResponse] = Await.result(getProjectByNameAsync(name, workspace_name), Duration.Inf)

  def getProjectCodeVersionAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectCodeVersionResponse]("GET", basePath + s"/project/getProjectCodeVersion", __query.toMap, body, ModeldbGetProjectCodeVersionResponse.fromJson)
  }

  def getProjectCodeVersion(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectCodeVersionResponse] = Await.result(getProjectCodeVersionAsync(id), Duration.Inf)

  def getProjectReadmeAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectReadmeResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectReadmeResponse]("GET", basePath + s"/project/getProjectReadme", __query.toMap, body, ModeldbGetProjectReadmeResponse.fromJson)
  }

  def getProjectReadme(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectReadmeResponse] = Await.result(getProjectReadmeAsync(id), Duration.Inf)

  def getProjectShortNameAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectShortNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectShortNameResponse]("GET", basePath + s"/project/getProjectShortName", __query.toMap, body, ModeldbGetProjectShortNameResponse.fromJson)
  }

  def getProjectShortName(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectShortNameResponse] = Await.result(getProjectShortNameAsync(id), Duration.Inf)

  def getProjectTagsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/project/getProjectTags", __query.toMap, body, ModeldbGetTagsResponse.fromJson)
  }

  def getProjectTags(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getProjectTagsAsync(id), Duration.Inf)

  def getProjectsAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetProjectsResponse]("GET", basePath + s"/project/getProjects", __query.toMap, body, ModeldbGetProjectsResponse.fromJson)
  }

  def getProjects(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetProjectsResponse] = Await.result(getProjectsAsync(ascending, page_limit, page_number, sort_key, workspace_name), Duration.Inf)

  def getPublicProjectsAsync(user_id: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetPublicProjectsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (user_id.isDefined) __query.update("user_id", client.toQuery(user_id.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetPublicProjectsResponse]("GET", basePath + s"/project/getPublicProjects", __query.toMap, body, ModeldbGetPublicProjectsResponse.fromJson)
  }

  def getPublicProjects(user_id: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetPublicProjectsResponse] = Await.result(getPublicProjectsAsync(user_id, workspace_name), Duration.Inf)

  def getSummaryAsync(entityId: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetSummaryResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entityId.isDefined) __query.update("entityId", client.toQuery(entityId.get))
    val body: String = null
    return client.request[String, ModeldbGetSummaryResponse]("GET", basePath + s"/project/getSummary", __query.toMap, body, ModeldbGetSummaryResponse.fromJson)
  }

  def getSummary(entityId: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetSummaryResponse] = Await.result(getSummaryAsync(entityId), Duration.Inf)

  def getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/project/getUrlForArtifact", __query.toMap, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(getUrlForArtifactAsync(body), Duration.Inf)

  def logArtifactsAsync(body: ModeldbLogProjectArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogProjectArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogProjectArtifacts, ModeldbLogProjectArtifactsResponse]("POST", basePath + s"/project/logArtifacts", __query.toMap, body, ModeldbLogProjectArtifactsResponse.fromJson)
  }

  def logArtifacts(body: ModeldbLogProjectArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogProjectArtifactsResponse] = Await.result(logArtifactsAsync(body), Duration.Inf)

  def logProjectCodeVersionAsync(body: ModeldbLogProjectCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogProjectCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogProjectCodeVersion, ModeldbLogProjectCodeVersionResponse]("POST", basePath + s"/project/logProjectCodeVersion", __query.toMap, body, ModeldbLogProjectCodeVersionResponse.fromJson)
  }

  def logProjectCodeVersion(body: ModeldbLogProjectCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogProjectCodeVersionResponse] = Await.result(logProjectCodeVersionAsync(body), Duration.Inf)

  def setProjectReadmeAsync(body: ModeldbSetProjectReadme)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectReadmeResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectReadme, ModeldbSetProjectReadmeResponse]("POST", basePath + s"/project/setProjectReadme", __query.toMap, body, ModeldbSetProjectReadmeResponse.fromJson)
  }

  def setProjectReadme(body: ModeldbSetProjectReadme)(implicit ec: ExecutionContext): Try[ModeldbSetProjectReadmeResponse] = Await.result(setProjectReadmeAsync(body), Duration.Inf)

  def setProjectShortNameAsync(body: ModeldbSetProjectShortName)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectShortNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectShortName, ModeldbSetProjectShortNameResponse]("POST", basePath + s"/project/setProjectShortName", __query.toMap, body, ModeldbSetProjectShortNameResponse.fromJson)
  }

  def setProjectShortName(body: ModeldbSetProjectShortName)(implicit ec: ExecutionContext): Try[ModeldbSetProjectShortNameResponse] = Await.result(setProjectShortNameAsync(body), Duration.Inf)

  def setProjectVisibilityAsync(body: ModeldbSetProjectVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectVisibiltyResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectVisibilty, ModeldbSetProjectVisibiltyResponse]("POST", basePath + s"/project/setProjectVisibility", __query.toMap, body, ModeldbSetProjectVisibiltyResponse.fromJson)
  }

  def setProjectVisibility(body: ModeldbSetProjectVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetProjectVisibiltyResponse] = Await.result(setProjectVisibilityAsync(body), Duration.Inf)

  def setProjectWorkspaceAsync(body: ModeldbSetProjectWorkspace)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectWorkspaceResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectWorkspace, ModeldbSetProjectWorkspaceResponse]("POST", basePath + s"/project/setProjectWorkspace", __query.toMap, body, ModeldbSetProjectWorkspaceResponse.fromJson)
  }

  def setProjectWorkspace(body: ModeldbSetProjectWorkspace)(implicit ec: ExecutionContext): Try[ModeldbSetProjectWorkspaceResponse] = Await.result(setProjectWorkspaceAsync(body), Duration.Inf)

  def updateProjectAttributesAsync(body: ModeldbUpdateProjectAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectAttributes, ModeldbUpdateProjectAttributesResponse]("POST", basePath + s"/project/updateProjectAttributes", __query.toMap, body, ModeldbUpdateProjectAttributesResponse.fromJson)
  }

  def updateProjectAttributes(body: ModeldbUpdateProjectAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectAttributesResponse] = Await.result(updateProjectAttributesAsync(body), Duration.Inf)

  def updateProjectDescriptionAsync(body: ModeldbUpdateProjectDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectDescription, ModeldbUpdateProjectDescriptionResponse]("POST", basePath + s"/project/updateProjectDescription", __query.toMap, body, ModeldbUpdateProjectDescriptionResponse.fromJson)
  }

  def updateProjectDescription(body: ModeldbUpdateProjectDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectDescriptionResponse] = Await.result(updateProjectDescriptionAsync(body), Duration.Inf)

  def updateProjectNameAsync(body: ModeldbUpdateProjectName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectName, ModeldbUpdateProjectNameResponse]("POST", basePath + s"/project/updateProjectName", __query.toMap, body, ModeldbUpdateProjectNameResponse.fromJson)
  }

  def updateProjectName(body: ModeldbUpdateProjectName)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectNameResponse] = Await.result(updateProjectNameAsync(body), Duration.Inf)

  def verifyConnectionAsync()(implicit ec: ExecutionContext): Future[Try[ModeldbVerifyConnectionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    val body: String = null
    return client.request[String, ModeldbVerifyConnectionResponse]("GET", basePath + s"/project/verifyConnection", __query.toMap, body, ModeldbVerifyConnectionResponse.fromJson)
  }

  def verifyConnection()(implicit ec: ExecutionContext): Try[ModeldbVerifyConnectionResponse] = Await.result(verifyConnectionAsync(), Duration.Inf)

}
