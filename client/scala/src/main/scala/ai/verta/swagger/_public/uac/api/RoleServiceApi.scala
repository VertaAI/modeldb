// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class RoleServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def deleteRoleAsync(body: UacDeleteRole)(implicit ec: ExecutionContext): Future[Try[UacDeleteRoleResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteRole, UacDeleteRoleResponse]("POST", basePath + s"/role/deleteRole", __query, body, UacDeleteRoleResponse.fromJson)
  }

  def deleteRole(body: UacDeleteRole)(implicit ec: ExecutionContext): Try[UacDeleteRoleResponse] = Await.result(deleteRoleAsync(body), Duration.Inf)

  def deleteRoleBindingAsync(body: UacDeleteRoleBinding)(implicit ec: ExecutionContext): Future[Try[UacDeleteRoleBindingResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteRoleBinding, UacDeleteRoleBindingResponse]("POST", basePath + s"/role/deleteRoleBinding", __query, body, UacDeleteRoleBindingResponse.fromJson)
  }

  def deleteRoleBinding(body: UacDeleteRoleBinding)(implicit ec: ExecutionContext): Try[UacDeleteRoleBindingResponse] = Await.result(deleteRoleBindingAsync(body), Duration.Inf)

  def getBindingRoleByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[UacGetRoleBindingByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, UacGetRoleBindingByIdResponse]("GET", basePath + s"/role/getRoleBindingById", __query, body, UacGetRoleBindingByIdResponse.fromJson)
  }

  def getBindingRoleById(id: String)(implicit ec: ExecutionContext): Try[UacGetRoleBindingByIdResponse] = Await.result(getBindingRoleByIdAsync(id), Duration.Inf)

  def getRoleBindingByNameAsync(name: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetRoleBindingByNameResponse]] = {
    val __query = Map[String,String](
      "name" -> client.toQuery(name),
      "scope.org_id" -> client.toQuery(scope_org_id),
      "scope.team_id" -> client.toQuery(scope_team_id)
    )
    val body: String = null
    return client.request[String, UacGetRoleBindingByNameResponse]("GET", basePath + s"/role/getRoleBindingByName", __query, body, UacGetRoleBindingByNameResponse.fromJson)
  }

  def getRoleBindingByName(name: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Try[UacGetRoleBindingByNameResponse] = Await.result(getRoleBindingByNameAsync(name, scope_org_id, scope_team_id), Duration.Inf)

  def getRoleByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[UacGetRoleByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, UacGetRoleByIdResponse]("GET", basePath + s"/role/getRoleById", __query, body, UacGetRoleByIdResponse.fromJson)
  }

  def getRoleById(id: String)(implicit ec: ExecutionContext): Try[UacGetRoleByIdResponse] = Await.result(getRoleByIdAsync(id), Duration.Inf)

  def getRoleByNameAsync(name: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetRoleByNameResponse]] = {
    val __query = Map[String,String](
      "name" -> client.toQuery(name),
      "scope.org_id" -> client.toQuery(scope_org_id),
      "scope.team_id" -> client.toQuery(scope_team_id)
    )
    val body: String = null
    return client.request[String, UacGetRoleByNameResponse]("GET", basePath + s"/role/getRoleByName", __query, body, UacGetRoleByNameResponse.fromJson)
  }

  def getRoleByName(name: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Try[UacGetRoleByNameResponse] = Await.result(getRoleByNameAsync(name, scope_org_id, scope_team_id), Duration.Inf)

  def listRoleBindingsAsync(entity_id: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Future[Try[UacListRoleBindingsResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id),
      "scope.org_id" -> client.toQuery(scope_org_id),
      "scope.team_id" -> client.toQuery(scope_team_id)
    )
    val body: String = null
    return client.request[String, UacListRoleBindingsResponse]("GET", basePath + s"/role/listRoleBindings", __query, body, UacListRoleBindingsResponse.fromJson)
  }

  def listRoleBindings(entity_id: String, scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Try[UacListRoleBindingsResponse] = Await.result(listRoleBindingsAsync(entity_id, scope_org_id, scope_team_id), Duration.Inf)

  def listRolesAsync(scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Future[Try[UacListRolesResponse]] = {
    val __query = Map[String,String](
      "scope.org_id" -> client.toQuery(scope_org_id),
      "scope.team_id" -> client.toQuery(scope_team_id)
    )
    val body: String = null
    return client.request[String, UacListRolesResponse]("GET", basePath + s"/role/listRoles", __query, body, UacListRolesResponse.fromJson)
  }

  def listRoles(scope_org_id: String, scope_team_id: String)(implicit ec: ExecutionContext): Try[UacListRolesResponse] = Await.result(listRolesAsync(scope_org_id, scope_team_id), Duration.Inf)

  def setRoleAsync(body: UacSetRole)(implicit ec: ExecutionContext): Future[Try[UacSetRoleResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetRole, UacSetRoleResponse]("POST", basePath + s"/role/setRole", __query, body, UacSetRoleResponse.fromJson)
  }

  def setRole(body: UacSetRole)(implicit ec: ExecutionContext): Try[UacSetRoleResponse] = Await.result(setRoleAsync(body), Duration.Inf)

  def setRoleBindingAsync(body: UacSetRoleBinding)(implicit ec: ExecutionContext): Future[Try[UacSetRoleBindingResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetRoleBinding, UacSetRoleBindingResponse]("POST", basePath + s"/role/setRoleBinding", __query, body, UacSetRoleBindingResponse.fromJson)
  }

  def setRoleBinding(body: UacSetRoleBinding)(implicit ec: ExecutionContext): Try[UacSetRoleBindingResponse] = Await.result(setRoleBindingAsync(body), Duration.Inf)

}
