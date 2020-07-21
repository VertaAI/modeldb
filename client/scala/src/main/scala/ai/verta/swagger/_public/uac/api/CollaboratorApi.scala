// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class CollaboratorApi(client: HttpClient, val basePath: String = "/v1") {
  def CollaboratorService_addOrUpdateDatasetCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateDatasetCollaborator", __query.toMap, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def CollaboratorService_addOrUpdateDatasetCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(CollaboratorService_addOrUpdateDatasetCollaboratorAsync(body), Duration.Inf)

  def CollaboratorService_addOrUpdateProjectCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateProjectCollaborator", __query.toMap, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def CollaboratorService_addOrUpdateProjectCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(CollaboratorService_addOrUpdateProjectCollaboratorAsync(body), Duration.Inf)

  def CollaboratorService_addOrUpdateRepositoryCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateRepositoryCollaborator", __query.toMap, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def CollaboratorService_addOrUpdateRepositoryCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(CollaboratorService_addOrUpdateRepositoryCollaboratorAsync(body), Duration.Inf)

  def CollaboratorService_getDatasetCollaboratorsAsync(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getDatasetCollaborators", __query.toMap, body, UacGetCollaboratorResponse.fromJson)
  }

  def CollaboratorService_getDatasetCollaborators(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(CollaboratorService_getDatasetCollaboratorsAsync(entity_id), Duration.Inf)

  def CollaboratorService_getProjectCollaboratorsAsync(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getProjectCollaborators", __query.toMap, body, UacGetCollaboratorResponse.fromJson)
  }

  def CollaboratorService_getProjectCollaborators(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(CollaboratorService_getProjectCollaboratorsAsync(entity_id), Duration.Inf)

  def CollaboratorService_getRepositoryCollaboratorsAsync(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getRepositoryCollaborators", __query.toMap, body, UacGetCollaboratorResponse.fromJson)
  }

  def CollaboratorService_getRepositoryCollaborators(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(CollaboratorService_getRepositoryCollaboratorsAsync(entity_id), Duration.Inf)

  def CollaboratorService_removeDatasetCollaboratorAsync(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    if (share_with.isDefined) __query.update("share_with", client.toQuery(share_with.get))
    if (date_deleted.isDefined) __query.update("date_deleted", client.toQuery(date_deleted.get))
    if (authz_entity_type.isDefined) __query.update("authz_entity_type", client.toQuery(authz_entity_type.get))
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeDatasetCollaborator", __query.toMap, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def CollaboratorService_removeDatasetCollaborator(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(CollaboratorService_removeDatasetCollaboratorAsync(authz_entity_type, date_deleted, entity_id, share_with), Duration.Inf)

  def CollaboratorService_removeProjectCollaboratorAsync(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    if (share_with.isDefined) __query.update("share_with", client.toQuery(share_with.get))
    if (date_deleted.isDefined) __query.update("date_deleted", client.toQuery(date_deleted.get))
    if (authz_entity_type.isDefined) __query.update("authz_entity_type", client.toQuery(authz_entity_type.get))
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeProjectCollaborator", __query.toMap, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def CollaboratorService_removeProjectCollaborator(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(CollaboratorService_removeProjectCollaboratorAsync(authz_entity_type, date_deleted, entity_id, share_with), Duration.Inf)

  def CollaboratorService_removeRepositoryCollaboratorAsync(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    if (share_with.isDefined) __query.update("share_with", client.toQuery(share_with.get))
    if (date_deleted.isDefined) __query.update("date_deleted", client.toQuery(date_deleted.get))
    if (authz_entity_type.isDefined) __query.update("authz_entity_type", client.toQuery(authz_entity_type.get))
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeRepositoryCollaborator", __query.toMap, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def CollaboratorService_removeRepositoryCollaborator(authz_entity_type: Option[String]=None, date_deleted: Option[BigInt]=None, entity_id: Option[String]=None, share_with: Option[String]=None)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(CollaboratorService_removeRepositoryCollaboratorAsync(authz_entity_type, date_deleted, entity_id, share_with), Duration.Inf)

}
