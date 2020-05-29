// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class ExperimentServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addAttributeAsync(body: ModeldbAddAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddAttributes, ModeldbAddAttributesResponse]("POST", basePath + s"/experiment/addAttribute", __query.toMap, body, ModeldbAddAttributesResponse.fromJson)
  }

  def addAttribute(body: ModeldbAddAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddAttributesResponse] = Await.result(addAttributeAsync(body), Duration.Inf)

  def addExperimentAttributesAsync(body: ModeldbAddExperimentAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentAttributes, ModeldbAddExperimentAttributesResponse]("POST", basePath + s"/experiment/addExperimentAttributes", __query.toMap, body, ModeldbAddExperimentAttributesResponse.fromJson)
  }

  def addExperimentAttributes(body: ModeldbAddExperimentAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentAttributesResponse] = Await.result(addExperimentAttributesAsync(body), Duration.Inf)

  def addExperimentTagAsync(body: ModeldbAddExperimentTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentTag, ModeldbAddExperimentTagResponse]("POST", basePath + s"/experiment/addExperimentTag", __query.toMap, body, ModeldbAddExperimentTagResponse.fromJson)
  }

  def addExperimentTag(body: ModeldbAddExperimentTag)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentTagResponse] = Await.result(addExperimentTagAsync(body), Duration.Inf)

  def addExperimentTagsAsync(body: ModeldbAddExperimentTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentTags, ModeldbAddExperimentTagsResponse]("POST", basePath + s"/experiment/addExperimentTags", __query.toMap, body, ModeldbAddExperimentTagsResponse.fromJson)
  }

  def addExperimentTags(body: ModeldbAddExperimentTags)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentTagsResponse] = Await.result(addExperimentTagsAsync(body), Duration.Inf)

  def createExperimentAsync(body: ModeldbCreateExperiment)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateExperimentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateExperiment, ModeldbCreateExperimentResponse]("POST", basePath + s"/experiment/createExperiment", __query.toMap, body, ModeldbCreateExperimentResponse.fromJson)
  }

  def createExperiment(body: ModeldbCreateExperiment)(implicit ec: ExecutionContext): Try[ModeldbCreateExperimentResponse] = Await.result(createExperimentAsync(body), Duration.Inf)

  def deleteArtifactAsync(body: ModeldbDeleteExperimentArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentArtifact, ModeldbDeleteExperimentArtifactResponse]("DELETE", basePath + s"/experiment/deleteArtifact", __query.toMap, body, ModeldbDeleteExperimentArtifactResponse.fromJson)
  }

  def deleteArtifact(body: ModeldbDeleteExperimentArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentArtifactResponse] = Await.result(deleteArtifactAsync(body), Duration.Inf)

  def deleteExperimentAsync(body: ModeldbDeleteExperiment)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperiment, ModeldbDeleteExperimentResponse]("DELETE", basePath + s"/experiment/deleteExperiment", __query.toMap, body, ModeldbDeleteExperimentResponse.fromJson)
  }

  def deleteExperiment(body: ModeldbDeleteExperiment)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentResponse] = Await.result(deleteExperimentAsync(body), Duration.Inf)

  def deleteExperimentAttributesAsync(attribute_keys: Option[List[String]]=None, delete_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (delete_all.isDefined) __query.update("delete_all", client.toQuery(delete_all.get))
    val body: String = null
    return client.request[String, ModeldbDeleteExperimentAttributesResponse]("DELETE", basePath + s"/experiment/deleteExperimentAttributes", __query.toMap, body, ModeldbDeleteExperimentAttributesResponse.fromJson)
  }

  def deleteExperimentAttributes(attribute_keys: Option[List[String]]=None, delete_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentAttributesResponse] = Await.result(deleteExperimentAttributesAsync(attribute_keys, delete_all, id), Duration.Inf)

  def deleteExperimentTagAsync(body: ModeldbDeleteExperimentTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentTag, ModeldbDeleteExperimentTagResponse]("DELETE", basePath + s"/experiment/deleteExperimentTag", __query.toMap, body, ModeldbDeleteExperimentTagResponse.fromJson)
  }

  def deleteExperimentTag(body: ModeldbDeleteExperimentTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentTagResponse] = Await.result(deleteExperimentTagAsync(body), Duration.Inf)

  def deleteExperimentTagsAsync(body: ModeldbDeleteExperimentTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentTags, ModeldbDeleteExperimentTagsResponse]("DELETE", basePath + s"/experiment/deleteExperimentTags", __query.toMap, body, ModeldbDeleteExperimentTagsResponse.fromJson)
  }

  def deleteExperimentTags(body: ModeldbDeleteExperimentTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentTagsResponse] = Await.result(deleteExperimentTagsAsync(body), Duration.Inf)

  def deleteExperimentsAsync(body: ModeldbDeleteExperiments)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperiments, ModeldbDeleteExperimentsResponse]("DELETE", basePath + s"/experiment/deleteExperiments", __query.toMap, body, ModeldbDeleteExperimentsResponse.fromJson)
  }

  def deleteExperiments(body: ModeldbDeleteExperiments)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentsResponse] = Await.result(deleteExperimentsAsync(body), Duration.Inf)

  def findExperimentsAsync(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Future[Try[ModeldbFindExperimentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperiments, ModeldbFindExperimentsResponse]("POST", basePath + s"/experiment/findExperiments", __query.toMap, body, ModeldbFindExperimentsResponse.fromJson)
  }

  def findExperiments(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Try[ModeldbFindExperimentsResponse] = Await.result(findExperimentsAsync(body), Duration.Inf)

  def getArtifactsAsync(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/experiment/getArtifacts", __query.toMap, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def getArtifacts(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(getArtifactsAsync(id, key), Duration.Inf)

  def getExperimentAttributesAsync(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/experiment/getExperimentAttributes", __query.toMap, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getExperimentAttributes(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getExperimentAttributesAsync(attribute_keys, get_all, id), Duration.Inf)

  def getExperimentByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentByIdResponse]("GET", basePath + s"/experiment/getExperimentById", __query.toMap, body, ModeldbGetExperimentByIdResponse.fromJson)
  }

  def getExperimentById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentByIdResponse] = Await.result(getExperimentByIdAsync(id), Duration.Inf)

  def getExperimentByNameAsync(name: Option[String]=None, project_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentByNameResponse]("GET", basePath + s"/experiment/getExperimentByName", __query.toMap, body, ModeldbGetExperimentByNameResponse.fromJson)
  }

  def getExperimentByName(name: Option[String]=None, project_id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentByNameResponse] = Await.result(getExperimentByNameAsync(name, project_id), Duration.Inf)

  def getExperimentCodeVersionAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentCodeVersionResponse]("GET", basePath + s"/experiment/getExperimentCodeVersion", __query.toMap, body, ModeldbGetExperimentCodeVersionResponse.fromJson)
  }

  def getExperimentCodeVersion(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentCodeVersionResponse] = Await.result(getExperimentCodeVersionAsync(id), Duration.Inf)

  def getExperimentTagsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/experiment/getExperimentTags", __query.toMap, body, ModeldbGetTagsResponse.fromJson)
  }

  def getExperimentTags(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getExperimentTagsAsync(id), Duration.Inf)

  def getExperimentsInProjectAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentsInProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentsInProjectResponse]("GET", basePath + s"/experiment/getExperimentsInProject", __query.toMap, body, ModeldbGetExperimentsInProjectResponse.fromJson)
  }

  def getExperimentsInProject(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentsInProjectResponse] = Await.result(getExperimentsInProjectAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/experiment/getUrlForArtifact", __query.toMap, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(getUrlForArtifactAsync(body), Duration.Inf)

  def logArtifactsAsync(body: ModeldbLogExperimentArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentArtifacts, ModeldbLogExperimentArtifactsResponse]("POST", basePath + s"/experiment/logArtifacts", __query.toMap, body, ModeldbLogExperimentArtifactsResponse.fromJson)
  }

  def logArtifacts(body: ModeldbLogExperimentArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentArtifactsResponse] = Await.result(logArtifactsAsync(body), Duration.Inf)

  def logExperimentCodeVersionAsync(body: ModeldbLogExperimentCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentCodeVersion, ModeldbLogExperimentCodeVersionResponse]("POST", basePath + s"/experiment/logExperimentCodeVersion", __query.toMap, body, ModeldbLogExperimentCodeVersionResponse.fromJson)
  }

  def logExperimentCodeVersion(body: ModeldbLogExperimentCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentCodeVersionResponse] = Await.result(logExperimentCodeVersionAsync(body), Duration.Inf)

  def updateExperimentDescriptionAsync(body: ModeldbUpdateExperimentDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentDescription, ModeldbUpdateExperimentDescriptionResponse]("POST", basePath + s"/experiment/updateExperimentDescription", __query.toMap, body, ModeldbUpdateExperimentDescriptionResponse.fromJson)
  }

  def updateExperimentDescription(body: ModeldbUpdateExperimentDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentDescriptionResponse] = Await.result(updateExperimentDescriptionAsync(body), Duration.Inf)

  def updateExperimentNameAsync(body: ModeldbUpdateExperimentName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentName, ModeldbUpdateExperimentNameResponse]("POST", basePath + s"/experiment/updateExperimentName", __query.toMap, body, ModeldbUpdateExperimentNameResponse.fromJson)
  }

  def updateExperimentName(body: ModeldbUpdateExperimentName)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentNameResponse] = Await.result(updateExperimentNameAsync(body), Duration.Inf)

  def updateExperimentNameOrDescriptionAsync(body: ModeldbUpdateExperimentNameOrDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentNameOrDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentNameOrDescription, ModeldbUpdateExperimentNameOrDescriptionResponse]("POST", basePath + s"/experiment/updateExperimentNameOrDescription", __query.toMap, body, ModeldbUpdateExperimentNameOrDescriptionResponse.fromJson)
  }

  def updateExperimentNameOrDescription(body: ModeldbUpdateExperimentNameOrDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentNameOrDescriptionResponse] = Await.result(updateExperimentNameOrDescriptionAsync(body), Duration.Inf)

}
