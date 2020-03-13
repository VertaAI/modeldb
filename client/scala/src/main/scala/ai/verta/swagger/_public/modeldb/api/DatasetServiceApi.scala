// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class DatasetServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addDatasetAttributesAsync(body: ModeldbAddDatasetAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetAttributes, ModeldbAddDatasetAttributesResponse]("POST", basePath + s"/dataset/addDatasetAttributes", __query, body, ModeldbAddDatasetAttributesResponse.fromJson)
  }

  def addDatasetAttributes(body: ModeldbAddDatasetAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetAttributesResponse] = Await.result(addDatasetAttributesAsync(body), Duration.Inf)

  def addDatasetTagsAsync(body: ModeldbAddDatasetTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetTags, ModeldbAddDatasetTagsResponse]("POST", basePath + s"/dataset/addDatasetTags", __query, body, ModeldbAddDatasetTagsResponse.fromJson)
  }

  def addDatasetTags(body: ModeldbAddDatasetTags)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetTagsResponse] = Await.result(addDatasetTagsAsync(body), Duration.Inf)

  def createDatasetAsync(body: ModeldbCreateDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateDatasetResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateDataset, ModeldbCreateDatasetResponse]("POST", basePath + s"/dataset/createDataset", __query, body, ModeldbCreateDatasetResponse.fromJson)
  }

  def createDataset(body: ModeldbCreateDataset)(implicit ec: ExecutionContext): Try[ModeldbCreateDatasetResponse] = Await.result(createDatasetAsync(body), Duration.Inf)

  def deleteDatasetAsync(body: ModeldbDeleteDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDataset, ModeldbDeleteDatasetResponse]("DELETE", basePath + s"/dataset/deleteDataset", __query, body, ModeldbDeleteDatasetResponse.fromJson)
  }

  def deleteDataset(body: ModeldbDeleteDataset)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetResponse] = Await.result(deleteDatasetAsync(body), Duration.Inf)

  def deleteDatasetAttributesAsync(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "delete_all" -> client.toQuery(delete_all)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteDatasetAttributesResponse]("DELETE", basePath + s"/dataset/deleteDatasetAttributes", __query, body, ModeldbDeleteDatasetAttributesResponse.fromJson)
  }

  def deleteDatasetAttributes(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetAttributesResponse] = Await.result(deleteDatasetAttributesAsync(id, attribute_keys, delete_all), Duration.Inf)

  def deleteDatasetTagsAsync(body: ModeldbDeleteDatasetTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetTags, ModeldbDeleteDatasetTagsResponse]("DELETE", basePath + s"/dataset/deleteDatasetTags", __query, body, ModeldbDeleteDatasetTagsResponse.fromJson)
  }

  def deleteDatasetTags(body: ModeldbDeleteDatasetTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetTagsResponse] = Await.result(deleteDatasetTagsAsync(body), Duration.Inf)

  def deleteDatasetsAsync(body: ModeldbDeleteDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasets, ModeldbDeleteDatasetsResponse]("DELETE", basePath + s"/dataset/deleteDatasets", __query, body, ModeldbDeleteDatasetsResponse.fromJson)
  }

  def deleteDatasets(body: ModeldbDeleteDatasets)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetsResponse] = Await.result(deleteDatasetsAsync(body), Duration.Inf)

  def findDatasetsAsync(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Future[Try[ModeldbFindDatasetsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasets, ModeldbFindDatasetsResponse]("POST", basePath + s"/dataset/findDatasets", __query, body, ModeldbFindDatasetsResponse.fromJson)
  }

  def findDatasets(body: ModeldbFindDatasets)(implicit ec: ExecutionContext): Try[ModeldbFindDatasetsResponse] = Await.result(findDatasetsAsync(body), Duration.Inf)

  def getAllDatasetsAsync(page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAllDatasetsResponse]] = {
    val __query = Map[String,String](
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key),
      "workspace_name" -> client.toQuery(workspace_name)
    )
    val body: String = null
    return client.request[String, ModeldbGetAllDatasetsResponse]("GET", basePath + s"/dataset/getAllDatasets", __query, body, ModeldbGetAllDatasetsResponse.fromJson)
  }

  def getAllDatasets(page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetAllDatasetsResponse] = Await.result(getAllDatasetsAsync(page_number, page_limit, ascending, sort_key, workspace_name), Duration.Inf)

  def getDatasetAttributesAsync(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "get_all" -> client.toQuery(get_all)
    )
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/dataset/getDatasetAttributes", __query, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getDatasetAttributes(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getDatasetAttributesAsync(id, attribute_keys, get_all), Duration.Inf)

  def getDatasetByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetDatasetByIdResponse]("GET", basePath + s"/dataset/getDatasetById", __query, body, ModeldbGetDatasetByIdResponse.fromJson)
  }

  def getDatasetById(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetByIdResponse] = Await.result(getDatasetByIdAsync(id), Duration.Inf)

  def getDatasetByNameAsync(name: String, workspace_name: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetByNameResponse]] = {
    val __query = Map[String,String](
      "name" -> client.toQuery(name),
      "workspace_name" -> client.toQuery(workspace_name)
    )
    val body: String = null
    return client.request[String, ModeldbGetDatasetByNameResponse]("GET", basePath + s"/dataset/getDatasetByName", __query, body, ModeldbGetDatasetByNameResponse.fromJson)
  }

  def getDatasetByName(name: String, workspace_name: String)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetByNameResponse] = Await.result(getDatasetByNameAsync(name, workspace_name), Duration.Inf)

  def getDatasetTagsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/dataset/getDatasetTags", __query, body, ModeldbGetTagsResponse.fromJson)
  }

  def getDatasetTags(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getDatasetTagsAsync(id), Duration.Inf)

  def getExperimentRunByDatasetAsync(body: ModeldbGetExperimentRunByDataset)(implicit ec: ExecutionContext): Future[Try[ModeldbGetExperimentRunByDatasetResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbGetExperimentRunByDataset, ModeldbGetExperimentRunByDatasetResponse]("POST", basePath + s"/dataset/getExperimentRunByDataset", __query, body, ModeldbGetExperimentRunByDatasetResponse.fromJson)
  }

  def getExperimentRunByDataset(body: ModeldbGetExperimentRunByDataset)(implicit ec: ExecutionContext): Try[ModeldbGetExperimentRunByDatasetResponse] = Await.result(getExperimentRunByDatasetAsync(body), Duration.Inf)

  def getLastExperimentByDatasetIdAsync(dataset_id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbLastExperimentByDatasetIdResponse]] = {
    val __query = Map[String,String](
      "dataset_id" -> client.toQuery(dataset_id)
    )
    val body: String = null
    return client.request[String, ModeldbLastExperimentByDatasetIdResponse]("GET", basePath + s"/dataset/getLastExperimentByDatasetId", __query, body, ModeldbLastExperimentByDatasetIdResponse.fromJson)
  }

  def getLastExperimentByDatasetId(dataset_id: String)(implicit ec: ExecutionContext): Try[ModeldbLastExperimentByDatasetIdResponse] = Await.result(getLastExperimentByDatasetIdAsync(dataset_id), Duration.Inf)

  def setDatasetVisibilityAsync(body: ModeldbSetDatasetVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetVisibiltyResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetVisibilty, ModeldbSetDatasetVisibiltyResponse]("POST", basePath + s"/dataset/setDatasetVisibility", __query, body, ModeldbSetDatasetVisibiltyResponse.fromJson)
  }

  def setDatasetVisibility(body: ModeldbSetDatasetVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetVisibiltyResponse] = Await.result(setDatasetVisibilityAsync(body), Duration.Inf)

  def setDatasetWorkspaceAsync(body: ModeldbSetDatasetWorkspace)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetWorkspaceResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetWorkspace, ModeldbSetDatasetWorkspaceResponse]("POST", basePath + s"/dataset/setDatasetWorkspace", __query, body, ModeldbSetDatasetWorkspaceResponse.fromJson)
  }

  def setDatasetWorkspace(body: ModeldbSetDatasetWorkspace)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetWorkspaceResponse] = Await.result(setDatasetWorkspaceAsync(body), Duration.Inf)

  def updateDatasetAttributesAsync(body: ModeldbUpdateDatasetAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetAttributes, ModeldbUpdateDatasetAttributesResponse]("POST", basePath + s"/dataset/updateDatasetAttributes", __query, body, ModeldbUpdateDatasetAttributesResponse.fromJson)
  }

  def updateDatasetAttributes(body: ModeldbUpdateDatasetAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetAttributesResponse] = Await.result(updateDatasetAttributesAsync(body), Duration.Inf)

  def updateDatasetDescriptionAsync(body: ModeldbUpdateDatasetDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetDescriptionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetDescription, ModeldbUpdateDatasetDescriptionResponse]("POST", basePath + s"/dataset/updateDatasetDescription", __query, body, ModeldbUpdateDatasetDescriptionResponse.fromJson)
  }

  def updateDatasetDescription(body: ModeldbUpdateDatasetDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetDescriptionResponse] = Await.result(updateDatasetDescriptionAsync(body), Duration.Inf)

  def updateDatasetNameAsync(body: ModeldbUpdateDatasetName)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetNameResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetName, ModeldbUpdateDatasetNameResponse]("POST", basePath + s"/dataset/updateDatasetName", __query, body, ModeldbUpdateDatasetNameResponse.fromJson)
  }

  def updateDatasetName(body: ModeldbUpdateDatasetName)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetNameResponse] = Await.result(updateDatasetNameAsync(body), Duration.Inf)

}
