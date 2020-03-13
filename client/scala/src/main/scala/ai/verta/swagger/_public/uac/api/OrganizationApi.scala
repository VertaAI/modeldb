// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class OrganizationApi(client: HttpClient, val basePath: String = "/v1") {
  def addUserAsync(body: UacAddUser)(implicit ec: ExecutionContext): Future[Try[UacAddUserResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddUser, UacAddUserResponse]("POST", basePath + s"/organization/addUser", __query, body, UacAddUserResponse.fromJson)
  }

  def addUser(body: UacAddUser)(implicit ec: ExecutionContext): Try[UacAddUserResponse] = Await.result(addUserAsync(body), Duration.Inf)

  def deleteOrganizationAsync(body: UacDeleteOrganization)(implicit ec: ExecutionContext): Future[Try[UacDeleteOrganizationResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteOrganization, UacDeleteOrganizationResponse]("POST", basePath + s"/organization/deleteOrganization", __query, body, UacDeleteOrganizationResponse.fromJson)
  }

  def deleteOrganization(body: UacDeleteOrganization)(implicit ec: ExecutionContext): Try[UacDeleteOrganizationResponse] = Await.result(deleteOrganizationAsync(body), Duration.Inf)

  def getOrganizationByIdAsync(org_id: String)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByIdResponse]] = {
    val __query = Map[String,String](
      "org_id" -> client.toQuery(org_id)
    )
    val body: String = null
    return client.request[String, UacGetOrganizationByIdResponse]("GET", basePath + s"/organization/getOrganizationById", __query, body, UacGetOrganizationByIdResponse.fromJson)
  }

  def getOrganizationById(org_id: String)(implicit ec: ExecutionContext): Try[UacGetOrganizationByIdResponse] = Await.result(getOrganizationByIdAsync(org_id), Duration.Inf)

  def getOrganizationByNameAsync(org_name: String)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByNameResponse]] = {
    val __query = Map[String,String](
      "org_name" -> client.toQuery(org_name)
    )
    val body: String = null
    return client.request[String, UacGetOrganizationByNameResponse]("GET", basePath + s"/organization/getOrganizationByName", __query, body, UacGetOrganizationByNameResponse.fromJson)
  }

  def getOrganizationByName(org_name: String)(implicit ec: ExecutionContext): Try[UacGetOrganizationByNameResponse] = Await.result(getOrganizationByNameAsync(org_name), Duration.Inf)

  def getOrganizationByShortNameAsync(short_name: String)(implicit ec: ExecutionContext): Future[Try[UacGetOrganizationByShortNameResponse]] = {
    val __query = Map[String,String](
      "short_name" -> client.toQuery(short_name)
    )
    val body: String = null
    return client.request[String, UacGetOrganizationByShortNameResponse]("GET", basePath + s"/organization/getOrganizationByShortName", __query, body, UacGetOrganizationByShortNameResponse.fromJson)
  }

  def getOrganizationByShortName(short_name: String)(implicit ec: ExecutionContext): Try[UacGetOrganizationByShortNameResponse] = Await.result(getOrganizationByShortNameAsync(short_name), Duration.Inf)

  def listMyOrganizationsAsync()(implicit ec: ExecutionContext): Future[Try[UacListMyOrganizationsResponse]] = {
    val __query = Map[String,String](
    )
    val body: String = null
    return client.request[String, UacListMyOrganizationsResponse]("GET", basePath + s"/organization/listMyOrganizations", __query, body, UacListMyOrganizationsResponse.fromJson)
  }

  def listMyOrganizations()(implicit ec: ExecutionContext): Try[UacListMyOrganizationsResponse] = Await.result(listMyOrganizationsAsync(), Duration.Inf)

  def listTeamsAsync(org_id: String)(implicit ec: ExecutionContext): Future[Try[UacListTeamsResponse]] = {
    val __query = Map[String,String](
      "org_id" -> client.toQuery(org_id)
    )
    val body: String = null
    return client.request[String, UacListTeamsResponse]("GET", basePath + s"/organization/listTeams", __query, body, UacListTeamsResponse.fromJson)
  }

  def listTeams(org_id: String)(implicit ec: ExecutionContext): Try[UacListTeamsResponse] = Await.result(listTeamsAsync(org_id), Duration.Inf)

  def listUsersAsync(org_id: String)(implicit ec: ExecutionContext): Future[Try[UacListUsersResponse]] = {
    val __query = Map[String,String](
      "org_id" -> client.toQuery(org_id)
    )
    val body: String = null
    return client.request[String, UacListUsersResponse]("GET", basePath + s"/organization/listUsers", __query, body, UacListUsersResponse.fromJson)
  }

  def listUsers(org_id: String)(implicit ec: ExecutionContext): Try[UacListUsersResponse] = Await.result(listUsersAsync(org_id), Duration.Inf)

  def removeUserAsync(body: UacRemoveUser)(implicit ec: ExecutionContext): Future[Try[UacRemoveUserResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacRemoveUser, UacRemoveUserResponse]("POST", basePath + s"/organization/removeUser", __query, body, UacRemoveUserResponse.fromJson)
  }

  def removeUser(body: UacRemoveUser)(implicit ec: ExecutionContext): Try[UacRemoveUserResponse] = Await.result(removeUserAsync(body), Duration.Inf)

  def setOrganizationAsync(body: UacSetOrganization)(implicit ec: ExecutionContext): Future[Try[UacSetOrganizationResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetOrganization, UacSetOrganizationResponse]("POST", basePath + s"/organization/setOrganization", __query, body, UacSetOrganizationResponse.fromJson)
  }

  def setOrganization(body: UacSetOrganization)(implicit ec: ExecutionContext): Try[UacSetOrganizationResponse] = Await.result(setOrganizationAsync(body), Duration.Inf)

}
