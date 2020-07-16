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
  def AddLabelsAsync(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddLabelsRequest, MetadataAddLabelsRequestResponse]("PUT", basePath + s"/metadata/labels", __query.toMap, body, MetadataAddLabelsRequestResponse.fromJson)
  }

  def AddLabels(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataAddLabelsRequestResponse] = Await.result(AddLabelsAsync(body), Duration.Inf)

  def AddPropertyAsync(body: MetadataAddPropertyRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddPropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddPropertyRequest, MetadataAddPropertyRequestResponse]("PUT", basePath + s"/metadata/property", __query.toMap, body, MetadataAddPropertyRequestResponse.fromJson)
  }

  def AddProperty(body: MetadataAddPropertyRequest)(implicit ec: ExecutionContext): Try[MetadataAddPropertyRequestResponse] = Await.result(AddPropertyAsync(body), Duration.Inf)

  def DeleteLabelsAsync(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataDeleteLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataDeleteLabelsRequest, MetadataDeleteLabelsRequestResponse]("DELETE", basePath + s"/metadata/labels", __query.toMap, body, MetadataDeleteLabelsRequestResponse.fromJson)
  }

  def DeleteLabels(body: MetadataDeleteLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataDeleteLabelsRequestResponse] = Await.result(DeleteLabelsAsync(body), Duration.Inf)

  def DeletePropertyAsync(body: MetadataDeletePropertyRequest)(implicit ec: ExecutionContext): Future[Try[MetadataDeletePropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataDeletePropertyRequest, MetadataDeletePropertyRequestResponse]("DELETE", basePath + s"/metadata/property", __query.toMap, body, MetadataDeletePropertyRequestResponse.fromJson)
  }

  def DeleteProperty(body: MetadataDeletePropertyRequest)(implicit ec: ExecutionContext): Try[MetadataDeletePropertyRequestResponse] = Await.result(DeletePropertyAsync(body), Duration.Inf)

  def GetLabelIdsAsync(labels: Option[List[String]]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetLabelIdsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (labels.isDefined) __query.update("labels", client.toQuery(labels.get))
    val body: String = null
    return client.request[String, MetadataGetLabelIdsRequestResponse]("GET", basePath + s"/metadata/getLabelIds", __query.toMap, body, MetadataGetLabelIdsRequestResponse.fromJson)
  }

  def GetLabelIds(labels: Option[List[String]]=None)(implicit ec: ExecutionContext): Try[MetadataGetLabelIdsRequestResponse] = Await.result(GetLabelIdsAsync(labels), Duration.Inf)

  def GetLabelsAsync(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_id_type.isDefined) __query.update("id.id_type", client.toQuery(id_id_type.get))
    if (id_int_id.isDefined) __query.update("id.int_id", client.toQuery(id_int_id.get))
    if (id_string_id.isDefined) __query.update("id.string_id", client.toQuery(id_string_id.get))
    val body: String = null
    return client.request[String, MetadataGetLabelsRequestResponse]("GET", basePath + s"/metadata/labels", __query.toMap, body, MetadataGetLabelsRequestResponse.fromJson)
  }

  def GetLabels(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None)(implicit ec: ExecutionContext): Try[MetadataGetLabelsRequestResponse] = Await.result(GetLabelsAsync(id_id_type, id_int_id, id_string_id), Duration.Inf)

  def GetPropertyAsync(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[MetadataGetPropertyRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_id_type.isDefined) __query.update("id.id_type", client.toQuery(id_id_type.get))
    if (id_int_id.isDefined) __query.update("id.int_id", client.toQuery(id_int_id.get))
    if (id_string_id.isDefined) __query.update("id.string_id", client.toQuery(id_string_id.get))
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, MetadataGetPropertyRequestResponse]("GET", basePath + s"/metadata/property", __query.toMap, body, MetadataGetPropertyRequestResponse.fromJson)
  }

  def GetProperty(id_id_type: Option[String]=None, id_int_id: Option[BigInt]=None, id_string_id: Option[String]=None, key: Option[String]=None)(implicit ec: ExecutionContext): Try[MetadataGetPropertyRequestResponse] = Await.result(GetPropertyAsync(id_id_type, id_int_id, id_string_id, key), Duration.Inf)

  def UpdateLabelsAsync(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Future[Try[MetadataAddLabelsRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[MetadataAddLabelsRequest, MetadataAddLabelsRequestResponse]("POST", basePath + s"/metadata/labels", __query.toMap, body, MetadataAddLabelsRequestResponse.fromJson)
  }

  def UpdateLabels(body: MetadataAddLabelsRequest)(implicit ec: ExecutionContext): Try[MetadataAddLabelsRequestResponse] = Await.result(UpdateLabelsAsync(body), Duration.Inf)

}
