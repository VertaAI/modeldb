// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class UACServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def UACService_createUserAsync(body: UacCreateUser)(implicit ec: ExecutionContext): Future[Try[UacCreateUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacCreateUser, UacCreateUserResponse]("POST", basePath + s"/uac/createUser", __query.toMap, body, UacCreateUserResponse.fromJson)
  }

  def UACService_createUser(body: UacCreateUser)(implicit ec: ExecutionContext): Try[UacCreateUserResponse] = Await.result(UACService_createUserAsync(body), Duration.Inf)

  def UACService_deleteUserAsync(body: UacDeleteUser)(implicit ec: ExecutionContext): Future[Try[UacDeleteUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteUser, UacDeleteUserResponse]("POST", basePath + s"/uac/deleteUser", __query.toMap, body, UacDeleteUserResponse.fromJson)
  }

  def UACService_deleteUser(body: UacDeleteUser)(implicit ec: ExecutionContext): Try[UacDeleteUserResponse] = Await.result(UACService_deleteUserAsync(body), Duration.Inf)

  def UACService_getCurrentUserAsync()(implicit ec: ExecutionContext): Future[Try[UacUserInfo]] = {
    var __query = new mutable.HashMap[String,List[String]]
    val body: String = null
    return client.request[String, UacUserInfo]("GET", basePath + s"/uac/getCurrentUser", __query.toMap, body, UacUserInfo.fromJson)
  }

  def UACService_getCurrentUser()(implicit ec: ExecutionContext): Try[UacUserInfo] = Await.result(UACService_getCurrentUserAsync(), Duration.Inf)

  def UACService_getUserAsync(email: Option[String]=None, user_id: Option[String]=None, username: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[UacUserInfo]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (user_id.isDefined) __query.update("user_id", client.toQuery(user_id.get))
    if (email.isDefined) __query.update("email", client.toQuery(email.get))
    if (username.isDefined) __query.update("username", client.toQuery(username.get))
    val body: String = null
    return client.request[String, UacUserInfo]("GET", basePath + s"/uac/getUser", __query.toMap, body, UacUserInfo.fromJson)
  }

  def UACService_getUser(email: Option[String]=None, user_id: Option[String]=None, username: Option[String]=None)(implicit ec: ExecutionContext): Try[UacUserInfo] = Await.result(UACService_getUserAsync(email, user_id, username), Duration.Inf)

  def UACService_getUsersAsync(body: UacGetUsers)(implicit ec: ExecutionContext): Future[Try[UacGetUsersResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetUsers, UacGetUsersResponse]("POST", basePath + s"/uac/getUsers", __query.toMap, body, UacGetUsersResponse.fromJson)
  }

  def UACService_getUsers(body: UacGetUsers)(implicit ec: ExecutionContext): Try[UacGetUsersResponse] = Await.result(UACService_getUsersAsync(body), Duration.Inf)

  def UACService_getUsersFuzzyAsync(body: UacGetUsersFuzzy)(implicit ec: ExecutionContext): Future[Try[UacGetUsersFuzzyResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetUsersFuzzy, UacGetUsersFuzzyResponse]("POST", basePath + s"/uac/getUsersFuzzy", __query.toMap, body, UacGetUsersFuzzyResponse.fromJson)
  }

  def UACService_getUsersFuzzy(body: UacGetUsersFuzzy)(implicit ec: ExecutionContext): Try[UacGetUsersFuzzyResponse] = Await.result(UACService_getUsersFuzzyAsync(body), Duration.Inf)

  def UACService_updateUserAsync(body: UacUpdateUser)(implicit ec: ExecutionContext): Future[Try[UacUpdateUserResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacUpdateUser, UacUpdateUserResponse]("POST", basePath + s"/uac/updateUser", __query.toMap, body, UacUpdateUserResponse.fromJson)
  }

  def UACService_updateUser(body: UacUpdateUser)(implicit ec: ExecutionContext): Try[UacUpdateUserResponse] = Await.result(UACService_updateUserAsync(body), Duration.Inf)

}
