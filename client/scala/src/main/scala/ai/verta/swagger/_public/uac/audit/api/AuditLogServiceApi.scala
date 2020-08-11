// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.uac.audit.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.uac.audit.model._

class AuditLogServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def AuditLogService_findAuditLogAsync(body: VersioningFindAuditLog)(implicit ec: ExecutionContext): Future[Try[VersioningFindAuditLogResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningFindAuditLog, VersioningFindAuditLogResponse]("POST", basePath + s"/audit-log/findAuditLog", __query.toMap, body, VersioningFindAuditLogResponse.fromJson)
  }

  def AuditLogService_findAuditLog(body: VersioningFindAuditLog)(implicit ec: ExecutionContext): Try[VersioningFindAuditLogResponse] = Await.result(AuditLogService_findAuditLogAsync(body), Duration.Inf)

  def AuditLogService_postAuditLogsAsync(body: VersioningAddAuditLogBatch)(implicit ec: ExecutionContext): Future[Try[VersioningAddAuditLogBatchResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[VersioningAddAuditLogBatch, VersioningAddAuditLogBatchResponse]("POST", basePath + s"/audit-log/postAuditLogs", __query.toMap, body, VersioningAddAuditLogBatchResponse.fromJson)
  }

  def AuditLogService_postAuditLogs(body: VersioningAddAuditLogBatch)(implicit ec: ExecutionContext): Try[VersioningAddAuditLogBatchResponse] = Await.result(AuditLogService_postAuditLogsAsync(body), Duration.Inf)

}
