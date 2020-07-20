// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.metadata.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.metadata.model._

class MetadataServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def MetadataService_AddLabelsAsync(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddLabelsRequest, MetadataAddLabelsRequestResponse]("PUT", basePath + s"/metadata/labels", __query.toMap, body, MetadataAddLabelsRequestResponse.fromJson)
  }

  def MetadataService_AddLabels(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataAddLabelsRequestResponse] = Await.result(MetadataService_AddLabelsAsync(body), Duration.Inf)

  def MetadataService_AddPropertyAsync(body: MetadataAddPropertyRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddPropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddPropertyRequest, MetadataAddPropertyRequestResponse]("PUT", basePath + s"/metadata/property", __query.toMap, body, MetadataAddPropertyRequestResponse.fromJson)
  }

  def MetadataService_AddProperty(body: MetadataAddPropertyRequest)(implicit ec: ExecutionContext): Try[MetadataAddPropertyRequestResponse] = Await.result(MetadataService_AddPropertyAsync(body), Duration.Inf)

  def MetadataService_DeleteLabelsAsync(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataDeleteLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataDeleteLabelsRequest, MetadataDeleteLabelsRequestResponse]("DELETE", basePath + s"/metadata/labels", __query.toMap, body, MetadataDeleteLabelsRequestResponse.fromJson)
  }

  def MetadataService_DeleteLabels(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataDeleteLabelsRequestResponse] = Await.result(MetadataService_DeleteLabelsAsync(body), Duration.Inf)

  def MetadataService_DeletePropertyAsync(body: MetadataDeletePropertyRequest)(implicit ec: ExecutionContext): Future[Try[MetadataDeletePropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataDeletePropertyRequest, MetadataDeletePropertyRequestResponse]("DELETE", basePath + s"/metadata/property", __query.toMap, body, MetadataDeletePropertyRequestResponse.fromJson)
  }

  def MetadataService_DeleteProperty(body: MetadataDeletePropertyRequest)(implicit ec: ExecutionContext): Try[MetadataDeletePropertyRequestResponse] = Await.result(MetadataService_DeletePropertyAsync(body), Duration.Inf)

  def MetadataService_GetLabelIdsAsync(labels: Option[List[String]]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetLabelIdsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (labels.isDefined) __query.update("labels", client.toQuery(labels.get))
    val body: String = null
    return client.request[String, MetadataGetLabelIdsRequestResponse]("GET", basePath + s"/metadata/getLabelIds", __query.toMap, body, MetadataGetLabelIdsRequestResponse.fromJson)
  }

  def MetadataService_GetLabelIds(labels: Option[List[String]]=None)(implicit ec: ExecutionContext): Try[MetadataGetLabelIdsRequestResponse] = Await.result(MetadataService_GetLabelIdsAsync(labels), Duration.Inf)

  def MetadataService_GetLabelsAsync(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_id_type.isDefined) __query.update("id.id_type", client.toQuery(id_id_type.get))
    if (id_int_id.isDefined) __query.update("id.int_id", client.toQuery(id_int_id.get))
    if (id_string_id.isDefined) __query.update("id.string_id", client.toQuery(id_string_id.get))
    val body: String = null
    return client.request[String, MetadataGetLabelsRequestResponse]("GET", basePath + s"/metadata/labels", __query.toMap, body, MetadataGetLabelsRequestResponse.fromJson)
  }

  def MetadataService_GetLabels(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None)(implicit ec: ExecutionContext): Try[MetadataGetLabelsRequestResponse] = Await.result(MetadataService_GetLabelsAsync(id_id_type, id_int_id, id_string_id), Duration.Inf)

  def MetadataService_GetPropertyAsync(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetPropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_id_type.isDefined) __query.update("id.id_type", client.toQuery(id_id_type.get))
    if (id_int_id.isDefined) __query.update("id.int_id", client.toQuery(id_int_id.get))
    if (id_string_id.isDefined) __query.update("id.string_id", client.toQuery(id_string_id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, MetadataGetPropertyRequestResponse]("GET", basePath + s"/metadata/property", __query.toMap, body, MetadataGetPropertyRequestResponse.fromJson)
  }

  def MetadataService_GetProperty(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[MetadataGetPropertyRequestResponse] = Await.result(MetadataService_GetPropertyAsync(id_id_type, id_int_id, id_string_id, key), Duration.Inf)

  def MetadataService_UpdateLabelsAsync(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddLabelsRequest, MetadataAddLabelsRequestResponse]("POST", basePath + s"/metadata/labels", __query.toMap, body, MetadataAddLabelsRequestResponse.fromJson)
  }

  def MetadataService_UpdateLabels(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataAddLabelsRequestResponse] = Await.result(MetadataService_UpdateLabelsAsync(body), Duration.Inf)

}
