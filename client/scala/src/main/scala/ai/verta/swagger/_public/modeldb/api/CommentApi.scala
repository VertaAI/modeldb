// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class CommentApi(client: HttpClient, val basePath: String = "/v1") {
  def addExperimentRunCommentAsync(body: ModeldbAddComment)(implicit ec: ExecutionContext): Future[Try[ModeldbAddCommentResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddComment, ModeldbAddCommentResponse]("POST", basePath + s"/comment/addExperimentRunComment", __query, body, ModeldbAddCommentResponse.fromJson)
  }

  def addExperimentRunComment(body: ModeldbAddComment)(implicit ec: ExecutionContext): Try[ModeldbAddCommentResponse] = Await.result(addExperimentRunCommentAsync(body), Duration.Inf)

  def deleteExperimentRunCommentAsync(id: String, entity_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteCommentResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "entity_id" -> client.toQuery(entity_id)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteCommentResponse]("DELETE", basePath + s"/comment/deleteExperimentRunComment", __query, body, ModeldbDeleteCommentResponse.fromJson)
  }

  def deleteExperimentRunComment(id: String, entity_id: String)(implicit ec: ExecutionContext): Try[ModeldbDeleteCommentResponse] = Await.result(deleteExperimentRunCommentAsync(id, entity_id), Duration.Inf)

  def getExperimentRunCommentsAsync(entity_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetCommentsResponse]] = {
    val __query = Map[String,String](
      "entity_id" -> client.toQuery(entity_id)
    )
    val body: String = null
    return client.request[String, ModeldbGetCommentsResponse]("GET", basePath + s"/comment/getExperimentRunComments", __query, body, ModeldbGetCommentsResponse.fromJson)
  }

  def getExperimentRunComments(entity_id: String)(implicit ec: ExecutionContext): Try[ModeldbGetCommentsResponse] = Await.result(getExperimentRunCommentsAsync(entity_id), Duration.Inf)

  def updateExperimentRunCommentAsync(body: ModeldbUpdateComment)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateCommentResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateComment, ModeldbUpdateCommentResponse]("POST", basePath + s"/comment/updateExperimentRunComment", __query, body, ModeldbUpdateCommentResponse.fromJson)
  }

  def updateExperimentRunComment(body: ModeldbUpdateComment)(implicit ec: ExecutionContext): Try[ModeldbUpdateCommentResponse] = Await.result(updateExperimentRunCommentAsync(body), Duration.Inf)

}
