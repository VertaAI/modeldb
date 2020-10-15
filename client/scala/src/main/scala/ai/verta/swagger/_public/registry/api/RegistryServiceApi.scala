// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.registry.model._

class RegistryServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def RegistryService_CreateModelVersionAsync(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("POST", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_CreateModelVersion(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_CreateModelVersionAsync(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name), Duration.Inf)

  def RegistryService_CreateModelVersion2Async(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("POST", basePath + s"/registry/registered_models/$id_model_id_registered_model_id/model_versions", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_CreateModelVersion2(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_CreateModelVersion2Async(body, id_model_id_registered_model_id), Duration.Inf)

  def RegistryService_CreateRegisteredModelAsync(body: RegistryRegisteredModel, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("POST", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_CreateRegisteredModel(body: RegistryRegisteredModel, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_CreateRegisteredModelAsync(body, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_DeleteModelVersionAsync(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteModelVersionRequestResponse]("DELETE", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions/$id_model_version_id", __query.toMap, body, RegistryDeleteModelVersionRequestResponse.fromJson)
  }

  def RegistryService_DeleteModelVersion(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteModelVersionRequestResponse] = Await.result(RegistryService_DeleteModelVersionAsync(id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id, id_model_id_registered_model_id), Duration.Inf)

  def RegistryService_DeleteModelVersion2Async(id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_name.isDefined) __query.update("id.model_id.named_id.name", client.toQuery(id_model_id_named_id_name.get))
    if (id_model_id_named_id_workspace_name.isDefined) __query.update("id.model_id.named_id.workspace_name", client.toQuery(id_model_id_named_id_workspace_name.get))
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteModelVersionRequestResponse]("DELETE", basePath + s"/registry/registered_models/$id_model_id_registered_model_id/model_versions/$id_model_version_id", __query.toMap, body, RegistryDeleteModelVersionRequestResponse.fromJson)
  }

  def RegistryService_DeleteModelVersion2(id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteModelVersionRequestResponse] = Await.result(RegistryService_DeleteModelVersion2Async(id_model_id_registered_model_id, id_model_version_id, id_model_id_named_id_name, id_model_id_named_id_workspace_name), Duration.Inf)

  def RegistryService_DeleteModelVersion3Async(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_name.isDefined) __query.update("id.model_id.named_id.name", client.toQuery(id_model_id_named_id_name.get))
    if (id_model_id_named_id_workspace_name.isDefined) __query.update("id.model_id.named_id.workspace_name", client.toQuery(id_model_id_named_id_workspace_name.get))
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteModelVersionRequestResponse]("DELETE", basePath + s"/registry/model_versions/$id_model_version_id", __query.toMap, body, RegistryDeleteModelVersionRequestResponse.fromJson)
  }

  def RegistryService_DeleteModelVersion3(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteModelVersionRequestResponse] = Await.result(RegistryService_DeleteModelVersion3Async(id_model_version_id, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_id_registered_model_id), Duration.Inf)

  def RegistryService_DeleteRegisteredModelAsync(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id.isDefined) __query.update("id.registered_model_id", client.toQuery(id_registered_model_id.get))
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, RegistryDeleteRegisteredModelRequestResponse]("DELETE", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistryDeleteRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_DeleteRegisteredModel(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteRegisteredModelRequestResponse] = Await.result(RegistryService_DeleteRegisteredModelAsync(id_named_id_name, id_named_id_workspace_name, id_registered_model_id), Duration.Inf)

  def RegistryService_DeleteRegisteredModel2Async(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_name.isDefined) __query.update("id.named_id.name", client.toQuery(id_named_id_name.get))
    if (id_named_id_workspace_name.isDefined) __query.update("id.named_id.workspace_name", client.toQuery(id_named_id_workspace_name.get))
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteRegisteredModelRequestResponse]("DELETE", basePath + s"/registry/registered_models/$id_registered_model_id", __query.toMap, body, RegistryDeleteRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_DeleteRegisteredModel2(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteRegisteredModelRequestResponse] = Await.result(RegistryService_DeleteRegisteredModel2Async(id_registered_model_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_FindModelVersionAsync(body: RegistryFindModelVersionRequest, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name/model_versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def RegistryService_FindModelVersion(body: RegistryFindModelVersionRequest, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(RegistryService_FindModelVersionAsync(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_FindModelVersion2Async(body: RegistryFindModelVersionRequest, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/registered_models/$id_registered_model_id/model_versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def RegistryService_FindModelVersion2(body: RegistryFindModelVersionRequest, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(RegistryService_FindModelVersion2Async(body, id_registered_model_id), Duration.Inf)

  def RegistryService_FindModelVersion3Async(body: RegistryFindModelVersionRequest, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/workspaces/$id_named_id_workspace_name/model_versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def RegistryService_FindModelVersion3(body: RegistryFindModelVersionRequest, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(RegistryService_FindModelVersion3Async(body, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_FindModelVersion4Async(body: RegistryFindModelVersionRequest)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/model_versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def RegistryService_FindModelVersion4(body: RegistryFindModelVersionRequest)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(RegistryService_FindModelVersion4Async(body), Duration.Inf)

  def RegistryService_FindRegisteredModelAsync(body: RegistryFindRegisteredModelRequest, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistryFindRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindRegisteredModelRequest, RegistryFindRegisteredModelRequestResponse]("POST", basePath + s"/registry/workspaces/$workspace_name/registered_models/find", __query.toMap, body, RegistryFindRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_FindRegisteredModel(body: RegistryFindRegisteredModelRequest, workspace_name: String)(implicit ec: ExecutionContext): Try[RegistryFindRegisteredModelRequestResponse] = Await.result(RegistryService_FindRegisteredModelAsync(body, workspace_name), Duration.Inf)

  def RegistryService_FindRegisteredModel2Async(body: RegistryFindRegisteredModelRequest)(implicit ec: ExecutionContext): Future[Try[RegistryFindRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindRegisteredModelRequest, RegistryFindRegisteredModelRequestResponse]("POST", basePath + s"/registry/registered_models/find", __query.toMap, body, RegistryFindRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_FindRegisteredModel2(body: RegistryFindRegisteredModelRequest)(implicit ec: ExecutionContext): Try[RegistryFindRegisteredModelRequestResponse] = Await.result(RegistryService_FindRegisteredModel2Async(body), Duration.Inf)

  def RegistryService_GetModelVersionAsync(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetModelVersionRequestResponse]("GET", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions/$id_model_version_id", __query.toMap, body, RegistryGetModelVersionRequestResponse.fromJson)
  }

  def RegistryService_GetModelVersion(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetModelVersionRequestResponse] = Await.result(RegistryService_GetModelVersionAsync(id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id, id_model_id_registered_model_id), Duration.Inf)

  def RegistryService_GetModelVersion2Async(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_name.isDefined) __query.update("id.model_id.named_id.name", client.toQuery(id_model_id_named_id_name.get))
    if (id_model_id_named_id_workspace_name.isDefined) __query.update("id.model_id.named_id.workspace_name", client.toQuery(id_model_id_named_id_workspace_name.get))
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetModelVersionRequestResponse]("GET", basePath + s"/registry/model_versions/$id_model_version_id", __query.toMap, body, RegistryGetModelVersionRequestResponse.fromJson)
  }

  def RegistryService_GetModelVersion2(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetModelVersionRequestResponse] = Await.result(RegistryService_GetModelVersion2Async(id_model_version_id, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_id_registered_model_id), Duration.Inf)

  def RegistryService_GetRegisteredModelAsync(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id.isDefined) __query.update("id.registered_model_id", client.toQuery(id_registered_model_id.get))
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, RegistryGetRegisteredModelRequestResponse]("GET", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistryGetRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_GetRegisteredModel(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetRegisteredModelRequestResponse] = Await.result(RegistryService_GetRegisteredModelAsync(id_named_id_name, id_named_id_workspace_name, id_registered_model_id), Duration.Inf)

  def RegistryService_GetRegisteredModel2Async(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_name.isDefined) __query.update("id.named_id.name", client.toQuery(id_named_id_name.get))
    if (id_named_id_workspace_name.isDefined) __query.update("id.named_id.workspace_name", client.toQuery(id_named_id_workspace_name.get))
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    val body: String = null
    return client.request[String, RegistryGetRegisteredModelRequestResponse]("GET", basePath + s"/registry/registered_models/$id_registered_model_id", __query.toMap, body, RegistryGetRegisteredModelRequestResponse.fromJson)
  }

  def RegistryService_GetRegisteredModel2(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryGetRegisteredModelRequestResponse] = Await.result(RegistryService_GetRegisteredModel2Async(id_registered_model_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_UpdateModelVersionAsync(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PATCH", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersionAsync(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateModelVersion2Async(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PATCH", basePath + s"/registry/registered_models/$id_model_id_registered_model_id/model_versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion2(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersion2Async(body, id_model_id_registered_model_id, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateModelVersion3Async(body: RegistrySetModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistrySetModelVersion, RegistrySetModelVersionResponse]("PATCH", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions/$id_model_version_id/full_body", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion3(body: RegistrySetModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersion3Async(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateModelVersion4Async(body: RegistrySetModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistrySetModelVersion, RegistrySetModelVersionResponse]("PATCH", basePath + s"/registry/registered_models/$id_model_id_registered_model_id/model_versions/$id_model_version_id/full_body", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion4(body: RegistrySetModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersion4Async(body, id_model_id_registered_model_id, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateModelVersion5Async(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PUT", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/model_versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion5(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersion5Async(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateModelVersion6Async(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PUT", basePath + s"/registry/registered_models/$id_model_id_registered_model_id/model_versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def RegistryService_UpdateModelVersion6(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(RegistryService_UpdateModelVersion6Async(body, id_model_id_registered_model_id, id_model_version_id), Duration.Inf)

  def RegistryService_UpdateRegisteredModelAsync(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PATCH", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModelAsync(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_UpdateRegisteredModel2Async(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PATCH", basePath + s"/registry/registered_models/$id_registered_model_id", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel2(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModel2Async(body, id_registered_model_id), Duration.Inf)

  def RegistryService_UpdateRegisteredModel3Async(body: RegistrySetRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistrySetRegisteredModel, RegistrySetRegisteredModelResponse]("PATCH", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name/full_body", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel3(body: RegistrySetRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModel3Async(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_UpdateRegisteredModel4Async(body: RegistrySetRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistrySetRegisteredModel, RegistrySetRegisteredModelResponse]("PATCH", basePath + s"/registry/registered_models/$id_registered_model_id/full_body", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel4(body: RegistrySetRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModel4Async(body, id_registered_model_id), Duration.Inf)

  def RegistryService_UpdateRegisteredModel5Async(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PUT", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel5(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModel5Async(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def RegistryService_UpdateRegisteredModel6Async(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PUT", basePath + s"/registry/registered_models/$id_registered_model_id", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def RegistryService_UpdateRegisteredModel6(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(RegistryService_UpdateRegisteredModel6Async(body, id_registered_model_id), Duration.Inf)

  def RegistryService_commitArtifactPartAsync(body: RegistryCommitArtifactPart, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryCommitArtifactPartResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryCommitArtifactPart, RegistryCommitArtifactPartResponse]("POST", basePath + s"/registry/model_versions/$model_version_id/commitArtifactPart", __query.toMap, body, RegistryCommitArtifactPartResponse.fromJson)
  }

  def RegistryService_commitArtifactPart(body: RegistryCommitArtifactPart, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryCommitArtifactPartResponse] = Await.result(RegistryService_commitArtifactPartAsync(body, model_version_id), Duration.Inf)

  def RegistryService_commitMultipartArtifactAsync(body: RegistryCommitMultipartArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryCommitMultipartArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryCommitMultipartArtifact, RegistryCommitMultipartArtifactResponse]("POST", basePath + s"/registry/model_versions/$model_version_id/commitMultipartArtifact", __query.toMap, body, RegistryCommitMultipartArtifactResponse.fromJson)
  }

  def RegistryService_commitMultipartArtifact(body: RegistryCommitMultipartArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryCommitMultipartArtifactResponse] = Await.result(RegistryService_commitMultipartArtifactAsync(body, model_version_id), Duration.Inf)

  def RegistryService_getCommittedArtifactPartsAsync(model_version_id: BigInt, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetCommittedArtifactPartsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetCommittedArtifactPartsResponse]("GET", basePath + s"/registry/model_versions/$model_version_id/getCommittedArtifactParts", __query.toMap, body, RegistryGetCommittedArtifactPartsResponse.fromJson)
  }

  def RegistryService_getCommittedArtifactParts(model_version_id: BigInt, key: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryGetCommittedArtifactPartsResponse] = Await.result(RegistryService_getCommittedArtifactPartsAsync(model_version_id, key), Duration.Inf)

  def RegistryService_getUrlForArtifactAsync(body: RegistryGetUrlForArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryGetUrlForArtifact, RegistryGetUrlForArtifactResponse]("POST", basePath + s"/registry/model_versions/$model_version_id/getUrlForArtifact", __query.toMap, body, RegistryGetUrlForArtifactResponse.fromJson)
  }

  def RegistryService_getUrlForArtifact(body: RegistryGetUrlForArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryGetUrlForArtifactResponse] = Await.result(RegistryService_getUrlForArtifactAsync(body, model_version_id), Duration.Inf)

}
