// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class LineageApi(client: HttpClient, val basePath: String = "/v1") {
  def addLineageAsync(body: ModeldbAddLineage)(implicit ec: ExecutionContext): Future[Try[ModeldbAddLineageResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddLineage, ModeldbAddLineageResponse]("POST", basePath + s"/lineage/addLineage", __query.toMap, body, ModeldbAddLineageResponse.fromJson)
  }

  def addLineage(body: ModeldbAddLineage)(implicit ec: ExecutionContext): Try[ModeldbAddLineageResponse] = Await.result(addLineageAsync(body), Duration.Inf)

  def deleteLineageAsync(body: ModeldbDeleteLineage)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteLineageResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteLineage, ModeldbDeleteLineageResponse]("POST", basePath + s"/lineage/deleteLineage", __query.toMap, body, ModeldbDeleteLineageResponse.fromJson)
  }

  def deleteLineage(body: ModeldbDeleteLineage)(implicit ec: ExecutionContext): Try[ModeldbDeleteLineageResponse] = Await.result(deleteLineageAsync(body), Duration.Inf)

  def findAllInputsAsync(body: ModeldbFindAllInputs)(implicit ec: ExecutionContext): Future[Try[ModeldbFindAllInputsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindAllInputs, ModeldbFindAllInputsResponse]("POST", basePath + s"/lineage/findAllInputs", __query.toMap, body, ModeldbFindAllInputsResponse.fromJson)
  }

  def findAllInputs(body: ModeldbFindAllInputs)(implicit ec: ExecutionContext): Try[ModeldbFindAllInputsResponse] = Await.result(findAllInputsAsync(body), Duration.Inf)

  def findAllInputsOutputsAsync(body: ModeldbFindAllInputsOutputs)(implicit ec: ExecutionContext): Future[Try[ModeldbFindAllInputsOutputsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindAllInputsOutputs, ModeldbFindAllInputsOutputsResponse]("POST", basePath + s"/lineage/findAllInputsOutputs", __query.toMap, body, ModeldbFindAllInputsOutputsResponse.fromJson)
  }

  def findAllInputsOutputs(body: ModeldbFindAllInputsOutputs)(implicit ec: ExecutionContext): Try[ModeldbFindAllInputsOutputsResponse] = Await.result(findAllInputsOutputsAsync(body), Duration.Inf)

  def findAllOutputsAsync(body: ModeldbFindAllOutputs)(implicit ec: ExecutionContext): Future[Try[ModeldbFindAllOutputsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindAllOutputs, ModeldbFindAllOutputsResponse]("POST", basePath + s"/lineage/findAllOutputs", __query.toMap, body, ModeldbFindAllOutputsResponse.fromJson)
  }

  def findAllOutputs(body: ModeldbFindAllOutputs)(implicit ec: ExecutionContext): Try[ModeldbFindAllOutputsResponse] = Await.result(findAllOutputsAsync(body), Duration.Inf)

}
