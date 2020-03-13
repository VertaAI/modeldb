// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class AuthorizationApi(client: HttpClient, val basePath: String = "/v1") {
  def getAllowedEntitiesAsync(body: UacGetAllowedEntities)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedEntitiesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedEntities, UacGetAllowedEntitiesResponse]("POST", basePath + s"/authz/getAllowedEntities", __query, body, UacGetAllowedEntitiesResponse.fromJson)
  }

  def getAllowedEntities(body: UacGetAllowedEntities)(implicit ec: ExecutionContext): Try[UacGetAllowedEntitiesResponse] = Await.result(getAllowedEntitiesAsync(body), Duration.Inf)

  def getAllowedResourcesAsync(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedResourcesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedResources, UacGetAllowedResourcesResponse]("POST", basePath + s"/authz/getAllowedResources", __query, body, UacGetAllowedResourcesResponse.fromJson)
  }

  def getAllowedResources(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Try[UacGetAllowedResourcesResponse] = Await.result(getAllowedResourcesAsync(body), Duration.Inf)

  def getDireclyAllowedResourcesAsync(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetAllowedResourcesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetAllowedResources, UacGetAllowedResourcesResponse]("POST", basePath + s"/authz/getDirectlyAllowedResources", __query, body, UacGetAllowedResourcesResponse.fromJson)
  }

  def getDireclyAllowedResources(body: UacGetAllowedResources)(implicit ec: ExecutionContext): Try[UacGetAllowedResourcesResponse] = Await.result(getDireclyAllowedResourcesAsync(body), Duration.Inf)

  def getSelfAllowedActionsBatchAsync(body: UacGetSelfAllowedActionsBatch)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedActionsBatchResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedActionsBatch, UacGetSelfAllowedActionsBatchResponse]("POST", basePath + s"/authz/getSelfAllowedActionsBatch", __query, body, UacGetSelfAllowedActionsBatchResponse.fromJson)
  }

  def getSelfAllowedActionsBatch(body: UacGetSelfAllowedActionsBatch)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedActionsBatchResponse] = Await.result(getSelfAllowedActionsBatchAsync(body), Duration.Inf)

  def getSelfAllowedResourcesAsync(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedResourcesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedResources, UacGetSelfAllowedResourcesResponse]("POST", basePath + s"/authz/getSelfAllowedResources", __query, body, UacGetSelfAllowedResourcesResponse.fromJson)
  }

  def getSelfAllowedResources(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedResourcesResponse] = Await.result(getSelfAllowedResourcesAsync(body), Duration.Inf)

  def getSelfDirectlyAllowedResourcesAsync(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Future[Try[UacGetSelfAllowedResourcesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetSelfAllowedResources, UacGetSelfAllowedResourcesResponse]("POST", basePath + s"/authz/getSelfDirectlyAllowedResources", __query, body, UacGetSelfAllowedResourcesResponse.fromJson)
  }

  def getSelfDirectlyAllowedResources(body: UacGetSelfAllowedResources)(implicit ec: ExecutionContext): Try[UacGetSelfAllowedResourcesResponse] = Await.result(getSelfDirectlyAllowedResourcesAsync(body), Duration.Inf)

  def isAllowedAsync(body: UacIsAllowed)(implicit ec: ExecutionContext): Future[Try[UacIsAllowedResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacIsAllowed, UacIsAllowedResponse]("POST", basePath + s"/authz/isAllowed", __query, body, UacIsAllowedResponse.fromJson)
  }

  def isAllowed(body: UacIsAllowed)(implicit ec: ExecutionContext): Try[UacIsAllowedResponse] = Await.result(isAllowedAsync(body), Duration.Inf)

  def isSelfAllowedAsync(body: UacIsSelfAllowed)(implicit ec: ExecutionContext): Future[Try[UacIsSelfAllowedResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacIsSelfAllowed, UacIsSelfAllowedResponse]("POST", basePath + s"/authz/isSelfAllowed", __query, body, UacIsSelfAllowedResponse.fromJson)
  }

  def isSelfAllowed(body: UacIsSelfAllowed)(implicit ec: ExecutionContext): Try[UacIsSelfAllowedResponse] = Await.result(isSelfAllowedAsync(body), Duration.Inf)

}
