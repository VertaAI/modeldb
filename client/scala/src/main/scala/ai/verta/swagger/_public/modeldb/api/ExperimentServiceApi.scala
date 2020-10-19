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
  def ExperimentService_addAttributeAsync(body: ModeldbAddAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddAttributes, ModeldbAddAttributesResponse]("POST", basePath + s"/experiment/addAttribute", __query.toMap, body, ModeldbAddAttributesResponse.fromJson)
  }

  def ExperimentService_addAttribute(body: ModeldbAddAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddAttributesResponse] = Await.result(ExperimentService_addAttributeAsync(body), Duration.Inf)

  def ExperimentService_addExperimentAttributesAsync(body: ModeldbAddExperimentAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentAttributes, ModeldbAddExperimentAttributesResponse]("POST", basePath + s"/experiment/addExperimentAttributes", __query.toMap, body, ModeldbAddExperimentAttributesResponse.fromJson)
  }

  def ExperimentService_addExperimentAttributes(body: ModeldbAddExperimentAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentAttributesResponse] = Await.result(ExperimentService_addExperimentAttributesAsync(body), Duration.Inf)

  def ExperimentService_addExperimentTagAsync(body: ModeldbAddExperimentTag)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentTag, ModeldbAddExperimentTagResponse]("POST", basePath + s"/experiment/addExperimentTag", __query.toMap, body, ModeldbAddExperimentTagResponse.fromJson)
  }

  def ExperimentService_addExperimentTag(body: ModeldbAddExperimentTag)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentTagResponse] = Await.result(ExperimentService_addExperimentTagAsync(body), Duration.Inf)

  def ExperimentService_addExperimentTagsAsync(body: ModeldbAddExperimentTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddExperimentTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddExperimentTags, ModeldbAddExperimentTagsResponse]("POST", basePath + s"/experiment/addExperimentTags", __query.toMap, body, ModeldbAddExperimentTagsResponse.fromJson)
  }

  def ExperimentService_addExperimentTags(body: ModeldbAddExperimentTags)(implicit ec: ExecutionContext): Try[ModeldbAddExperimentTagsResponse] = Await.result(ExperimentService_addExperimentTagsAsync(body), Duration.Inf)

  def ExperimentService_createExperimentAsync(body: ModeldbCreateExperiment)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateExperimentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateExperiment, ModeldbCreateExperimentResponse]("POST", basePath + s"/experiment/createExperiment", __query.toMap, body, ModeldbCreateExperimentResponse.fromJson)
  }

  def ExperimentService_createExperiment(body: ModeldbCreateExperiment)(implicit ec: ExecutionContext): Try[ModeldbCreateExperimentResponse] = Await.result(ExperimentService_createExperimentAsync(body), Duration.Inf)

  def ExperimentService_deleteArtifactAsync(body: ModeldbDeleteExperimentArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentArtifact, ModeldbDeleteExperimentArtifactResponse]("DELETE", basePath + s"/experiment/deleteArtifact", __query.toMap, body, ModeldbDeleteExperimentArtifactResponse.fromJson)
  }

  def ExperimentService_deleteArtifact(body: ModeldbDeleteExperimentArtifact)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentArtifactResponse] = Await.result(ExperimentService_deleteArtifactAsync(body), Duration.Inf)

  def ExperimentService_deleteExperimentAsync(body: ModeldbDeleteExperiment)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperiment, ModeldbDeleteExperimentResponse]("DELETE", basePath + s"/experiment/deleteExperiment", __query.toMap, body, ModeldbDeleteExperimentResponse.fromJson)
  }

  def ExperimentService_deleteExperiment(body: ModeldbDeleteExperiment)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentResponse] = Await.result(ExperimentService_deleteExperimentAsync(body), Duration.Inf)

  def ExperimentService_deleteExperimentAttributesAsync(attribute_keys: Option[List[String]]=None, delete_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (delete_all.isDefined) __query.update("delete_all", client.toQuery(delete_all.get))
    val body: String = null
    return client.request[String, ModeldbDeleteExperimentAttributesResponse]("DELETE", basePath + s"/experiment/deleteExperimentAttributes", __query.toMap, body, ModeldbDeleteExperimentAttributesResponse.fromJson)
  }

  def ExperimentService_deleteExperimentAttributes(attribute_keys: Option[List[String]]=None, delete_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentAttributesResponse] = Await.result(ExperimentService_deleteExperimentAttributesAsync(attribute_keys, delete_all, id), Duration.Inf)

  def ExperimentService_deleteExperimentTagAsync(body: ModeldbDeleteExperimentTag)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentTagResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentTag, ModeldbDeleteExperimentTagResponse]("DELETE", basePath + s"/experiment/deleteExperimentTag", __query.toMap, body, ModeldbDeleteExperimentTagResponse.fromJson)
  }

  def ExperimentService_deleteExperimentTag(body: ModeldbDeleteExperimentTag)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentTagResponse] = Await.result(ExperimentService_deleteExperimentTagAsync(body), Duration.Inf)

  def ExperimentService_deleteExperimentTagsAsync(body: ModeldbDeleteExperimentTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperimentTags, ModeldbDeleteExperimentTagsResponse]("DELETE", basePath + s"/experiment/deleteExperimentTags", __query.toMap, body, ModeldbDeleteExperimentTagsResponse.fromJson)
  }

  def ExperimentService_deleteExperimentTags(body: ModeldbDeleteExperimentTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentTagsResponse] = Await.result(ExperimentService_deleteExperimentTagsAsync(body), Duration.Inf)

  def ExperimentService_deleteExperimentsAsync(body: ModeldbDeleteExperiments)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteExperimentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteExperiments, ModeldbDeleteExperimentsResponse]("DELETE", basePath + s"/experiment/deleteExperiments", __query.toMap, body, ModeldbDeleteExperimentsResponse.fromJson)
  }

  def ExperimentService_deleteExperiments(body: ModeldbDeleteExperiments)(implicit ec: ExecutionContext): Try[ModeldbDeleteExperimentsResponse] = Await.result(ExperimentService_deleteExperimentsAsync(body), Duration.Inf)

  def ExperimentService_findExperimentsAsync(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Future[Try[ModeldbFindExperimentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindExperiments, ModeldbFindExperimentsResponse]("POST", basePath + s"/experiment/findExperiments", __query.toMap, body, ModeldbFindExperimentsResponse.fromJson)
  }

  def ExperimentService_findExperiments(body: ModeldbFindExperiments)(implicit ec: ExecutionContext): Try[ModeldbFindExperimentsResponse] = Await.result(ExperimentService_findExperimentsAsync(body), Duration.Inf)

  def ExperimentService_getArtifactsAsync(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ModeldbGetArtifactsResponse]("GET", basePath + s"/experiment/getArtifacts", __query.toMap, body, ModeldbGetArtifactsResponse.fromJson)
  }

  def ExperimentService_getArtifacts(id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetArtifactsResponse] = Await.result(ExperimentService_getArtifactsAsync(id, key), Duration.Inf)

  def ExperimentService_getExperimentAttributesAsync(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/experiment/getExperimentAttributes", __query.toMap, body, ModeldbGetAttributesResponse.fromJson)
  }

  def ExperimentService_getExperimentAttributes(attribute_keys: Option[List[String]]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(ExperimentService_getExperimentAttributesAsync(attribute_keys, get_all, id), Duration.Inf)

  def ExperimentService_getExperimentByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentByIdResponse]("GET", basePath + s"/experiment/getExperimentById", __query.toMap, body, ModeldbGetExperimentByIdResponse.fromJson)
  }

  def ExperimentService_getExperimentById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentByIdResponse] = Await.result(ExperimentService_getExperimentByIdAsync(id), Duration.Inf)

  def ExperimentService_getExperimentByNameAsync(name: Option[String]=None, project_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentByNameResponse]("GET", basePath + s"/experiment/getExperimentByName", __query.toMap, body, ModeldbGetExperimentByNameResponse.fromJson)
  }

  def ExperimentService_getExperimentByName(name: Option[String]=None, project_id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentByNameResponse] = Await.result(ExperimentService_getExperimentByNameAsync(name, project_id), Duration.Inf)

  def ExperimentService_getExperimentCodeVersionAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentCodeVersionResponse]("GET", basePath + s"/experiment/getExperimentCodeVersion", __query.toMap, body, ModeldbGetExperimentCodeVersionResponse.fromJson)
  }

  def ExperimentService_getExperimentCodeVersion(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentCodeVersionResponse] = Await.result(ExperimentService_getExperimentCodeVersionAsync(id), Duration.Inf)

  def ExperimentService_getExperimentTagsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/experiment/getExperimentTags", __query.toMap, body, ModeldbGetTagsResponse.fromJson)
  }

  def ExperimentService_getExperimentTags(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(ExperimentService_getExperimentTagsAsync(id), Duration.Inf)

  def ExperimentService_getExperimentsInProjectAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentsInProjectResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (project_id.isDefined) __query.update("project_id", client.toQuery(project_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetExperimentsInProjectResponse]("GET", basePath + s"/experiment/getExperimentsInProject", __query.toMap, body, ModeldbGetExperimentsInProjectResponse.fromJson)
  }

  def ExperimentService_getExperimentsInProject(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, project_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentsInProjectResponse] = Await.result(ExperimentService_getExperimentsInProjectAsync(ascending, page_limit, page_number, project_id, sort_key), Duration.Inf)

  def ExperimentService_getUrlForArtifactAsync(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForArtifact, ModeldbGetUrlForArtifactResponse]("POST", basePath + s"/experiment/getUrlForArtifact", __query.toMap, body, ModeldbGetUrlForArtifactResponse.fromJson)
  }

  def ExperimentService_getUrlForArtifact(body: ModeldbGetUrlForArtifact)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForArtifactResponse] = Await.result(ExperimentService_getUrlForArtifactAsync(body), Duration.Inf)

  def ExperimentService_logArtifactsAsync(body: ModeldbLogExperimentArtifacts)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentArtifactsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentArtifacts, ModeldbLogExperimentArtifactsResponse]("POST", basePath + s"/experiment/logArtifacts", __query.toMap, body, ModeldbLogExperimentArtifactsResponse.fromJson)
  }

  def ExperimentService_logArtifacts(body: ModeldbLogExperimentArtifacts)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentArtifactsResponse] = Await.result(ExperimentService_logArtifactsAsync(body), Duration.Inf)

  def ExperimentService_logExperimentCodeVersionAsync(body: ModeldbLogExperimentCodeVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbLogExperimentCodeVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbLogExperimentCodeVersion, ModeldbLogExperimentCodeVersionResponse]("POST", basePath + s"/experiment/logExperimentCodeVersion", __query.toMap, body, ModeldbLogExperimentCodeVersionResponse.fromJson)
  }

  def ExperimentService_logExperimentCodeVersion(body: ModeldbLogExperimentCodeVersion)(implicit ec: ExecutionContext): Try[ModeldbLogExperimentCodeVersionResponse] = Await.result(ExperimentService_logExperimentCodeVersionAsync(body), Duration.Inf)

  def ExperimentService_updateExperimentDescriptionAsync(body: ModeldbUpdateExperimentDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentDescription, ModeldbUpdateExperimentDescriptionResponse]("POST", basePath + s"/experiment/updateExperimentDescription", __query.toMap, body, ModeldbUpdateExperimentDescriptionResponse.fromJson)
  }

  def ExperimentService_updateExperimentDescription(body: ModeldbUpdateExperimentDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentDescriptionResponse] = Await.result(ExperimentService_updateExperimentDescriptionAsync(body), Duration.Inf)

  def ExperimentService_updateExperimentNameAsync(body: ModeldbUpdateExperimentName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentName, ModeldbUpdateExperimentNameResponse]("POST", basePath + s"/experiment/updateExperimentName", __query.toMap, body, ModeldbUpdateExperimentNameResponse.fromJson)
  }

  def ExperimentService_updateExperimentName(body: ModeldbUpdateExperimentName)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentNameResponse] = Await.result(ExperimentService_updateExperimentNameAsync(body), Duration.Inf)

  def ExperimentService_updateExperimentNameOrDescriptionAsync(body: ModeldbUpdateExperimentNameOrDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateExperimentNameOrDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateExperimentNameOrDescription, ModeldbUpdateExperimentNameOrDescriptionResponse]("POST", basePath + s"/experiment/updateExperimentNameOrDescription", __query.toMap, body, ModeldbUpdateExperimentNameOrDescriptionResponse.fromJson)
  }

  def ExperimentService_updateExperimentNameOrDescription(body: ModeldbUpdateExperimentNameOrDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateExperimentNameOrDescriptionResponse] = Await.result(ExperimentService_updateExperimentNameOrDescriptionAsync(body), Duration.Inf)

}
