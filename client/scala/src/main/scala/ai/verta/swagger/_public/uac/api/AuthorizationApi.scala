// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class AuthorizationApi(client: HttpClient, val basePath: String = "/v1") {
  def AuthzService_getAllowedEntitiesAsync(body: UacGetAllowedEntities)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedEntitiesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedEntities, UacGetAllowedEntitiesResponse]("POST", basePath + s"/authz/getAllowedEntities", __query.toMap, body, UacGetAllowedEntitiesResponse.fromJson)
  }

  def AuthzService_getAllowedEntities(body: UacGetAllowedEntities)(implicit ec: ExecutionContext): Try[UacGetAllowedEntitiesResponse] = Await.result(AuthzService_getAllowedEntitiesAsync(body), Duration.Inf)

  def AuthzService_getAllowedEntitiesWithActionsAsync(body: UacGetAllowedEntitiesWithActions)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedEntitiesWithActionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedEntitiesWithActions, UacGetAllowedEntitiesWithActionsResponse]("POST", basePath + s"/authz/getAllowedEntitiesWithActions", __query.toMap, body, UacGetAllowedEntitiesWithActionsResponse.fromJson)
  }

  def AuthzService_getAllowedEntitiesWithActions(body: UacGetAllowedEntitiesWithActions)(implicit ec: ExecutionContext): Try[UacGetAllowedEntitiesWithActionsResponse] = Await.result(AuthzService_getAllowedEntitiesWithActionsAsync(body), Duration.Inf)

  def AuthzService_getAllowedResourcesAsync(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedResourcesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedResources, UacGetAllowedResourcesResponse]("POST", basePath + s"/authz/getAllowedResources", __query.toMap, body, UacGetAllowedResourcesResponse.fromJson)
  }

  def AuthzService_getAllowedResources(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Try[UacGetAllowedResourcesResponse] = Await.result(AuthzService_getAllowedResourcesAsync(body), Duration.Inf)

  def AuthzService_getDireclyAllowedResourcesAsync(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedResourcesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedResources, UacGetAllowedResourcesResponse]("POST", basePath + s"/authz/getDirectlyAllowedResources", __query.toMap, body, UacGetAllowedResourcesResponse.fromJson)
  }

  def AuthzService_getDireclyAllowedResources(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Try[UacGetAllowedResourcesResponse] = Await.result(AuthzService_getDireclyAllowedResourcesAsync(body), Duration.Inf)

  def AuthzService_getSelfAllowedActionsBatchAsync(body: UacGetSelfAllowedActionsBatch)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedActionsBatchResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedActionsBatch, UacGetSelfAllowedActionsBatchResponse]("POST", basePath + s"/authz/getSelfAllowedActionsBatch", __query.toMap, body, UacGetSelfAllowedActionsBatchResponse.fromJson)
  }

  def AuthzService_getSelfAllowedActionsBatch(body: UacGetSelfAllowedActionsBatch)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedActionsBatchResponse] = Await.result(AuthzService_getSelfAllowedActionsBatchAsync(body), Duration.Inf)

  def AuthzService_getSelfAllowedResourcesAsync(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedResourcesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedResources, UacGetSelfAllowedResourcesResponse]("POST", basePath + s"/authz/getSelfAllowedResources", __query.toMap, body, UacGetSelfAllowedResourcesResponse.fromJson)
  }

  def AuthzService_getSelfAllowedResources(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedResourcesResponse] = Await.result(AuthzService_getSelfAllowedResourcesAsync(body), Duration.Inf)

  def AuthzService_getSelfDirectlyAllowedResourcesAsync(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedResourcesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedResources, UacGetSelfAllowedResourcesResponse]("POST", basePath + s"/authz/getSelfDirectlyAllowedResources", __query.toMap, body, UacGetSelfAllowedResourcesResponse.fromJson)
  }

  def AuthzService_getSelfDirectlyAllowedResources(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedResourcesResponse] = Await.result(AuthzService_getSelfDirectlyAllowedResourcesAsync(body), Duration.Inf)

  def AuthzService_isAllowedAsync(body: UacIsAllowed)(implicit ec: ExecutionContext): Future[Try[UacIsAllowedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacIsAllowed, UacIsAllowedResponse]("POST", basePath + s"/authz/isAllowed", __query.toMap, body, UacIsAllowedResponse.fromJson)
  }

  def AuthzService_isAllowed(body: UacIsAllowed)(implicit ec: ExecutionContext): Try[UacIsAllowedResponse] = Await.result(AuthzService_isAllowedAsync(body), Duration.Inf)

  def AuthzService_isSelfAllowedAsync(body: UacIsSelfAllowed)(implicit ec: ExecutionContext): Future[Try[UacIsSelfAllowedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacIsSelfAllowed, UacIsSelfAllowedResponse]("POST", basePath + s"/authz/isSelfAllowed", __query.toMap, body, UacIsSelfAllowedResponse.fromJson)
  }

  def AuthzService_isSelfAllowed(body: UacIsSelfAllowed)(implicit ec: ExecutionContext): Try[UacIsSelfAllowedResponse] = Await.result(AuthzService_isSelfAllowedAsync(body), Duration.Inf)

}
