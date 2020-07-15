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
  def CreateModelVersionAsync(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("POST", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/versions", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def CreateModelVersion(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(CreateModelVersionAsync(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name), Duration.Inf)

  def CreateModelVersion2Async(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("POST", basePath + s"/registry/$id_model_id_registered_model_id/versions", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def CreateModelVersion2(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(CreateModelVersion2Async(body, id_model_id_registered_model_id), Duration.Inf)

  def CreateRegisteredModelAsync(body: RegistryRegisteredModel, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("POST", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def CreateRegisteredModel(body: RegistryRegisteredModel, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(CreateRegisteredModelAsync(body, id_named_id_workspace_name), Duration.Inf)

  def DeleteModelVersionAsync(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteModelVersionRequestResponse]("DELETE", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/versions/$id_model_version_id", __query.toMap, body, RegistryDeleteModelVersionRequestResponse.fromJson)
  }

  def DeleteModelVersion(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteModelVersionRequestResponse] = Await.result(DeleteModelVersionAsync(id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id, id_model_id_registered_model_id), Duration.Inf)

  def DeleteModelVersion2Async(id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_name.isDefined) __query.update("id.model_id.named_id.name", client.toQuery(id_model_id_named_id_name.get))
    if (id_model_id_named_id_workspace_name.isDefined) __query.update("id.model_id.named_id.workspace_name", client.toQuery(id_model_id_named_id_workspace_name.get))
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteModelVersionRequestResponse]("DELETE", basePath + s"/registry/$id_model_id_registered_model_id/versions/$id_model_version_id", __query.toMap, body, RegistryDeleteModelVersionRequestResponse.fromJson)
  }

  def DeleteModelVersion2(id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteModelVersionRequestResponse] = Await.result(DeleteModelVersion2Async(id_model_id_registered_model_id, id_model_version_id, id_model_id_named_id_name, id_model_id_named_id_workspace_name), Duration.Inf)

  def DeleteRegisteredModelAsync(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id.isDefined) __query.update("id.registered_model_id", client.toQuery(id_registered_model_id.get))
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, RegistryDeleteRegisteredModelRequestResponse]("DELETE", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistryDeleteRegisteredModelRequestResponse.fromJson)
  }

  def DeleteRegisteredModel(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteRegisteredModelRequestResponse] = Await.result(DeleteRegisteredModelAsync(id_named_id_name, id_named_id_workspace_name, id_registered_model_id), Duration.Inf)

  def DeleteRegisteredModel2Async(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryDeleteRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_name.isDefined) __query.update("id.named_id.name", client.toQuery(id_named_id_name.get))
    if (id_named_id_workspace_name.isDefined) __query.update("id.named_id.workspace_name", client.toQuery(id_named_id_workspace_name.get))
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    val body: String = null
    return client.request[String, RegistryDeleteRegisteredModelRequestResponse]("DELETE", basePath + s"/registry/$id_registered_model_id", __query.toMap, body, RegistryDeleteRegisteredModelRequestResponse.fromJson)
  }

  def DeleteRegisteredModel2(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryDeleteRegisteredModelRequestResponse] = Await.result(DeleteRegisteredModel2Async(id_registered_model_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def FindModelVersionAsync(body: RegistryFindModelVersionRequest, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name/versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def FindModelVersion(body: RegistryFindModelVersionRequest, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(FindModelVersionAsync(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def FindModelVersion2Async(body: RegistryFindModelVersionRequest, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryFindModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindModelVersionRequest, RegistryFindModelVersionRequestResponse]("POST", basePath + s"/registry/$id_registered_model_id/versions/find", __query.toMap, body, RegistryFindModelVersionRequestResponse.fromJson)
  }

  def FindModelVersion2(body: RegistryFindModelVersionRequest, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryFindModelVersionRequestResponse] = Await.result(FindModelVersion2Async(body, id_registered_model_id), Duration.Inf)

  def FindRegisteredModelAsync(body: RegistryFindRegisteredModelRequest, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistryFindRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindRegisteredModelRequest, RegistryFindRegisteredModelRequestResponse]("POST", basePath + s"/registry/workspaces/$workspace_name/registered_models/find", __query.toMap, body, RegistryFindRegisteredModelRequestResponse.fromJson)
  }

  def FindRegisteredModel(body: RegistryFindRegisteredModelRequest, workspace_name: String)(implicit ec: ExecutionContext): Try[RegistryFindRegisteredModelRequestResponse] = Await.result(FindRegisteredModelAsync(body, workspace_name), Duration.Inf)

  def FindRegisteredModel2Async(body: RegistryFindRegisteredModelRequest)(implicit ec: ExecutionContext): Future[Try[RegistryFindRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryFindRegisteredModelRequest, RegistryFindRegisteredModelRequestResponse]("POST", basePath + s"/registry/registered_models/find", __query.toMap, body, RegistryFindRegisteredModelRequestResponse.fromJson)
  }

  def FindRegisteredModel2(body: RegistryFindRegisteredModelRequest)(implicit ec: ExecutionContext): Try[RegistryFindRegisteredModelRequestResponse] = Await.result(FindRegisteredModel2Async(body), Duration.Inf)

  def GetModelVersionAsync(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetModelVersionRequestResponse]("GET", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/versions/$id_model_version_id", __query.toMap, body, RegistryGetModelVersionRequestResponse.fromJson)
  }

  def GetModelVersion(id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetModelVersionRequestResponse] = Await.result(GetModelVersionAsync(id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id, id_model_id_registered_model_id), Duration.Inf)

  def GetModelVersion2Async(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetModelVersionRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_name.isDefined) __query.update("id.model_id.named_id.name", client.toQuery(id_model_id_named_id_name.get))
    if (id_model_id_named_id_workspace_name.isDefined) __query.update("id.model_id.named_id.workspace_name", client.toQuery(id_model_id_named_id_workspace_name.get))
    if (id_model_id_registered_model_id.isDefined) __query.update("id.model_id.registered_model_id", client.toQuery(id_model_id_registered_model_id.get))
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetModelVersionRequestResponse]("GET", basePath + s"/registry/registered_model_versions/$id_model_version_id", __query.toMap, body, RegistryGetModelVersionRequestResponse.fromJson)
  }

  def GetModelVersion2(id_model_version_id: BigInt, id_model_id_named_id_name: Option[String]=None, id_model_id_named_id_workspace_name: Option[String]=None, id_model_id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetModelVersionRequestResponse] = Await.result(GetModelVersion2Async(id_model_version_id, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_id_registered_model_id), Duration.Inf)

  def GetRegisteredModelAsync(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id.isDefined) __query.update("id.registered_model_id", client.toQuery(id_registered_model_id.get))
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    val body: String = null
    return client.request[String, RegistryGetRegisteredModelRequestResponse]("GET", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistryGetRegisteredModelRequestResponse.fromJson)
  }

  def GetRegisteredModel(id_named_id_name: String, id_named_id_workspace_name: String, id_registered_model_id: Option[BigInt]=None)(implicit ec: ExecutionContext): Try[RegistryGetRegisteredModelRequestResponse] = Await.result(GetRegisteredModelAsync(id_named_id_name, id_named_id_workspace_name, id_registered_model_id), Duration.Inf)

  def GetRegisteredModel2Async(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetRegisteredModelRequestResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_name.isDefined) __query.update("id.named_id.name", client.toQuery(id_named_id_name.get))
    if (id_named_id_workspace_name.isDefined) __query.update("id.named_id.workspace_name", client.toQuery(id_named_id_workspace_name.get))
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    val body: String = null
    return client.request[String, RegistryGetRegisteredModelRequestResponse]("GET", basePath + s"/registry/registered_models/$id_registered_model_id", __query.toMap, body, RegistryGetRegisteredModelRequestResponse.fromJson)
  }

  def GetRegisteredModel2(id_registered_model_id: BigInt, id_named_id_name: Option[String]=None, id_named_id_workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryGetRegisteredModelRequestResponse] = Await.result(GetRegisteredModel2Async(id_registered_model_id, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def UpdateModelVersionAsync(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_workspace_name\"")
    if (id_model_id_named_id_name == null) throw new Exception("Missing required parameter \"id_model_id_named_id_name\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PUT", basePath + s"/registry/workspaces/$id_model_id_named_id_workspace_name/registered_models/$id_model_id_named_id_name/versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def UpdateModelVersion(body: RegistryModelVersion, id_model_id_named_id_name: String, id_model_id_named_id_workspace_name: String, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(UpdateModelVersionAsync(body, id_model_id_named_id_name, id_model_id_named_id_workspace_name, id_model_version_id), Duration.Inf)

  def UpdateModelVersion2Async(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetModelVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_model_id_registered_model_id == null) throw new Exception("Missing required parameter \"id_model_id_registered_model_id\"")
    if (id_model_version_id == null) throw new Exception("Missing required parameter \"id_model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryModelVersion, RegistrySetModelVersionResponse]("PUT", basePath + s"/registry/$id_model_id_registered_model_id/versions/$id_model_version_id", __query.toMap, body, RegistrySetModelVersionResponse.fromJson)
  }

  def UpdateModelVersion2(body: RegistryModelVersion, id_model_id_registered_model_id: BigInt, id_model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetModelVersionResponse] = Await.result(UpdateModelVersion2Async(body, id_model_id_registered_model_id, id_model_version_id), Duration.Inf)

  def UpdateRegisteredModelAsync(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_named_id_workspace_name == null) throw new Exception("Missing required parameter \"id_named_id_workspace_name\"")
    if (id_named_id_name == null) throw new Exception("Missing required parameter \"id_named_id_name\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PUT", basePath + s"/registry/workspaces/$id_named_id_workspace_name/registered_models/$id_named_id_name", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def UpdateRegisteredModel(body: RegistryRegisteredModel, id_named_id_name: String, id_named_id_workspace_name: String)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(UpdateRegisteredModelAsync(body, id_named_id_name, id_named_id_workspace_name), Duration.Inf)

  def UpdateRegisteredModel2Async(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistrySetRegisteredModelResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id_registered_model_id == null) throw new Exception("Missing required parameter \"id_registered_model_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryRegisteredModel, RegistrySetRegisteredModelResponse]("PUT", basePath + s"/registry/$id_registered_model_id", __query.toMap, body, RegistrySetRegisteredModelResponse.fromJson)
  }

  def UpdateRegisteredModel2(body: RegistryRegisteredModel, id_registered_model_id: BigInt)(implicit ec: ExecutionContext): Try[RegistrySetRegisteredModelResponse] = Await.result(UpdateRegisteredModel2Async(body, id_registered_model_id), Duration.Inf)

  def commitArtifactPartAsync(body: RegistryCommitArtifactPart, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryCommitArtifactPartResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryCommitArtifactPart, RegistryCommitArtifactPartResponse]("POST", basePath + s"/registry/versions/$model_version_id/commitArtifactPart", __query.toMap, body, RegistryCommitArtifactPartResponse.fromJson)
  }

  def commitArtifactPart(body: RegistryCommitArtifactPart, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryCommitArtifactPartResponse] = Await.result(commitArtifactPartAsync(body, model_version_id), Duration.Inf)

  def commitMultipartArtifactAsync(body: RegistryCommitMultipartArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryCommitMultipartArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryCommitMultipartArtifact, RegistryCommitMultipartArtifactResponse]("POST", basePath + s"/registry/versions/$model_version_id/commitMultipartArtifact", __query.toMap, body, RegistryCommitMultipartArtifactResponse.fromJson)
  }

  def commitMultipartArtifact(body: RegistryCommitMultipartArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryCommitMultipartArtifactResponse] = Await.result(commitMultipartArtifactAsync(body, model_version_id), Duration.Inf)

  def getCommittedArtifactPartsAsync(model_version_id: BigInt, key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[RegistryGetCommittedArtifactPartsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (key.isDefined) __query.update("key", client.toQuery(key.get))
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    val body: String = null
    return client.request[String, RegistryGetCommittedArtifactPartsResponse]("GET", basePath + s"/registry/versions/$model_version_id/getCommittedArtifactParts", __query.toMap, body, RegistryGetCommittedArtifactPartsResponse.fromJson)
  }

  def getCommittedArtifactParts(model_version_id: BigInt, key: Option[String]=None)(implicit ec: ExecutionContext): Try[RegistryGetCommittedArtifactPartsResponse] = Await.result(getCommittedArtifactPartsAsync(model_version_id, key), Duration.Inf)

  def getUrlForArtifactAsync(body: RegistryGetUrlForArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Future[Try[RegistryGetUrlForArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (model_version_id == null) throw new Exception("Missing required parameter \"model_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[RegistryGetUrlForArtifact, RegistryGetUrlForArtifactResponse]("POST", basePath + s"/registry/versions/$model_version_id/getUrlForArtifact", __query.toMap, body, RegistryGetUrlForArtifactResponse.fromJson)
  }

  def getUrlForArtifact(body: RegistryGetUrlForArtifact, model_version_id: BigInt)(implicit ec: ExecutionContext): Try[RegistryGetUrlForArtifactResponse] = Await.result(getUrlForArtifactAsync(body, model_version_id), Duration.Inf)

}
