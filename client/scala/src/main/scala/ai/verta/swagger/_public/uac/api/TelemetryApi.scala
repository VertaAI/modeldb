// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.model._

class TelemetryApi(client: HttpClient, val basePath: String = "/v1") {
  def collectTelemetryAsync(body: UacCollectTelemetry)(implicit ec: ExecutionContext): Future[Try[UacCollectTelemetryResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[UacCollectTelemetry, UacCollectTelemetryResponse]("POST", basePath + s"/telemetry/collectTelemetry", __query, body, UacCollectTelemetryResponse.fromJson)
  }

  def collectTelemetry(body: UacCollectTelemetry)(implicit ec: ExecutionContext): Try[UacCollectTelemetryResponse] = Await.result(collectTelemetryAsync(body), Duration.Inf)

}
