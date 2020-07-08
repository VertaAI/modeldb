// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class TeamApi(client: HttpClient, val basePath: String = "/v1") {
  def addUserAsync(body: UacAddTeamUser)(implicit ec: ExecutionContext): Future[Try[UacAddTeamUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacAddTeamUser, UacAddTeamUserResponse]("POST", basePath + s"/team/addUser", __query.toMap, body, UacAddTeamUserResponse.fromJson)
  }

  def addUser(body: UacAddTeamUser)(implicit ec: ExecutionContext): Try[UacAddTeamUserResponse] = Await.result(addUserAsync(body), Duration.Inf)

  def deleteTeamAsync(body: UacDeleteTeam)(implicit ec: ExecutionContext): Future[Try[UacDeleteTeamResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteTeam, UacDeleteTeamResponse]("POST", basePath + s"/team/deleteTeam", __query.toMap, body, UacDeleteTeamResponse.fromJson)
  }

  def deleteTeam(body: UacDeleteTeam)(implicit ec: ExecutionContext): Try[UacDeleteTeamResponse] = Await.result(deleteTeamAsync(body), Duration.Inf)

  def getTeamByIdAsync(team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetTeamByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (team_id.isDefined) __query.update("team_id", client.toQuery(team_id.get))
    val body: String = null
    return client.request[String, UacGetTeamByIdResponse]("GET", basePath + s"/team/getTeamById", __query.toMap, body, UacGetTeamByIdResponse.fromJson)
  }

  def getTeamById(team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetTeamByIdResponse] = Await.result(getTeamByIdAsync(team_id), Duration.Inf)

  def getTeamByNameAsync(org_id: Option[String]=None, team_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetTeamByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_id.isDefined) __query.update("org_id", client.toQuery(org_id.get))
    if (team_name.isDefined) __query.update("team_name", client.toQuery(team_name.get))
    val body: String = null
    return client.request[String, UacGetTeamByNameResponse]("GET", basePath + s"/team/getTeamByName", __query.toMap, body, UacGetTeamByNameResponse.fromJson)
  }

  def getTeamByName(org_id: Option[String]=None, team_name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetTeamByNameResponse] = Await.result(getTeamByNameAsync(org_id, team_name), Duration.Inf)

  def getTeamByShortNameAsync(org_id: Option[String]=None, short_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacGetTeamByShortNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (org_id.isDefined) __query.update("org_id", client.toQuery(org_id.get))
    if (short_name.isDefined) __query.update("short_name", client.toQuery(short_name.get))
    val body: String = null
    return client.request[String, UacGetTeamByShortNameResponse]("GET", basePath + s"/team/getTeamByShortName", __query.toMap, body, UacGetTeamByShortNameResponse.fromJson)
  }

  def getTeamByShortName(org_id: Option[String]=None, short_name: Option[String]=None)(implicit ec: ExecutionContext): Try[UacGetTeamByShortNameResponse] = Await.result(getTeamByShortNameAsync(org_id, short_name), Duration.Inf)

  def listMyTeamsAsync()(implicit ec: ExecutionContext): Future[Try[UacListMyTeamsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    val body: String = null
    return client.request[String, UacListMyTeamsResponse]("GET", basePath + s"/team/listMyTeams", __query.toMap, body, UacListMyTeamsResponse.fromJson)
  }

  def listMyTeams()(implicit ec: ExecutionContext): Try[UacListMyTeamsResponse] = Await.result(listMyTeamsAsync(), Duration.Inf)

  def listUsersAsync(team_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacListTeamUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (team_id.isDefined) __query.update("team_id", client.toQuery(team_id.get))
    val body: String = null
    return client.request[String, UacListTeamUserResponse]("GET", basePath + s"/team/listUsers", __query.toMap, body, UacListTeamUserResponse.fromJson)
  }

  def listUsers(team_id: Option[String]=None)(implicit ec: ExecutionContext): Try[UacListTeamUserResponse] = Await.result(listUsersAsync(team_id), Duration.Inf)

  def removeUserAsync(body: UacRemoveTeamUser)(implicit ec: ExecutionContext): Future[Try[UacRemoveTeamUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacRemoveTeamUser, UacRemoveTeamUserResponse]("POST", basePath + s"/team/removeUser", __query.toMap, body, UacRemoveTeamUserResponse.fromJson)
  }

  def removeUser(body: UacRemoveTeamUser)(implicit ec: ExecutionContext): Try[UacRemoveTeamUserResponse] = Await.result(removeUserAsync(body), Duration.Inf)

  def setTeamAsync(body: UacSetTeam)(implicit ec: ExecutionContext): Future[Try[UacSetTeamResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacSetTeam, UacSetTeamResponse]("POST", basePath + s"/team/setTeam", __query.toMap, body, UacSetTeamResponse.fromJson)
  }

  def setTeam(body: UacSetTeam)(implicit ec: ExecutionContext): Try[UacSetTeamResponse] = Await.result(setTeamAsync(body), Duration.Inf)

}
