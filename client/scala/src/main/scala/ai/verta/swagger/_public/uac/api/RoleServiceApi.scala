// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class RoleServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def RoleService_deleteRoleAsync(body: UacDeleteRole)(implicit ec: ExecutionContext): Future[Try[UacDeleteRoleResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteRole, UacDeleteRoleResponse]("POST", basePath + s"/role/deleteRole", __query.toMap, body, UacDeleteRoleResponse.fromJson)
  }

  def RoleService_deleteRole(body: UacDeleteRole)(implicit ec: ExecutionContext): Try[UacDeleteRoleResponse] = Await.result(RoleService_deleteRoleAsync(body), Duration.Inf)

  def RoleService_deleteRoleBindingAsync(body: UacDeleteRoleBinding)(implicit ec: ExecutionContext): Future[Try[UacDeleteRoleBindingResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteRoleBinding, UacDeleteRoleBindingResponse]("POST", basePath + s"/role/deleteRoleBinding", __query.toMap, body, UacDeleteRoleBindingResponse.fromJson)
  }

  def RoleService_deleteRoleBinding(body: UacDeleteRoleBinding)(implicit ec: ExecutionContext): Try[UacDeleteRoleBindingResponse] = Await.result(RoleService_deleteRoleBindingAsync(body), Duration.Inf)

  def RoleService_deleteRoleBindingsAsync(body: UacDeleteRoleBindings)(implicit ec: ExecutionContext): Future[Try[UacDeleteRoleBindingsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteRoleBindings, UacDeleteRoleBindingsResponse]("POST", basePath + s"/role/deleteRoleBindings", __query.toMap, body, UacDeleteRoleBindingsResponse.fromJson)
  }

  def RoleService_deleteRoleBindings(body: UacDeleteRoleBindings)(implicit ec: ExecutionContext): Try[UacDeleteRoleBindingsResponse] = Await.result(RoleService_deleteRoleBindingsAsync(body), Duration.Inf)

  def RoleService_getBindingRoleByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetRoleBindingByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, UacGetRoleBindingByIdResponse]("GET", basePath + s"/role/getRoleBindingById", __query.toMap, body, UacGetRoleBindingByIdResponse.fromJson)
  }

  def RoleService_getBindingRoleById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetRoleBindingByIdResponse] = Await.result(RoleService_getBindingRoleByIdAsync(id), Duration.Inf)

  def RoleService_getRoleBindingByNameAsync(name: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetRoleBindingByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (scope_org_id.isDefined) __query.update("scope.org_id", client.toQuery(scope_org_id.get))
    if (scope_team_id.isDefined) __query.update("scope.team_id", client.toQuery(scope_team_id.get))
    val body: String = null
    return client.request[String, UacGetRoleBindingByNameResponse]("GET", basePath + s"/role/getRoleBindingByName", __query.toMap, body, UacGetRoleBindingByNameResponse.fromJson)
  }

  def RoleService_getRoleBindingByName(name: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetRoleBindingByNameResponse] = Await.result(RoleService_getRoleBindingByNameAsync(name, scope_org_id, scope_team_id), Duration.Inf)

  def RoleService_getRoleByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetRoleByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, UacGetRoleByIdResponse]("GET", basePath + s"/role/getRoleById", __query.toMap, body, UacGetRoleByIdResponse.fromJson)
  }

  def RoleService_getRoleById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetRoleByIdResponse] = Await.result(RoleService_getRoleByIdAsync(id), Duration.Inf)

  def RoleService_getRoleByNameAsync(name: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetRoleByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (scope_org_id.isDefined) __query.update("scope.org_id", client.toQuery(scope_org_id.get))
    if (scope_team_id.isDefined) __query.update("scope.team_id", client.toQuery(scope_team_id.get))
    val body: String = null
    return client.request[String, UacGetRoleByNameResponse]("GET", basePath + s"/role/getRoleByName", __query.toMap, body, UacGetRoleByNameResponse.fromJson)
  }

  def RoleService_getRoleByName(name: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetRoleByNameResponse] = Await.result(RoleService_getRoleByNameAsync(name, scope_org_id, scope_team_id), Duration.Inf)

  def RoleService_listRoleBindingsAsync(entity_id: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacListRoleBindingsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    if (scope_org_id.isDefined) __query.update("scope.org_id", client.toQuery(scope_org_id.get))
    if (scope_team_id.isDefined) __query.update("scope.team_id", client.toQuery(scope_team_id.get))
    val body: String = null
    return client.request[String, UacListRoleBindingsResponse]("GET", basePath + s"/role/listRoleBindings", __query.toMap, body, UacListRoleBindingsResponse.fromJson)
  }

  def RoleService_listRoleBindings(entity_id: Option[String]=None, scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacListRoleBindingsResponse] = Await.result(RoleService_listRoleBindingsAsync(entity_id, scope_org_id, scope_team_id), Duration.Inf)

  def RoleService_listRolesAsync(scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacListRolesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (scope_org_id.isDefined) __query.update("scope.org_id", client.toQuery(scope_org_id.get))
    if (scope_team_id.isDefined) __query.update("scope.team_id", client.toQuery(scope_team_id.get))
    val body: String = null
    return client.request[String, UacListRolesResponse]("GET", basePath + s"/role/listRoles", __query.toMap, body, UacListRolesResponse.fromJson)
  }

  def RoleService_listRoles(scope_org_id: Option[String]=None, scope_team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacListRolesResponse] = Await.result(RoleService_listRolesAsync(scope_org_id, scope_team_id), Duration.Inf)

  def RoleService_removeResourcesAsync(resource_ids: Option[List[String]]=None, resource_type_modeldb_service_resource_type: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacRemoveResourcesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (resource_ids.isDefined) __query.update("resource_ids", client.toQuery(resource_ids.get))
    if (resource_type_modeldb_service_resource_type.isDefined) __query.update("resource_type.modeldb_service_resource_type", client.toQuery(resource_type_modeldb_service_resource_type.get))
    val body: String = null
    return client.request[String, UacRemoveResourcesResponse]("DELETE", basePath + s"/collaborator/removeResources", __query.toMap, body, UacRemoveResourcesResponse.fromJson)
  }

  def RoleService_removeResources(resource_ids: Option[List[String]]=None, resource_type_modeldb_service_resource_type: Option[String]=None)(implicit ec: ExecutionContext): Try[UacRemoveResourcesResponse] = Await.result(RoleService_removeResourcesAsync(resource_ids, resource_type_modeldb_service_resource_type), Duration.Inf)

  def RoleService_setRoleAsync(body: UacSetRole)(implicit ec: ExecutionContext): Future[Try[UacSetRoleResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetRole, UacSetRoleResponse]("POST", basePath + s"/role/setRole", __query.toMap, body, UacSetRoleResponse.fromJson)
  }

  def RoleService_setRole(body: UacSetRole)(implicit ec: ExecutionContext): Try[UacSetRoleResponse] = Await.result(RoleService_setRoleAsync(body), Duration.Inf)

  def RoleService_setRoleBindingAsync(body: UacSetRoleBinding)(implicit ec: ExecutionContext): Future[Try[UacSetRoleBindingResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetRoleBinding, UacSetRoleBindingResponse]("POST", basePath + s"/role/setRoleBinding", __query.toMap, body, UacSetRoleBindingResponse.fromJson)
  }

  def RoleService_setRoleBinding(body: UacSetRoleBinding)(implicit ec: ExecutionContext): Try[UacSetRoleBindingResponse] = Await.result(RoleService_setRoleBindingAsync(body), Duration.Inf)

}
