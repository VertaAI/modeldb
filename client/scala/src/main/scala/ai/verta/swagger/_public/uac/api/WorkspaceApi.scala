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
  def getWorkspaceByIdAsync(id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[UacWorkspace]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, UacWorkspace]("GET", basePath + s"/workspace/getWorkspaceById", __query.toMap, body, UacWorkspace.fromJson)
  }

  def getWorkspaceById(id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[UacWorkspace] = Await.result(getWorkspaceByIdAsync(id), Duration.Inf)

  def getWorkspaceByNameAsync(name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacWorkspace]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    val body: String = null
    return client.request[String, UacWorkspace]("GET", basePath + s"/workspace/getWorkspaceByName", __query.toMap, body, UacWorkspace.fromJson)
  }

  def getWorkspaceByName(name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacWorkspace] = Await.result(getWorkspaceByNameAsync(name), Duration.Inf)

}
