// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.artifactstore.model._

class ArtifactStoreApi(client: HttpClient, val basePath: String = "/v1") {
  def ArtifactStore_deleteArtifactAsync(body: ArtifactstoreDeleteArtifact)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreDeleteArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreDeleteArtifact, ArtifactstoreDeleteArtifactResponse]("POST", basePath + s"/artifact/deleteArtifact", __query.toMap, body, ArtifactstoreDeleteArtifactResponse.fromJson)
  }

  def ArtifactStore_deleteArtifact(body: ArtifactstoreDeleteArtifact)(implicit ec: ExecutionContext): Try[ArtifactstoreDeleteArtifactResponse] = Await.result(ArtifactStore_deleteArtifactAsync(body), Duration.Inf)

  def ArtifactStore_getArtifactAsync(key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreGetArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    val body: String = null
    return client.request[String, ArtifactstoreGetArtifactResponse]("GET", basePath + s"/artifact/getArtifact", __query.toMap, body, ArtifactstoreGetArtifactResponse.fromJson)
  }

  def ArtifactStore_getArtifact(key: Option[String]=None)(implicit ec: ExecutionContext): Try[ArtifactstoreGetArtifactResponse] = Await.result(ArtifactStore_getArtifactAsync(key), Duration.Inf)

  def ArtifactStore_storeArtifactAsync(body: ArtifactstoreStoreArtifact)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreStoreArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreStoreArtifact, ArtifactstoreStoreArtifactResponse]("POST", basePath + s"/artifact/storeArtifact", __query.toMap, body, ArtifactstoreStoreArtifactResponse.fromJson)
  }

  def ArtifactStore_storeArtifact(body: ArtifactstoreStoreArtifact)(implicit ec: ExecutionContext): Try[ArtifactstoreStoreArtifactResponse] = Await.result(ArtifactStore_storeArtifactAsync(body), Duration.Inf)

  def ArtifactStore_storeArtifactWithStreamAsync(body: ArtifactstoreStoreArtifactWithStream)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreStoreArtifactWithStreamResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreStoreArtifactWithStream, ArtifactstoreStoreArtifactWithStreamResponse]("POST", basePath + s"/artifact/storeArtifactWithStream", __query.toMap, body, ArtifactstoreStoreArtifactWithStreamResponse.fromJson)
  }

  def ArtifactStore_storeArtifactWithStream(body: ArtifactstoreStoreArtifactWithStream)(implicit ec: ExecutionContext): Try[ArtifactstoreStoreArtifactWithStreamResponse] = Await.result(ArtifactStore_storeArtifactWithStreamAsync(body), Duration.Inf)

}
