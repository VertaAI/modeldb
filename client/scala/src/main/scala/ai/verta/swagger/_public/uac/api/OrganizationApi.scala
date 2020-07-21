// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class OrganizationApi(client: HttpClient, val basePath: String = "/v1") {
  def OrganizationService_addUserAsync(body: UacAddUser)(implicit ec: ExecutionContext): Future[Try[UacAddUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddUser, UacAddUserResponse]("POST", basePath + s"/organization/addUser", __query.toMap, body, UacAddUserResponse.fromJson)
  }

  def OrganizationService_addUser(body: UacAddUser)(implicit ec: ExecutionContext): Try[UacAddUserResponse] = Await.result(OrganizationService_addUserAsync(body), Duration.Inf)

  def OrganizationService_deleteOrganizationAsync(body: UacDeleteOrganization)(implicit ec: ExecutionContext): Future[Try[UacDeleteOrganizationResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteOrganization, UacDeleteOrganizationResponse]("POST", basePath + s"/organization/deleteOrganization", __query.toMap, body, UacDeleteOrganizationResponse.fromJson)
  }

  def OrganizationService_deleteOrganization(body: UacDeleteOrganization)(implicit ec: ExecutionContext): Try[UacDeleteOrganizationResponse] = Await.result(OrganizationService_deleteOrganizationAsync(body), Duration.Inf)

  def OrganizationService_getOrganizationByIdAsync(org_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_id.isDefined) __query.update("org_id", client.toQuery(org_id.get))
    val body: String = null
    return client.request[String, UacGetOrganizationByIdResponse]("GET", basePath + s"/organization/getOrganizationById", __query.toMap, body, UacGetOrganizationByIdResponse.fromJson)
  }

  def OrganizationService_getOrganizationById(org_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetOrganizationByIdResponse] = Await.result(OrganizationService_getOrganizationByIdAsync(org_id), Duration.Inf)

  def OrganizationService_getOrganizationByNameAsync(org_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_name.isDefined) __query.update("org_name", client.toQuery(org_name.get))
    val body: String = null
    return client.request[String, UacGetOrganizationByNameResponse]("GET", basePath + s"/organization/getOrganizationByName", __query.toMap, body, UacGetOrganizationByNameResponse.fromJson)
  }

  def OrganizationService_getOrganizationByName(org_name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetOrganizationByNameResponse] = Await.result(OrganizationService_getOrganizationByNameAsync(org_name), Duration.Inf)

  def OrganizationService_getOrganizationByShortNameAsync(short_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByShortNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (short_name.isDefined) __query.update("short_name", client.toQuery(short_name.get))
    val body: String = null
    return client.request[String, UacGetOrganizationByShortNameResponse]("GET", basePath + s"/organization/getOrganizationByShortName", __query.toMap, body, UacGetOrganizationByShortNameResponse.fromJson)
  }

  def OrganizationService_getOrganizationByShortName(short_name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetOrganizationByShortNameResponse] = Await.result(OrganizationService_getOrganizationByShortNameAsync(short_name), Duration.Inf)

  def OrganizationService_listMyOrganizationsAsync()(implicit ec: ExecutionContext): Future[Try[UacListMyOrganizationsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    val body: String = null
    return client.request[String, UacListMyOrganizationsResponse]("GET", basePath + s"/organization/listMyOrganizations", __query.toMap, body, UacListMyOrganizationsResponse.fromJson)
  }

  def OrganizationService_listMyOrganizations()(implicit ec: ExecutionContext): Try[UacListMyOrganizationsResponse] = Await.result(OrganizationService_listMyOrganizationsAsync(), Duration.Inf)

  def OrganizationService_listTeamsAsync(org_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacListTeamsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_id.isDefined) __query.update("org_id", client.toQuery(org_id.get))
    val body: String = null
    return client.request[String, UacListTeamsResponse]("GET", basePath + s"/organization/listTeams", __query.toMap, body, UacListTeamsResponse.fromJson)
  }

  def OrganizationService_listTeams(org_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacListTeamsResponse] = Await.result(OrganizationService_listTeamsAsync(org_id), Duration.Inf)

  def OrganizationService_listUsersAsync(org_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacListUsersResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_id.isDefined) __query.update("org_id", client.toQuery(org_id.get))
    val body: String = null
    return client.request[String, UacListUsersResponse]("GET", basePath + s"/organization/listUsers", __query.toMap, body, UacListUsersResponse.fromJson)
  }

  def OrganizationService_listUsers(org_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacListUsersResponse] = Await.result(OrganizationService_listUsersAsync(org_id), Duration.Inf)

  def OrganizationService_removeUserAsync(body: UacRemoveUser)(implicit ec: ExecutionContext): Future[Try[UacRemoveUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacRemoveUser, UacRemoveUserResponse]("POST", basePath + s"/organization/removeUser", __query.toMap, body, UacRemoveUserResponse.fromJson)
  }

  def OrganizationService_removeUser(body: UacRemoveUser)(implicit ec: ExecutionContext): Try[UacRemoveUserResponse] = Await.result(OrganizationService_removeUserAsync(body), Duration.Inf)

  def OrganizationService_setOrganizationAsync(body: UacSetOrganization)(implicit ec: ExecutionContext): Future[Try[UacSetOrganizationResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetOrganization, UacSetOrganizationResponse]("POST", basePath + s"/organization/setOrganization", __query.toMap, body, UacSetOrganizationResponse.fromJson)
  }

  def OrganizationService_setOrganization(body: UacSetOrganization)(implicit ec: ExecutionContext): Try[UacSetOrganizationResponse] = Await.result(OrganizationService_setOrganizationAsync(body), Duration.Inf)

}
