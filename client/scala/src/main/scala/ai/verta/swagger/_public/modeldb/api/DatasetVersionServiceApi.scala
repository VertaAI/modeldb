// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class DatasetVersionServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def DatasetVersionService_addDatasetVersionAttributesAsync(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionAttributes, ModeldbAddDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/addDatasetVersionAttributes", __query.toMap, body, ModeldbAddDatasetVersionAttributesResponse.fromJson)
  }

  def DatasetVersionService_addDatasetVersionAttributes(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionAttributesResponse] = Await.result(DatasetVersionService_addDatasetVersionAttributesAsync(body), Duration.Inf)

  def DatasetVersionService_addDatasetVersionTagsAsync(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionTags, ModeldbAddDatasetVersionTagsResponse]("POST", basePath + s"/dataset-version/addDatasetVersionTags", __query.toMap, body, ModeldbAddDatasetVersionTagsResponse.fromJson)
  }

  def DatasetVersionService_addDatasetVersionTags(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionTagsResponse] = Await.result(DatasetVersionService_addDatasetVersionTagsAsync(body), Duration.Inf)

  def DatasetVersionService_commitMultipartVersionedDatasetBlobArtifactAsync(body: ModeldbCommitMultipartVersionedDatasetBlobArtifact)(implicit ec: ExecutionContext): Future[Try[ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCommitMultipartVersionedDatasetBlobArtifact, ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse]("POST", basePath + s"/dataset-version/commitMultipartVersionedDatasetBlobArtifact", __query.toMap, body, ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse.fromJson)
  }

  def DatasetVersionService_commitMultipartVersionedDatasetBlobArtifact(body: ModeldbCommitMultipartVersionedDatasetBlobArtifact)(implicit ec: ExecutionContext): Try[ModeldbCommitMultipartVersionedDatasetBlobArtifactResponse] = Await.result(DatasetVersionService_commitMultipartVersionedDatasetBlobArtifactAsync(body), Duration.Inf)

  def DatasetVersionService_commitVersionedDatasetBlobArtifactPartAsync(body: ModeldbCommitVersionedDatasetBlobArtifactPart)(implicit ec: ExecutionContext): Future[Try[ModeldbCommitVersionedDatasetBlobArtifactPartResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCommitVersionedDatasetBlobArtifactPart, ModeldbCommitVersionedDatasetBlobArtifactPartResponse]("POST", basePath + s"/dataset-version/commitVersionedDatasetBlobArtifactPart", __query.toMap, body, ModeldbCommitVersionedDatasetBlobArtifactPartResponse.fromJson)
  }

  def DatasetVersionService_commitVersionedDatasetBlobArtifactPart(body: ModeldbCommitVersionedDatasetBlobArtifactPart)(implicit ec: ExecutionContext): Try[ModeldbCommitVersionedDatasetBlobArtifactPartResponse] = Await.result(DatasetVersionService_commitVersionedDatasetBlobArtifactPartAsync(body), Duration.Inf)

  def DatasetVersionService_createDatasetVersionAsync(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateDatasetVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateDatasetVersion, ModeldbCreateDatasetVersionResponse]("POST", basePath + s"/dataset-version/createDatasetVersion", __query.toMap, body, ModeldbCreateDatasetVersionResponse.fromJson)
  }

  def DatasetVersionService_createDatasetVersion(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbCreateDatasetVersionResponse] = Await.result(DatasetVersionService_createDatasetVersionAsync(body), Duration.Inf)

  def DatasetVersionService_deleteDatasetVersionAsync(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersion, ModeldbDeleteDatasetVersionResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersion", __query.toMap, body, ModeldbDeleteDatasetVersionResponse.fromJson)
  }

  def DatasetVersionService_deleteDatasetVersion(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionResponse] = Await.result(DatasetVersionService_deleteDatasetVersionAsync(body), Duration.Inf)

  def DatasetVersionService_deleteDatasetVersionAttributesAsync(body: ModeldbDeleteDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersionAttributes, ModeldbDeleteDatasetVersionAttributesResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionAttributes", __query.toMap, body, ModeldbDeleteDatasetVersionAttributesResponse.fromJson)
  }

  def DatasetVersionService_deleteDatasetVersionAttributes(body: ModeldbDeleteDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionAttributesResponse] = Await.result(DatasetVersionService_deleteDatasetVersionAttributesAsync(body), Duration.Inf)

  def DatasetVersionService_deleteDatasetVersionTagsAsync(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersionTags, ModeldbDeleteDatasetVersionTagsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionTags", __query.toMap, body, ModeldbDeleteDatasetVersionTagsResponse.fromJson)
  }

  def DatasetVersionService_deleteDatasetVersionTags(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionTagsResponse] = Await.result(DatasetVersionService_deleteDatasetVersionTagsAsync(body), Duration.Inf)

  def DatasetVersionService_deleteDatasetVersionsAsync(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersions, ModeldbDeleteDatasetVersionsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersions", __query.toMap, body, ModeldbDeleteDatasetVersionsResponse.fromJson)
  }

  def DatasetVersionService_deleteDatasetVersions(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionsResponse] = Await.result(DatasetVersionService_deleteDatasetVersionsAsync(body), Duration.Inf)

  def DatasetVersionService_findDatasetVersionsAsync(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbFindDatasetVersionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasetVersions, ModeldbFindDatasetVersionsResponse]("POST", basePath + s"/dataset-version/findDatasetVersions", __query.toMap, body, ModeldbFindDatasetVersionsResponse.fromJson)
  }

  def DatasetVersionService_findDatasetVersions(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbFindDatasetVersionsResponse] = Await.result(DatasetVersionService_findDatasetVersionsAsync(body), Duration.Inf)

  def DatasetVersionService_getAllDatasetVersionsByDatasetIdAsync(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetAllDatasetVersionsByDatasetIdResponse]("GET", basePath + s"/dataset-version/getAllDatasetVersionsByDatasetId", __query.toMap, body, ModeldbGetAllDatasetVersionsByDatasetIdResponse.fromJson)
  }

  def DatasetVersionService_getAllDatasetVersionsByDatasetId(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse] = Await.result(DatasetVersionService_getAllDatasetVersionsByDatasetIdAsync(ascending, dataset_id, page_limit, page_number, sort_key), Duration.Inf)

  def DatasetVersionService_getCommittedVersionedDatasetBlobArtifactPartsAsync(dataset_id: Option[String]=None, dataset_version_id: Option[String]=None, path_dataset_component_blob_path: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    if (dataset_version_id.isDefined) __query.update("dataset_version_id", client.toQuery(dataset_version_id.get))
    if (path_dataset_component_blob_path.isDefined) __query.update("path_dataset_component_blob_path", client.toQuery(path_dataset_component_blob_path.get))
    val body: String = null
    return client.request[String, ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse]("GET", basePath + s"/dataset-version/getCommittedVersionedDatasetBlobArtifactParts", __query.toMap, body, ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse.fromJson)
  }

  def DatasetVersionService_getCommittedVersionedDatasetBlobArtifactParts(dataset_id: Option[String]=None, dataset_version_id: Option[String]=None, path_dataset_component_blob_path: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetCommittedVersionedDatasetBlobArtifactPartsResponse] = Await.result(DatasetVersionService_getCommittedVersionedDatasetBlobArtifactPartsAsync(dataset_id, dataset_version_id, path_dataset_component_blob_path), Duration.Inf)

  def DatasetVersionService_getDatasetVersionAttributesAsync(attribute_keys: Option[List[String]]=None, dataset_id: Option[String]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetVersionAttributesResponse]("GET", basePath + s"/dataset-version/getDatasetVersionAttributes", __query.toMap, body, ModeldbGetDatasetVersionAttributesResponse.fromJson)
  }

  def DatasetVersionService_getDatasetVersionAttributes(attribute_keys: Option[List[String]]=None, dataset_id: Option[String]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetVersionAttributesResponse] = Await.result(DatasetVersionService_getDatasetVersionAttributesAsync(attribute_keys, dataset_id, get_all, id), Duration.Inf)

  def DatasetVersionService_getDatasetVersionByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetVersionByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetVersionByIdResponse]("GET", basePath + s"/dataset-version/getDatasetVersionById", __query.toMap, body, ModeldbGetDatasetVersionByIdResponse.fromJson)
  }

  def DatasetVersionService_getDatasetVersionById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetVersionByIdResponse] = Await.result(DatasetVersionService_getDatasetVersionByIdAsync(id), Duration.Inf)

  def DatasetVersionService_getLatestDatasetVersionByDatasetIdAsync(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetLatestDatasetVersionByDatasetIdResponse]("GET", basePath + s"/dataset-version/getLatestDatasetVersionByDatasetId", __query.toMap, body, ModeldbGetLatestDatasetVersionByDatasetIdResponse.fromJson)
  }

  def DatasetVersionService_getLatestDatasetVersionByDatasetId(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse] = Await.result(DatasetVersionService_getLatestDatasetVersionByDatasetIdAsync(ascending, dataset_id, sort_key), Duration.Inf)

  def DatasetVersionService_getUrlForDatasetBlobVersionedAsync(body: ModeldbGetUrlForDatasetBlobVersioned, dataset_id: String, dataset_version_id: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForDatasetBlobVersionedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (workspace_name == null) throw new Exception("Missing required parameter \"workspace_name\"")
    if (dataset_id == null) throw new Exception("Missing required parameter \"dataset_id\"")
    if (dataset_version_id == null) throw new Exception("Missing required parameter \"dataset_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForDatasetBlobVersioned, ModeldbGetUrlForDatasetBlobVersionedResponse]("POST", basePath + s"/dataset-version/workspaces/$workspace_name/dataset/$dataset_id/datasetVersion/$dataset_version_id/getUrlForDatasetBlobVersioned", __query.toMap, body, ModeldbGetUrlForDatasetBlobVersionedResponse.fromJson)
  }

  def DatasetVersionService_getUrlForDatasetBlobVersioned(body: ModeldbGetUrlForDatasetBlobVersioned, dataset_id: String, dataset_version_id: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForDatasetBlobVersionedResponse] = Await.result(DatasetVersionService_getUrlForDatasetBlobVersionedAsync(body, dataset_id, dataset_version_id, workspace_name), Duration.Inf)

  def DatasetVersionService_getUrlForDatasetBlobVersioned2Async(body: ModeldbGetUrlForDatasetBlobVersioned, dataset_id: String, dataset_version_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetUrlForDatasetBlobVersionedResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id == null) throw new Exception("Missing required parameter \"dataset_id\"")
    if (dataset_version_id == null) throw new Exception("Missing required parameter \"dataset_version_id\"")
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetUrlForDatasetBlobVersioned, ModeldbGetUrlForDatasetBlobVersionedResponse]("POST", basePath + s"/dataset-version/dataset/$dataset_id/datasetVersion/$dataset_version_id/getUrlForDatasetBlobVersioned", __query.toMap, body, ModeldbGetUrlForDatasetBlobVersionedResponse.fromJson)
  }

  def DatasetVersionService_getUrlForDatasetBlobVersioned2(body: ModeldbGetUrlForDatasetBlobVersioned, dataset_id: String, dataset_version_id: String)(implicit ec: ExecutionContext): Try[ModeldbGetUrlForDatasetBlobVersionedResponse] = Await.result(DatasetVersionService_getUrlForDatasetBlobVersioned2Async(body, dataset_id, dataset_version_id), Duration.Inf)

  def DatasetVersionService_setDatasetVersionVisibilityAsync(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetVersionVisibiltyResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetVersionVisibilty, ModeldbSetDatasetVersionVisibiltyResponse]("POST", basePath + s"/dataset-version/setDatasetVersionVisibility", __query.toMap, body, ModeldbSetDatasetVersionVisibiltyResponse.fromJson)
  }

  def DatasetVersionService_setDatasetVersionVisibility(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetVersionVisibiltyResponse] = Await.result(DatasetVersionService_setDatasetVersionVisibilityAsync(body), Duration.Inf)

  def DatasetVersionService_updateDatasetVersionAttributesAsync(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionAttributes, ModeldbUpdateDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionAttributes", __query.toMap, body, ModeldbUpdateDatasetVersionAttributesResponse.fromJson)
  }

  def DatasetVersionService_updateDatasetVersionAttributes(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionAttributesResponse] = Await.result(DatasetVersionService_updateDatasetVersionAttributesAsync(body), Duration.Inf)

  def DatasetVersionService_updateDatasetVersionDescriptionAsync(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionDescription, ModeldbUpdateDatasetVersionDescriptionResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionDescription", __query.toMap, body, ModeldbUpdateDatasetVersionDescriptionResponse.fromJson)
  }

  def DatasetVersionService_updateDatasetVersionDescription(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionDescriptionResponse] = Await.result(DatasetVersionService_updateDatasetVersionDescriptionAsync(body), Duration.Inf)

}
