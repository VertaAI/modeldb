// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.artifactstore.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.artifactstore.model._

class ArtifactStoreApi(client: HttpClient, val basePath: String = "/v1") {
  def deleteArtifactAsync(body: ArtifactstoreDeleteArtifact)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreDeleteArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreDeleteArtifact, ArtifactstoreDeleteArtifactResponse]("POST", basePath + s"/artifact/deleteArtifact", __query, body, ArtifactstoreDeleteArtifactResponse.fromJson)
  }

  def deleteArtifact(body: ArtifactstoreDeleteArtifact)(implicit ec: ExecutionContext): Try[ArtifactstoreDeleteArtifactResponse] = Await.result(deleteArtifactAsync(body), Duration.Inf)

  def getArtifactAsync(key: String)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreGetArtifactResponse]] = {
    val __query = Map[String,String](
      "key" -> client.toQuery(key)
    )
    val body: String = null
    return client.request[String, ArtifactstoreGetArtifactResponse]("GET", basePath + s"/artifact/getArtifact", __query, body, ArtifactstoreGetArtifactResponse.fromJson)
  }

  def getArtifact(key: String)(implicit ec: ExecutionContext): Try[ArtifactstoreGetArtifactResponse] = Await.result(getArtifactAsync(key), Duration.Inf)

  def storeArtifactAsync(body: ArtifactstoreStoreArtifact)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreStoreArtifactResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreStoreArtifact, ArtifactstoreStoreArtifactResponse]("POST", basePath + s"/artifact/storeArtifact", __query, body, ArtifactstoreStoreArtifactResponse.fromJson)
  }

  def storeArtifact(body: ArtifactstoreStoreArtifact)(implicit ec: ExecutionContext): Try[ArtifactstoreStoreArtifactResponse] = Await.result(storeArtifactAsync(body), Duration.Inf)

  def storeArtifactWithStreamAsync(body: ArtifactstoreStoreArtifactWithStream)(implicit ec: ExecutionContext): Future[Try[ArtifactstoreStoreArtifactWithStreamResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ArtifactstoreStoreArtifactWithStream, ArtifactstoreStoreArtifactWithStreamResponse]("POST", basePath + s"/artifact/storeArtifactWithStream", __query, body, ArtifactstoreStoreArtifactWithStreamResponse.fromJson)
  }

  def storeArtifactWithStream(body: ArtifactstoreStoreArtifactWithStream)(implicit ec: ExecutionContext): Try[ArtifactstoreStoreArtifactWithStreamResponse] = Await.result(storeArtifactWithStreamAsync(body), Duration.Inf)

}
