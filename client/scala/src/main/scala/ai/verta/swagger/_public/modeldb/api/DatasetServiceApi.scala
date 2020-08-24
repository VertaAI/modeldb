// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class DatasetServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def DatasetService_addDatasetAttributesAsync(body: ModeldbAddDatasetAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetAttributes, ModeldbAddDatasetAttributesResponse]("POST", basePath + s"/dataset/addDatasetAttributes", __query.toMap, body, ModeldbAddDatasetAttributesResponse.fromJson)
  }

  def DatasetService_addDatasetAttributes(body: ModeldbAddDatasetAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetAttributesResponse] = Await.result(DatasetService_addDatasetAttributesAsync(body), Duration.Inf)

  def DatasetService_addDatasetTagsAsync(body: ModeldbAddDatasetTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetTags, ModeldbAddDatasetTagsResponse]("POST", basePath + s"/dataset/addDatasetTags", __query.toMap, body, ModeldbAddDatasetTagsResponse.fromJson)
  }

  def DatasetService_addDatasetTags(body: ModeldbAddDatasetTags)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetTagsResponse] = Await.result(DatasetService_addDatasetTagsAsync(body), Duration.Inf)

  def DatasetService_createDatasetAsync(body: ModeldbCreateDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateDatasetResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateDataset, ModeldbCreateDatasetResponse]("POST", basePath + s"/dataset/createDataset", __query.toMap, body, ModeldbCreateDatasetResponse.fromJson)
  }

  def DatasetService_createDataset(body: ModeldbCreateDataset)(implicit ec: ExecutionContext): Try[ModeldbCreateDatasetResponse] = Await.result(DatasetService_createDatasetAsync(body), Duration.Inf)

  def DatasetService_deleteDatasetAsync(body: ModeldbDeleteDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDataset, ModeldbDeleteDatasetResponse]("DELETE", basePath + s"/dataset/deleteDataset", __query.toMap, body, ModeldbDeleteDatasetResponse.fromJson)
  }

  def DatasetService_deleteDataset(body: ModeldbDeleteDataset)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetResponse] = Await.result(DatasetService_deleteDatasetAsync(body), Duration.Inf)

  def DatasetService_deleteDatasetAttributesAsync(body: ModeldbDeleteDatasetAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetAttributes, ModeldbDeleteDatasetAttributesResponse]("DELETE", basePath + s"/dataset/deleteDatasetAttributes", __query.toMap, body, ModeldbDeleteDatasetAttributesResponse.fromJson)
  }

  def DatasetService_deleteDatasetAttributes(body: ModeldbDeleteDatasetAttributes)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetAttributesResponse] = Await.result(DatasetService_deleteDatasetAttributesAsync(body), Duration.Inf)

  def DatasetService_deleteDatasetTagsAsync(body: ModeldbDeleteDatasetTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetTags, ModeldbDeleteDatasetTagsResponse]("DELETE", basePath + s"/dataset/deleteDatasetTags", __query.toMap, body, ModeldbDeleteDatasetTagsResponse.fromJson)
  }

  def DatasetService_deleteDatasetTags(body: ModeldbDeleteDatasetTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetTagsResponse] = Await.result(DatasetService_deleteDatasetTagsAsync(body), Duration.Inf)

  def DatasetService_deleteDatasetsAsync(body: ModeldbDeleteDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasets, ModeldbDeleteDatasetsResponse]("DELETE", basePath + s"/dataset/deleteDatasets", __query.toMap, body, ModeldbDeleteDatasetsResponse.fromJson)
  }

  def DatasetService_deleteDatasets(body: ModeldbDeleteDatasets)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetsResponse] = Await.result(DatasetService_deleteDatasetsAsync(body), Duration.Inf)

  def DatasetService_findDatasetsAsync(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbFindDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasets, ModeldbFindDatasetsResponse]("POST", basePath + s"/dataset/findDatasets", __query.toMap, body, ModeldbFindDatasetsResponse.fromJson)
  }

  def DatasetService_findDatasets(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Try[ModeldbFindDatasetsResponse] = Await.result(DatasetService_findDatasetsAsync(body), Duration.Inf)

  def DatasetService_getAllDatasetsAsync(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAllDatasetsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetAllDatasetsResponse]("GET", basePath + s"/dataset/getAllDatasets", __query.toMap, body, ModeldbGetAllDatasetsResponse.fromJson)
  }

  def DatasetService_getAllDatasets(ascending: Option[Boolean]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAllDatasetsResponse] = Await.result(DatasetService_getAllDatasetsAsync(ascending, page_limit, page_number, sort_key, workspace_name), Duration.Inf)

  def DatasetService_getDatasetByIdAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetByIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetByIdResponse]("GET", basePath + s"/dataset/getDatasetById", __query.toMap, body, ModeldbGetDatasetByIdResponse.fromJson)
  }

  def DatasetService_getDatasetById(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetByIdResponse] = Await.result(DatasetService_getDatasetByIdAsync(id), Duration.Inf)

  def DatasetService_getDatasetByNameAsync(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetByNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (name.isDefined) __query.update("name", client.toQuery(name.get))
    if (workspace_name.isDefined) __query.update("workspace_name", client.toQuery(workspace_name.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetByNameResponse]("GET", basePath + s"/dataset/getDatasetByName", __query.toMap, body, ModeldbGetDatasetByNameResponse.fromJson)
  }

  def DatasetService_getDatasetByName(name: Option[String]=None, workspace_name: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetByNameResponse] = Await.result(DatasetService_getDatasetByNameAsync(name, workspace_name), Duration.Inf)

  def DatasetService_getDatasetTagsAsync(id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/dataset/getDatasetTags", __query.toMap, body, ModeldbGetTagsResponse.fromJson)
  }

  def DatasetService_getDatasetTags(id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(DatasetService_getDatasetTagsAsync(id), Duration.Inf)

  def DatasetService_getExperimentRunByDatasetAsync(body: ModeldbGetExperimentRunByDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByDatasetResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetExperimentRunByDataset, ModeldbGetExperimentRunByDatasetResponse]("POST", basePath + s"/dataset/getExperimentRunByDataset", __query.toMap, body, ModeldbGetExperimentRunByDatasetResponse.fromJson)
  }

  def DatasetService_getExperimentRunByDataset(body: ModeldbGetExperimentRunByDataset)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByDatasetResponse] = Await.result(DatasetService_getExperimentRunByDatasetAsync(body), Duration.Inf)

  def DatasetService_getLastExperimentByDatasetIdAsync(dataset_id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbLastExperimentByDatasetIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    val body: String = null
    return client.request[String, ModeldbLastExperimentByDatasetIdResponse]("GET", basePath + s"/dataset/getLastExperimentByDatasetId", __query.toMap, body, ModeldbLastExperimentByDatasetIdResponse.fromJson)
  }

  def DatasetService_getLastExperimentByDatasetId(dataset_id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbLastExperimentByDatasetIdResponse] = Await.result(DatasetService_getLastExperimentByDatasetIdAsync(dataset_id), Duration.Inf)

  def DatasetService_setDatasetVisibilityAsync(body: ModeldbSetDatasetVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetVisibiltyResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetVisibilty, ModeldbSetDatasetVisibiltyResponse]("POST", basePath + s"/dataset/setDatasetVisibility", __query.toMap, body, ModeldbSetDatasetVisibiltyResponse.fromJson)
  }

  def DatasetService_setDatasetVisibility(body: ModeldbSetDatasetVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetVisibiltyResponse] = Await.result(DatasetService_setDatasetVisibilityAsync(body), Duration.Inf)

  def DatasetService_setDatasetWorkspaceAsync(body: ModeldbSetDatasetWorkspace)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetWorkspaceResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetWorkspace, ModeldbSetDatasetWorkspaceResponse]("POST", basePath + s"/dataset/setDatasetWorkspace", __query.toMap, body, ModeldbSetDatasetWorkspaceResponse.fromJson)
  }

  def DatasetService_setDatasetWorkspace(body: ModeldbSetDatasetWorkspace)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetWorkspaceResponse] = Await.result(DatasetService_setDatasetWorkspaceAsync(body), Duration.Inf)

  def DatasetService_updateDatasetAttributesAsync(body: ModeldbUpdateDatasetAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetAttributes, ModeldbUpdateDatasetAttributesResponse]("POST", basePath + s"/dataset/updateDatasetAttributes", __query.toMap, body, ModeldbUpdateDatasetAttributesResponse.fromJson)
  }

  def DatasetService_updateDatasetAttributes(body: ModeldbUpdateDatasetAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetAttributesResponse] = Await.result(DatasetService_updateDatasetAttributesAsync(body), Duration.Inf)

  def DatasetService_updateDatasetDescriptionAsync(body: ModeldbUpdateDatasetDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetDescription, ModeldbUpdateDatasetDescriptionResponse]("POST", basePath + s"/dataset/updateDatasetDescription", __query.toMap, body, ModeldbUpdateDatasetDescriptionResponse.fromJson)
  }

  def DatasetService_updateDatasetDescription(body: ModeldbUpdateDatasetDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetDescriptionResponse] = Await.result(DatasetService_updateDatasetDescriptionAsync(body), Duration.Inf)

  def DatasetService_updateDatasetNameAsync(body: ModeldbUpdateDatasetName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetNameResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetName, ModeldbUpdateDatasetNameResponse]("POST", basePath + s"/dataset/updateDatasetName", __query.toMap, body, ModeldbUpdateDatasetNameResponse.fromJson)
  }

  def DatasetService_updateDatasetName(body: ModeldbUpdateDatasetName)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetNameResponse] = Await.result(DatasetService_updateDatasetNameAsync(body), Duration.Inf)

}
