// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class CommentApi(client: HttpClient, val basePath: String = "/v1") {
  def CommentService_addExperimentRunCommentAsync(body: ModeldbAddComment)(implicit ec: ExecutionContext): Future[Try[ModeldbAddCommentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddComment, ModeldbAddCommentResponse]("POST", basePath + s"/comment/addExperimentRunComment", __query.toMap, body, ModeldbAddCommentResponse.fromJson)
  }

  def CommentService_addExperimentRunComment(body: ModeldbAddComment)(implicit ec: ExecutionContext): Try[ModeldbAddCommentResponse] = Await.result(CommentService_addExperimentRunCommentAsync(body), Duration.Inf)

  def CommentService_deleteExperimentRunCommentAsync(entity_id: Option[String]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteCommentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    val body: String = null
    return client.request[String, ModeldbDeleteCommentResponse]("DELETE", basePath + s"/comment/deleteExperimentRunComment", __query.toMap, body, ModeldbDeleteCommentResponse.fromJson)
  }

  def CommentService_deleteExperimentRunComment(entity_id: Option[String]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbDeleteCommentResponse] = Await.result(CommentService_deleteExperimentRunCommentAsync(entity_id, id), Duration.Inf)

  def CommentService_getExperimentRunCommentsAsync(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetCommentsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (entity_id.isDefined) __query.update("entity_id", client.toQuery(entity_id.get))
    val body: String = null
    return client.request[String, ModeldbGetCommentsResponse]("GET", basePath + s"/comment/getExperimentRunComments", __query.toMap, body, ModeldbGetCommentsResponse.fromJson)
  }

  def CommentService_getExperimentRunComments(entity_id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetCommentsResponse] = Await.result(CommentService_getExperimentRunCommentsAsync(entity_id), Duration.Inf)

  def CommentService_updateExperimentRunCommentAsync(body: ModeldbUpdateComment)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateCommentResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateComment, ModeldbUpdateCommentResponse]("POST", basePath + s"/comment/updateExperimentRunComment", __query.toMap, body, ModeldbUpdateCommentResponse.fromJson)
  }

  def CommentService_updateExperimentRunComment(body: ModeldbUpdateComment)(implicit ec: ExecutionContext): Try[ModeldbUpdateCommentResponse] = Await.result(CommentService_updateExperimentRunCommentAsync(body), Duration.Inf)

}
