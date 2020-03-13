// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.metadata.model._

class MetadataServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def AddLabelsAsync(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddLabelsRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddLabelsRequest, MetadataAddLabelsRequestResponse]("PUT", basePath + s"/metadata/labels", __query, body, MetadataAddLabelsRequestResponse.fromJson)
  }

  def AddLabels(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataAddLabelsRequestResponse] = Await.result(AddLabelsAsync(body), Duration.Inf)

  def DeleteLabelsAsync(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataDeleteLabelsRequestResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataDeleteLabelsRequest, MetadataDeleteLabelsRequestResponse]("DELETE", basePath + s"/metadata/labels", __query, body, MetadataDeleteLabelsRequestResponse.fromJson)
  }

  def DeleteLabels(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataDeleteLabelsRequestResponse] = Await.result(DeleteLabelsAsync(body), Duration.Inf)

  def GetLabelsAsync(id_id_type: String, id_int_id: String, id_string_id: String)(implicit ec: ExecutionContext): Future[Try[MetadataGetLabelsRequestResponse]] = {
    val __query = Map[String,String](
      "id.id_type" -> client.toQuery(id_id_type),
      "id.int_id" -> client.toQuery(id_int_id),
      "id.string_id" -> client.toQuery(id_string_id)
    )
    val body: String = null
    return client.request[String, MetadataGetLabelsRequestResponse]("GET", basePath + s"/metadata/labels", __query, body, MetadataGetLabelsRequestResponse.fromJson)
  }

  def GetLabels(id_id_type: String, id_int_id: String, id_string_id: String)(implicit ec: ExecutionContext): Try[MetadataGetLabelsRequestResponse] = Await.result(GetLabelsAsync(id_id_type, id_int_id, id_string_id), Duration.Inf)

}
