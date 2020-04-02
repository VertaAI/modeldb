// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class CollaboratorApi(client: HttpClient, val basePath: String = "/v1") {
  def addOrUpdateDatasetCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateDatasetCollaborator", __query, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def addOrUpdateDatasetCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(addOrUpdateDatasetCollaboratorAsync(body), Duration.Inf)

  def addOrUpdateProjectCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateProjectCollaborator", __query, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def addOrUpdateProjectCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(addOrUpdateProjectCollaboratorAsync(body), Duration.Inf)

  def addOrUpdateRepositoryCollaboratorAsync(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Future[Try[UacAddCollaboratorRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddCollaboratorRequest, UacAddCollaboratorRequestResponse]("POST", basePath + s"/collaborator/addOrUpdateRepositoryCollaborator", __query, body, UacAddCollaboratorRequestResponse.fromJson)
  }

  def addOrUpdateRepositoryCollaborator(body: UacAddCollaboratorRequest)(implicit ec: ExecutionContext): Try[UacAddCollaboratorRequestResponse] = Await.result(addOrUpdateRepositoryCollaboratorAsync(body), Duration.Inf)

  def getDatasetCollaboratorsAsync(entity_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id)
    )
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getDatasetCollaborators", __query, body, UacGetCollaboratorResponse.fromJson)
  }

  def getDatasetCollaborators(entity_id: String)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(getDatasetCollaboratorsAsync(entity_id), Duration.Inf)

  def getProjectCollaboratorsAsync(entity_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id)
    )
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getProjectCollaborators", __query, body, UacGetCollaboratorResponse.fromJson)
  }

  def getProjectCollaborators(entity_id: String)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(getProjectCollaboratorsAsync(entity_id), Duration.Inf)

  def getRepositoryCollaboratorsAsync(entity_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id)
    )
    val body: String = null
    return client.request[String, UacGetCollaboratorResponse]("GET", basePath + s"/collaborator/getRepositoryCollaborators", __query, body, UacGetCollaboratorResponse.fromJson)
  }

  def getRepositoryCollaborators(entity_id: String)(implicit ec: ExecutionContext): Try[UacGetCollaboratorResponse] = Await.result(getRepositoryCollaboratorsAsync(entity_id), Duration.Inf)

  def removeDatasetCollaboratorAsync(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id),
      "share_with" -> client.toQuery(share_with),
      "date_deleted" -> client.toQuery(date_deleted),
      "authz_entity_type" -> client.toQuery(authz_entity_type)
    )
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeDatasetCollaborator", __query, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def removeDatasetCollaborator(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(removeDatasetCollaboratorAsync(entity_id, share_with, date_deleted, authz_entity_type), Duration.Inf)

  def removeProjectCollaboratorAsync(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id),
      "share_with" -> client.toQuery(share_with),
      "date_deleted" -> client.toQuery(date_deleted),
      "authz_entity_type" -> client.toQuery(authz_entity_type)
    )
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeProjectCollaborator", __query, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def removeProjectCollaborator(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(removeProjectCollaboratorAsync(entity_id, share_with, date_deleted, authz_entity_type), Duration.Inf)

  def removeRepositoryCollaboratorAsync(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Future[Try[UacRemoveCollaboratorResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id),
      "share_with" -> client.toQuery(share_with),
      "date_deleted" -> client.toQuery(date_deleted),
      "authz_entity_type" -> client.toQuery(authz_entity_type)
    )
    val body: String = null
    return client.request[String, UacRemoveCollaboratorResponse]("DELETE", basePath + s"/collaborator/removeRepositoryCollaborator", __query, body, UacRemoveCollaboratorResponse.fromJson)
  }

  def removeRepositoryCollaborator(entity_id: String, share_with: String, date_deleted: , authz_entity_type: String)(implicit ec: ExecutionContext): Try[UacRemoveCollaboratorResponse] = Await.result(removeRepositoryCollaboratorAsync(entity_id, share_with, date_deleted, authz_entity_type), Duration.Inf)

}
