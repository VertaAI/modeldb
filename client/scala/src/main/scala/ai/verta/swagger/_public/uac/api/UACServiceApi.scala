// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class UACServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def createUserAsync(body: UacCreateUser)(implicit ec: ExecutionContext): Future[Try[UacCreateUserResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacCreateUser, UacCreateUserResponse]("POST", basePath + s"/uac/createUser", __query, body, UacCreateUserResponse.fromJson)
  }

  def createUser(body: UacCreateUser)(implicit ec: ExecutionContext): Try[UacCreateUserResponse] = Await.result(createUserAsync(body), Duration.Inf)

  def deleteUserAsync(body: UacDeleteUser)(implicit ec: ExecutionContext): Future[Try[UacDeleteUserResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacDeleteUser, UacDeleteUserResponse]("POST", basePath + s"/uac/deleteUser", __query, body, UacDeleteUserResponse.fromJson)
  }

  def deleteUser(body: UacDeleteUser)(implicit ec: ExecutionContext): Try[UacDeleteUserResponse] = Await.result(deleteUserAsync(body), Duration.Inf)

  def getCurrentUserAsync()(implicit ec: ExecutionContext): Future[Try[UacUserInfo]] = {
    val __query = Map[String,String](
    )
    val body: String = null
    return client.request[String, UacUserInfo]("GET", basePath + s"/uac/getCurrentUser", __query, body, UacUserInfo.fromJson)
  }

  def getCurrentUser()(implicit ec: ExecutionContext): Try[UacUserInfo] = Await.result(getCurrentUserAsync(), Duration.Inf)

  def getUserAsync(user_id: String, email: String, username: String)(implicit ec: ExecutionContext): Future[Try[UacUserInfo]] = {
    val __query = Map[String,String](
      "user_id" -> client.toQuery(user_id),
      "email" -> client.toQuery(email),
      "username" -> client.toQuery(username)
    )
    val body: String = null
    return client.request[String, UacUserInfo]("GET", basePath + s"/uac/getUser", __query, body, UacUserInfo.fromJson)
  }

  def getUser(user_id: String, email: String, username: String)(implicit ec: ExecutionContext): Try[UacUserInfo] = Await.result(getUserAsync(user_id, email, username), Duration.Inf)

  def getUsersAsync(body: UacGetUsers)(implicit ec: ExecutionContext): Future[Try[UacGetUsersResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacGetUsers, UacGetUsersResponse]("POST", basePath + s"/uac/getUsers", __query, body, UacGetUsersResponse.fromJson)
  }

  def getUsers(body: UacGetUsers)(implicit ec: ExecutionContext): Try[UacGetUsersResponse] = Await.result(getUsersAsync(body), Duration.Inf)

  def updateUserAsync(body: UacUpdateUser)(implicit ec: ExecutionContext): Future[Try[UacUpdateUserResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacUpdateUser, UacUpdateUserResponse]("POST", basePath + s"/uac/updateUser", __query, body, UacUpdateUserResponse.fromJson)
  }

  def updateUser(body: UacUpdateUser)(implicit ec: ExecutionContext): Try[UacUpdateUserResponse] = Await.result(updateUserAsync(body), Duration.Inf)

}
