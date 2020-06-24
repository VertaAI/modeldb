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
  def addDatasetVersionAttributesAsync(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionAttributes, ModeldbAddDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/addDatasetVersionAttributes", __query.toMap, body, ModeldbAddDatasetVersionAttributesResponse.fromJson)
  }

  def addDatasetVersionAttributes(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionAttributesResponse] = Await.result(addDatasetVersionAttributesAsync(body), Duration.Inf)

  def addDatasetVersionTagsAsync(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionTags, ModeldbAddDatasetVersionTagsResponse]("POST", basePath + s"/dataset-version/addDatasetVersionTags", __query.toMap, body, ModeldbAddDatasetVersionTagsResponse.fromJson)
  }

  def addDatasetVersionTags(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionTagsResponse] = Await.result(addDatasetVersionTagsAsync(body), Duration.Inf)

  def createDatasetVersionAsync(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateDatasetVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateDatasetVersion, ModeldbCreateDatasetVersionResponse]("POST", basePath + s"/dataset-version/createDatasetVersion", __query.toMap, body, ModeldbCreateDatasetVersionResponse.fromJson)
  }

  def createDatasetVersion(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbCreateDatasetVersionResponse] = Await.result(createDatasetVersionAsync(body), Duration.Inf)

  def deleteDatasetVersionAsync(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersion, ModeldbDeleteDatasetVersionResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersion", __query.toMap, body, ModeldbDeleteDatasetVersionResponse.fromJson)
  }

  def deleteDatasetVersion(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionResponse] = Await.result(deleteDatasetVersionAsync(body), Duration.Inf)

  def deleteDatasetVersionAttributesAsync(body: ModeldbDeleteDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersionAttributes, ModeldbDeleteDatasetVersionAttributesResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionAttributes", __query.toMap, body, ModeldbDeleteDatasetVersionAttributesResponse.fromJson)
  }

  def deleteDatasetVersionAttributes(body: ModeldbDeleteDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionAttributesResponse] = Await.result(deleteDatasetVersionAttributesAsync(body), Duration.Inf)

  def deleteDatasetVersionTagsAsync(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionTagsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersionTags, ModeldbDeleteDatasetVersionTagsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionTags", __query.toMap, body, ModeldbDeleteDatasetVersionTagsResponse.fromJson)
  }

  def deleteDatasetVersionTags(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionTagsResponse] = Await.result(deleteDatasetVersionTagsAsync(body), Duration.Inf)

  def deleteDatasetVersionsAsync(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersions, ModeldbDeleteDatasetVersionsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersions", __query.toMap, body, ModeldbDeleteDatasetVersionsResponse.fromJson)
  }

  def deleteDatasetVersions(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionsResponse] = Await.result(deleteDatasetVersionsAsync(body), Duration.Inf)

  def findDatasetVersionsAsync(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbFindDatasetVersionsResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasetVersions, ModeldbFindDatasetVersionsResponse]("POST", basePath + s"/dataset-version/findDatasetVersions", __query.toMap, body, ModeldbFindDatasetVersionsResponse.fromJson)
  }

  def findDatasetVersions(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbFindDatasetVersionsResponse] = Await.result(findDatasetVersionsAsync(body), Duration.Inf)

  def getAllDatasetVersionsByDatasetIdAsync(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    if (page_number.isDefined) __query.update("page_number", client.toQuery(page_number.get))
    if (page_limit.isDefined) __query.update("page_limit", client.toQuery(page_limit.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetAllDatasetVersionsByDatasetIdResponse]("GET", basePath + s"/dataset-version/getAllDatasetVersionsByDatasetId", __query.toMap, body, ModeldbGetAllDatasetVersionsByDatasetIdResponse.fromJson)
  }

  def getAllDatasetVersionsByDatasetId(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, page_limit: Option[BigInt]=None, page_number: Option[BigInt]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse] = Await.result(getAllDatasetVersionsByDatasetIdAsync(ascending, dataset_id, page_limit, page_number, sort_key), Duration.Inf)

  def getDatasetVersionAttributesAsync(attribute_keys: Option[List[String]]=None, dataset_id: Option[String]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (id.isDefined) __query.update("id", client.toQuery(id.get))
    if (attribute_keys.isDefined) __query.update("attribute_keys", client.toQuery(attribute_keys.get))
    if (get_all.isDefined) __query.update("get_all", client.toQuery(get_all.get))
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    val body: String = null
    return client.request[String, ModeldbGetDatasetVersionAttributesResponse]("GET", basePath + s"/dataset-version/getDatasetVersionAttributes", __query.toMap, body, ModeldbGetDatasetVersionAttributesResponse.fromJson)
  }

  def getDatasetVersionAttributes(attribute_keys: Option[List[String]]=None, dataset_id: Option[String]=None, get_all: Option[Boolean]=None, id: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetVersionAttributesResponse] = Await.result(getDatasetVersionAttributesAsync(attribute_keys, dataset_id, get_all, id), Duration.Inf)

  def getLatestDatasetVersionByDatasetIdAsync(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Future[Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (dataset_id.isDefined) __query.update("dataset_id", client.toQuery(dataset_id.get))
    if (ascending.isDefined) __query.update("ascending", client.toQuery(ascending.get))
    if (sort_key.isDefined) __query.update("sort_key", client.toQuery(sort_key.get))
    val body: String = null
    return client.request[String, ModeldbGetLatestDatasetVersionByDatasetIdResponse]("GET", basePath + s"/dataset-version/getLatestDatasetVersionByDatasetId", __query.toMap, body, ModeldbGetLatestDatasetVersionByDatasetIdResponse.fromJson)
  }

  def getLatestDatasetVersionByDatasetId(ascending: Option[Boolean]=None, dataset_id: Option[String]=None, sort_key: Option[String]=None)(implicit ec: ExecutionContext): Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse] = Await.result(getLatestDatasetVersionByDatasetIdAsync(ascending, dataset_id, sort_key), Duration.Inf)

  def setDatasetVersionVisibilityAsync(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetVersionVisibiltyResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetVersionVisibilty, ModeldbSetDatasetVersionVisibiltyResponse]("POST", basePath + s"/dataset-version/setDatasetVersionVisibility", __query.toMap, body, ModeldbSetDatasetVersionVisibiltyResponse.fromJson)
  }

  def setDatasetVersionVisibility(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetVersionVisibiltyResponse] = Await.result(setDatasetVersionVisibilityAsync(body), Duration.Inf)

  def updateDatasetVersionAttributesAsync(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionAttributesResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionAttributes, ModeldbUpdateDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionAttributes", __query.toMap, body, ModeldbUpdateDatasetVersionAttributesResponse.fromJson)
  }

  def updateDatasetVersionAttributes(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionAttributesResponse] = Await.result(updateDatasetVersionAttributesAsync(body), Duration.Inf)

  def updateDatasetVersionDescriptionAsync(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionDescriptionResponse]] = {
    var __query = new mutable.HashMap[String,List[String]]
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionDescription, ModeldbUpdateDatasetVersionDescriptionResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionDescription", __query.toMap, body, ModeldbUpdateDatasetVersionDescriptionResponse.fromJson)
  }

  def updateDatasetVersionDescription(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionDescriptionResponse] = Await.result(updateDatasetVersionDescriptionAsync(body), Duration.Inf)

}
