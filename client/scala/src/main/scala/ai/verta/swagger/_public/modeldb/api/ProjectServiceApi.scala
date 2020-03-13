// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class ProjectServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addProjectAttributesAsync(body: ModeldbAddProjectAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectAttributes, ModeldbAddProjectAttributesResponse]("POST", basePath + s"/project/addProjectAttributes", __query, body, ModeldbAddProjectAttributesResponse.fromJson)
  }

  def addProjectAttributes(body: ModeldbAddProjectAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddProjectAttributesResponse] = Await.result(addProjectAttributesAsync(body), Duration.Inf)

  def addProjectTagAsync(body: ModeldbAddProjectTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectTagResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectTag, ModeldbAddProjectTagResponse]("POST", basePath + s"/project/addProjectTag", __query, body, ModeldbAddProjectTagResponse.fromJson)
  }

  def addProjectTag(body: ModeldbAddProjectTag)(implicit ec: ExecutionContext): Try[ModeldbAddProjectTagResponse] = Await.result(addProjectTagAsync(body), Duration.Inf)

  def addProjectTagsAsync(body: ModeldbAddProjectTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddProjectTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddProjectTags, ModeldbAddProjectTagsResponse]("POST", basePath + s"/project/addProjectTags", __query, body, ModeldbAddProjectTagsResponse.fromJson)
  }

  def addProjectTags(body: ModeldbAddProjectTags)(implicit ec: ExecutionContext): Try[ModeldbAddProjectTagsResponse] = Await.result(addProjectTagsAsync(body), Duration.Inf)

  def createProjectAsync(body: ModeldbCreateProject)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateProjectResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateProject, ModeldbCreateProjectResponse]("POST", basePath + s"/project/createProject", __query, body, ModeldbCreateProjectResponse.fromJson)
  }

  def createProject(body: ModeldbCreateProject)(implicit ec: ExecutionContext): Try[ModeldbCreateProjectResponse] = Await.result(createProjectAsync(body), Duration.Inf)

  def deepCopyProjectAsync(body: ModeldbDeepCopyProject)(implicit ec: ExecutionContext): Future[Try[ModeldbDeepCopyProjectResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeepCopyProject, ModeldbDeepCopyProjectResponse]("POST", basePath + s"/project/deepCopyProject", __query, body, ModeldbDeepCopyProjectResponse.fromJson)
  }

  def deepCopyProject(body: ModeldbDeepCopyProject)(implicit ec: ExecutionContext): Try[ModeldbDeepCopyProjectResponse] = Await.result(deepCopyProjectAsync(body), Duration.Inf)

  def deleteArtifactAsync(body: ModeldbDeleteProjectArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectArtifact, ModeldbDeleteProjectArtifactResponse]("DELETE", basePath + s"/project/deleteArtifact", __query, body, ModeldbDeleteProjectArtifactResponse.fromJson)
  }

  def deleteArtifact(body: ModeldbDeleteProjectArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectArtifactResponse] = Await.result(deleteArtifactAsync(body), Duration.Inf)

  def deleteProjectAsync(body: ModeldbDeleteProject)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProject, ModeldbDeleteProjectResponse]("DELETE", basePath + s"/project/deleteProject", __query, body, ModeldbDeleteProjectResponse.fromJson)
  }

  def deleteProject(body: ModeldbDeleteProject)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectResponse] = Await.result(deleteProjectAsync(body), Duration.Inf)

  def deleteProjectAttributesAsync(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "delete_all" -> client.toQuery(delete_all)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteProjectAttributesResponse]("DELETE", basePath + s"/project/deleteProjectAttributes", __query, body, ModeldbDeleteProjectAttributesResponse.fromJson)
  }

  def deleteProjectAttributes(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectAttributesResponse] = Await.result(deleteProjectAttributesAsync(id, attribute_keys, delete_all), Duration.Inf)

  def deleteProjectTagAsync(body: ModeldbDeleteProjectTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectTagResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectTag, ModeldbDeleteProjectTagResponse]("DELETE", basePath + s"/project/deleteProjectTag", __query, body, ModeldbDeleteProjectTagResponse.fromJson)
  }

  def deleteProjectTag(body: ModeldbDeleteProjectTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectTagResponse] = Await.result(deleteProjectTagAsync(body), Duration.Inf)

  def deleteProjectTagsAsync(body: ModeldbDeleteProjectTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjectTags, ModeldbDeleteProjectTagsResponse]("DELETE", basePath + s"/project/deleteProjectTags", __query, body, ModeldbDeleteProjectTagsResponse.fromJson)
  }

  def deleteProjectTags(body: ModeldbDeleteProjectTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectTagsResponse] = Await.result(deleteProjectTagsAsync(body), Duration.Inf)

  def deleteProjectsAsync(body: ModeldbDeleteProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteProjectsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteProjects, ModeldbDeleteProjectsResponse]("DELETE", basePath + s"/project/deleteProjects", __query, body, ModeldbDeleteProjectsResponse.fromJson)
  }

  def deleteProjects(body: ModeldbDeleteProjects)(implicit ec: ExecutionContext): Try[ModeldbDeleteProjectsResponse] = Await.result(deleteProjectsAsync(body), Duration.Inf)

  def findProjectsAsync(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Future[Try[ModeldbFindProjectsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindProjects, ModeldbFindProjectsResponse]("POST", basePath + s"/project/findProjects", __query, body, ModeldbFindProjectsResponse.fromJson)
  }

  def findProjects(body: ModeldbFindProjects)(implicit ec: ExecutionContext): Try[ModeldbFindProjectsResponse] = Await.result(findProjectsAsync(body), Duration.Inf)

  def getArtifactsAsync(id: String, key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "key" -> client.toQuery(key)
    )
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/project/getArtifacts", __query, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def getArtifacts(id: String, key: String)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(getArtifactsAsync(id, key), Duration.Inf)

  def getProjectAttributesAsync(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "get_all" -> client.toQuery(get_all)
    )
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/project/getProjectAttributes", __query, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getProjectAttributes(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getProjectAttributesAsync(id, attribute_keys, get_all), Duration.Inf)

  def getProjectByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectByIdResponse]("GET", basePath + s"/project/getProjectById", __query, body, ModeldbGetProjectByIdResponse.fromJson)
  }

  def getProjectById(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectByIdResponse] = Await.result(getProjectByIdAsync(id), Duration.Inf)

  def getProjectByNameAsync(name: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectByNameResponse]] = {
    val __query = Map[String,String](
      "name" -> client.toQuery(name),
      "workspace_name" -> client.toQuery(workspace_name)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectByNameResponse]("GET", basePath + s"/project/getProjectByName", __query, body, ModeldbGetProjectByNameResponse.fromJson)
  }

  def getProjectByName(name: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectByNameResponse] = Await.result(getProjectByNameAsync(name, workspace_name), Duration.Inf)

  def getProjectCodeVersionAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectCodeVersionResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectCodeVersionResponse]("GET", basePath + s"/project/getProjectCodeVersion", __query, body, ModeldbGetProjectCodeVersionResponse.fromJson)
  }

  def getProjectCodeVersion(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectCodeVersionResponse] = Await.result(getProjectCodeVersionAsync(id), Duration.Inf)

  def getProjectReadmeAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectReadmeResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectReadmeResponse]("GET", basePath + s"/project/getProjectReadme", __query, body, ModeldbGetProjectReadmeResponse.fromJson)
  }

  def getProjectReadme(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectReadmeResponse] = Await.result(getProjectReadmeAsync(id), Duration.Inf)

  def getProjectShortNameAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectShortNameResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectShortNameResponse]("GET", basePath + s"/project/getProjectShortName", __query, body, ModeldbGetProjectShortNameResponse.fromJson)
  }

  def getProjectShortName(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectShortNameResponse] = Await.result(getProjectShortNameAsync(id), Duration.Inf)

  def getProjectTagsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/project/getProjectTags", __query, body, ModeldbGetTagsResponse.fromJson)
  }

  def getProjectTags(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getProjectTagsAsync(id), Duration.Inf)

  def getProjectsAsync(page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetProjectsResponse]] = {
    val __query = Map[String,String](
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key),
      "workspace_name" -> client.toQuery(workspace_name)
    )
    val body: String = null
    return client.request[String, ModeldbGetProjectsResponse]("GET", basePath + s"/project/getProjects", __query, body, ModeldbGetProjectsResponse.fromJson)
  }

  def getProjects(page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetProjectsResponse] = Await.result(getProjectsAsync(page_number, page_limit, ascending, sort_key, workspace_name), Duration.Inf)

  def getPublicProjectsAsync(user_id: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetPublicProjectsResponse]] = {
    val __query = Map[String,String](
      "user_id" -> client.toQuery(user_id),
      "workspace_name" -> client.toQuery(workspace_name)
    )
    val body: String = null
    return client.request[String, ModeldbGetPublicProjectsResponse]("GET", basePath + s"/project/getPublicProjects", __query, body, ModeldbGetPublicProjectsResponse.fromJson)
  }

  def getPublicProjects(user_id: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetPublicProjectsResponse] = Await.result(getPublicProjectsAsync(user_id, workspace_name), Duration.Inf)

  def getSummaryAsync(entityId: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetSummaryResponse]] = {
    val __query = Map[String,String](
      "entityId" -> client.toQuery(entityId)
    )
    val body: String = null
    return client.request[String, ModeldbGetSummaryResponse]("GET", basePath + s"/project/getSummary", __query, body, ModeldbGetSummaryResponse.fromJson)
  }

  def getSummary(entityId: String)(implicit ec: ExecutionContext): Try[ModeldbGetSummaryResponse] = Await.result(getSummaryAsync(entityId), Duration.Inf)

  def getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/project/getUrlForArtifact", __query, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(getUrlForArtifactAsync(body), Duration.Inf)

  def logArtifactsAsync(body: ModeldbLogProjectArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogProjectArtifactsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogProjectArtifacts, ModeldbLogProjectArtifactsResponse]("POST", basePath + s"/project/logArtifacts", __query, body, ModeldbLogProjectArtifactsResponse.fromJson)
  }

  def logArtifacts(body: ModeldbLogProjectArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogProjectArtifactsResponse] = Await.result(logArtifactsAsync(body), Duration.Inf)

  def logProjectCodeVersionAsync(body: ModeldbLogProjectCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogProjectCodeVersionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogProjectCodeVersion, ModeldbLogProjectCodeVersionResponse]("POST", basePath + s"/project/logProjectCodeVersion", __query, body, ModeldbLogProjectCodeVersionResponse.fromJson)
  }

  def logProjectCodeVersion(body: ModeldbLogProjectCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogProjectCodeVersionResponse] = Await.result(logProjectCodeVersionAsync(body), Duration.Inf)

  def setProjectReadmeAsync(body: ModeldbSetProjectReadme)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectReadmeResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectReadme, ModeldbSetProjectReadmeResponse]("POST", basePath + s"/project/setProjectReadme", __query, body, ModeldbSetProjectReadmeResponse.fromJson)
  }

  def setProjectReadme(body: ModeldbSetProjectReadme)(implicit ec: ExecutionContext): Try[ModeldbSetProjectReadmeResponse] = Await.result(setProjectReadmeAsync(body), Duration.Inf)

  def setProjectShortNameAsync(body: ModeldbSetProjectShortName)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectShortNameResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectShortName, ModeldbSetProjectShortNameResponse]("POST", basePath + s"/project/setProjectShortName", __query, body, ModeldbSetProjectShortNameResponse.fromJson)
  }

  def setProjectShortName(body: ModeldbSetProjectShortName)(implicit ec: ExecutionContext): Try[ModeldbSetProjectShortNameResponse] = Await.result(setProjectShortNameAsync(body), Duration.Inf)

  def setProjectVisibilityAsync(body: ModeldbSetProjectVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectVisibiltyResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectVisibilty, ModeldbSetProjectVisibiltyResponse]("POST", basePath + s"/project/setProjectVisibility", __query, body, ModeldbSetProjectVisibiltyResponse.fromJson)
  }

  def setProjectVisibility(body: ModeldbSetProjectVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetProjectVisibiltyResponse] = Await.result(setProjectVisibilityAsync(body), Duration.Inf)

  def setProjectWorkspaceAsync(body: ModeldbSetProjectWorkspace)(implicit ec: ExecutionContext): Future[Try[ModeldbSetProjectWorkspaceResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetProjectWorkspace, ModeldbSetProjectWorkspaceResponse]("POST", basePath + s"/project/setProjectWorkspace", __query, body, ModeldbSetProjectWorkspaceResponse.fromJson)
  }

  def setProjectWorkspace(body: ModeldbSetProjectWorkspace)(implicit ec: ExecutionContext): Try[ModeldbSetProjectWorkspaceResponse] = Await.result(setProjectWorkspaceAsync(body), Duration.Inf)

  def updateProjectAttributesAsync(body: ModeldbUpdateProjectAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectAttributes, ModeldbUpdateProjectAttributesResponse]("POST", basePath + s"/project/updateProjectAttributes", __query, body, ModeldbUpdateProjectAttributesResponse.fromJson)
  }

  def updateProjectAttributes(body: ModeldbUpdateProjectAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectAttributesResponse] = Await.result(updateProjectAttributesAsync(body), Duration.Inf)

  def updateProjectDescriptionAsync(body: ModeldbUpdateProjectDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectDescriptionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectDescription, ModeldbUpdateProjectDescriptionResponse]("POST", basePath + s"/project/updateProjectDescription", __query, body, ModeldbUpdateProjectDescriptionResponse.fromJson)
  }

  def updateProjectDescription(body: ModeldbUpdateProjectDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectDescriptionResponse] = Await.result(updateProjectDescriptionAsync(body), Duration.Inf)

  def updateProjectNameAsync(body: ModeldbUpdateProjectName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateProjectNameResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateProjectName, ModeldbUpdateProjectNameResponse]("POST", basePath + s"/project/updateProjectName", __query, body, ModeldbUpdateProjectNameResponse.fromJson)
  }

  def updateProjectName(body: ModeldbUpdateProjectName)(implicit ec: ExecutionContext): Try[ModeldbUpdateProjectNameResponse] = Await.result(updateProjectNameAsync(body), Duration.Inf)

  def verifyConnectionAsync()(implicit ec: ExecutionContext): Future[Try[ModeldbVerifyConnectionResponse]] = {
    val __query = Map[String,String](
    )
    val body: String = null
    return client.request[String, ModeldbVerifyConnectionResponse]("GET", basePath + s"/project/verifyConnection", __query, body, ModeldbVerifyConnectionResponse.fromJson)
  }

  def verifyConnection()(implicit ec: ExecutionContext): Try[ModeldbVerifyConnectionResponse] = Await.result(verifyConnectionAsync(), Duration.Inf)

}
