// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class JobApi(client: HttpClient, val basePath: String = "/v1") {
  def createJobAsync(body: ModeldbCreateJob)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateJobResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateJob, ModeldbCreateJobResponse]("POST", basePath + s"/job/createJob", __query, body, ModeldbCreateJobResponse.fromJson)
  }

  def createJob(body: ModeldbCreateJob)(implicit ec: ExecutionContext): Try[ModeldbCreateJobResponse] = Await.result(createJobAsync(body), Duration.Inf)

  def deleteJobAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteJobResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteJobResponse]("GET", basePath + s"/job/deleteJob", __query, body, ModeldbDeleteJobResponse.fromJson)
  }

  def deleteJob(id: String)(implicit ec: ExecutionContext): Try[ModeldbDeleteJobResponse] = Await.result(deleteJobAsync(id), Duration.Inf)

  def getJobAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetJobResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetJobResponse]("GET", basePath + s"/job/getJob", __query, body, ModeldbGetJobResponse.fromJson)
  }

  def getJob(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetJobResponse] = Await.result(getJobAsync(id), Duration.Inf)

  def updateJobAsync(id: String, end_time: String, job_status: String)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateJobResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "end_time" -> client.toQuery(end_time),
      "job_status" -> client.toQuery(job_status)
    )
    val body: String = null
    return client.request[String, ModeldbUpdateJobResponse]("GET", basePath + s"/job/updateJob", __query, body, ModeldbUpdateJobResponse.fromJson)
  }

  def updateJob(id: String, end_time: String, job_status: String)(implicit ec: ExecutionContext): Try[ModeldbUpdateJobResponse] = Await.result(updateJobAsync(id, end_time, job_status), Duration.Inf)

}
