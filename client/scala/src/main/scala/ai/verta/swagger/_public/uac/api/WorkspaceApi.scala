// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class WorkspaceApi(client: HttpClient, val basePath: String = "/v1") {
  def WorkspaceService_getWorkspaceByIdAsync(id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[UacWorkspace]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, UacWorkspace]("GET", basePath + s"/workspace/getWorkspaceById", __query.toMap, body, UacWorkspace.fromJson)
  }

  def WorkspaceService_getWorkspaceById(id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[UacWorkspace] = Await.result(WorkspaceService_getWorkspaceByIdAsync(id), Duration.Inf)

  def WorkspaceService_getWorkspaceByLegacyIdAsync(id: Option[String]=None, workspace_type: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacWorkspace]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (workspace_type.isDefined) __query.update("workspace_type", client.toQuery(workspace_type.get))
    val body: String = null
    return client.request[String, UacWorkspace]("GET", basePath + s"/workspace/getWorkspaceByLegacyId", __query.toMap, body, UacWorkspace.fromJson)
  }

  def WorkspaceService_getWorkspaceByLegacyId(id: Option[String]=None, workspace_type: Option[String]=None)(implicit ec: ExecutionContext): Try[UacWorkspace] = Await.result(WorkspaceService_getWorkspaceByLegacyIdAsync(id, workspace_type), Duration.Inf)

  def WorkspaceService_getWorkspaceByNameAsync(name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacWorkspace]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    val body: String = null
    return client.request[String, UacWorkspace]("GET", basePath + s"/workspace/getWorkspaceByName", __query.toMap, body, UacWorkspace.fromJson)
  }

  def WorkspaceService_getWorkspaceByName(name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacWorkspace] = Await.result(WorkspaceService_getWorkspaceByNameAsync(name), Duration.Inf)

}
