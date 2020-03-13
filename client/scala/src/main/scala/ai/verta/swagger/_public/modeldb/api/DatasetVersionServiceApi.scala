// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.api

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Try

import ai.verta.swagger.client.HttpClient
import ai.verta.swagger.client.objects._
import ai.verta.swagger._public.modeldb.model._

class DatasetVersionServiceApi(client: HttpClient, val basePath: String = "/v1") {
  def addDatasetVersionAttributesAsync(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionAttributes, ModeldbAddDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/addDatasetVersionAttributes", __query, body, ModeldbAddDatasetVersionAttributesResponse.fromJson)
  }

  def addDatasetVersionAttributes(body: ModeldbAddDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionAttributesResponse] = Await.result(addDatasetVersionAttributesAsync(body), Duration.Inf)

  def addDatasetVersionTagsAsync(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbAddDatasetVersionTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbAddDatasetVersionTags, ModeldbAddDatasetVersionTagsResponse]("POST", basePath + s"/dataset-version/addDatasetVersionTags", __query, body, ModeldbAddDatasetVersionTagsResponse.fromJson)
  }

  def addDatasetVersionTags(body: ModeldbAddDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbAddDatasetVersionTagsResponse] = Await.result(addDatasetVersionTagsAsync(body), Duration.Inf)

  def createDatasetVersionAsync(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbCreateDatasetVersionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbCreateDatasetVersion, ModeldbCreateDatasetVersionResponse]("POST", basePath + s"/dataset-version/createDatasetVersion", __query, body, ModeldbCreateDatasetVersionResponse.fromJson)
  }

  def createDatasetVersion(body: ModeldbCreateDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbCreateDatasetVersionResponse] = Await.result(createDatasetVersionAsync(body), Duration.Inf)

  def deleteDatasetVersionAsync(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersion, ModeldbDeleteDatasetVersionResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersion", __query, body, ModeldbDeleteDatasetVersionResponse.fromJson)
  }

  def deleteDatasetVersion(body: ModeldbDeleteDatasetVersion)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionResponse] = Await.result(deleteDatasetVersionAsync(body), Duration.Inf)

  def deleteDatasetVersionAttributesAsync(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "delete_all" -> client.toQuery(delete_all)
    )
    val body: String = null
    return client.request[String, ModeldbDeleteDatasetVersionAttributesResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionAttributes", __query, body, ModeldbDeleteDatasetVersionAttributesResponse.fromJson)
  }

  def deleteDatasetVersionAttributes(id: String, attribute_keys: List[String], delete_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionAttributesResponse] = Await.result(deleteDatasetVersionAttributesAsync(id, attribute_keys, delete_all), Duration.Inf)

  def deleteDatasetVersionTagsAsync(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionTagsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersionTags, ModeldbDeleteDatasetVersionTagsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersionTags", __query, body, ModeldbDeleteDatasetVersionTagsResponse.fromJson)
  }

  def deleteDatasetVersionTags(body: ModeldbDeleteDatasetVersionTags)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionTagsResponse] = Await.result(deleteDatasetVersionTagsAsync(body), Duration.Inf)

  def deleteDatasetVersionsAsync(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbDeleteDatasetVersionsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbDeleteDatasetVersions, ModeldbDeleteDatasetVersionsResponse]("DELETE", basePath + s"/dataset-version/deleteDatasetVersions", __query, body, ModeldbDeleteDatasetVersionsResponse.fromJson)
  }

  def deleteDatasetVersions(body: ModeldbDeleteDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbDeleteDatasetVersionsResponse] = Await.result(deleteDatasetVersionsAsync(body), Duration.Inf)

  def findDatasetVersionsAsync(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Future[Try[ModeldbFindDatasetVersionsResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbFindDatasetVersions, ModeldbFindDatasetVersionsResponse]("POST", basePath + s"/dataset-version/findDatasetVersions", __query, body, ModeldbFindDatasetVersionsResponse.fromJson)
  }

  def findDatasetVersions(body: ModeldbFindDatasetVersions)(implicit ec: ExecutionContext): Try[ModeldbFindDatasetVersionsResponse] = Await.result(findDatasetVersionsAsync(body), Duration.Inf)

  def getAllDatasetVersionsByDatasetIdAsync(dataset_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse]] = {
    val __query = Map[String,String](
      "dataset_id" -> client.toQuery(dataset_id),
      "page_number" -> client.toQuery(page_number),
      "page_limit" -> client.toQuery(page_limit),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetAllDatasetVersionsByDatasetIdResponse]("GET", basePath + s"/dataset-version/getAllDatasetVersionsByDatasetId", __query, body, ModeldbGetAllDatasetVersionsByDatasetIdResponse.fromJson)
  }

  def getAllDatasetVersionsByDatasetId(dataset_id: String, page_number: BigInt, page_limit: BigInt, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetAllDatasetVersionsByDatasetIdResponse] = Await.result(getAllDatasetVersionsByDatasetIdAsync(dataset_id, page_number, page_limit, ascending, sort_key), Duration.Inf)

  def getDatasetVersionAttributesAsync(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Future[Try[ModeldbGetAttributesResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id),
      "attribute_keys" -> client.toQuery(attribute_keys),
      "get_all" -> client.toQuery(get_all)
    )
    val body: String = null
    return client.request[String, ModeldbGetAttributesResponse]("GET", basePath + s"/dataset-version/getDatasetVersionAttributes", __query, body, ModeldbGetAttributesResponse.fromJson)
  }

  def getDatasetVersionAttributes(id: String, attribute_keys: List[String], get_all: Boolean)(implicit ec: ExecutionContext): Try[ModeldbGetAttributesResponse] = Await.result(getDatasetVersionAttributesAsync(id, attribute_keys, get_all), Duration.Inf)

  def getDatasetVersionByIdAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetDatasetVersionByIdResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetDatasetVersionByIdResponse]("GET", basePath + s"/dataset-version/getDatasetVersionById", __query, body, ModeldbGetDatasetVersionByIdResponse.fromJson)
  }

  def getDatasetVersionById(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetDatasetVersionByIdResponse] = Await.result(getDatasetVersionByIdAsync(id), Duration.Inf)

  def getDatasetVersionTagsAsync(id: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetTagsResponse]] = {
    val __query = Map[String,String](
      "id" -> client.toQuery(id)
    )
    val body: String = null
    return client.request[String, ModeldbGetTagsResponse]("GET", basePath + s"/dataset-version/getDatasetVersionTags", __query, body, ModeldbGetTagsResponse.fromJson)
  }

  def getDatasetVersionTags(id: String)(implicit ec: ExecutionContext): Try[ModeldbGetTagsResponse] = Await.result(getDatasetVersionTagsAsync(id), Duration.Inf)

  def getLatestDatasetVersionByDatasetIdAsync(dataset_id: String, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Future[Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse]] = {
    val __query = Map[String,String](
      "dataset_id" -> client.toQuery(dataset_id),
      "ascending" -> client.toQuery(ascending),
      "sort_key" -> client.toQuery(sort_key)
    )
    val body: String = null
    return client.request[String, ModeldbGetLatestDatasetVersionByDatasetIdResponse]("GET", basePath + s"/dataset-version/getLatestDatasetVersionByDatasetId", __query, body, ModeldbGetLatestDatasetVersionByDatasetIdResponse.fromJson)
  }

  def getLatestDatasetVersionByDatasetId(dataset_id: String, ascending: Boolean, sort_key: String)(implicit ec: ExecutionContext): Try[ModeldbGetLatestDatasetVersionByDatasetIdResponse] = Await.result(getLatestDatasetVersionByDatasetIdAsync(dataset_id, ascending, sort_key), Duration.Inf)

  def setDatasetVersionVisibilityAsync(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Future[Try[ModeldbSetDatasetVersionVisibiltyResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbSetDatasetVersionVisibilty, ModeldbSetDatasetVersionVisibiltyResponse]("POST", basePath + s"/dataset-version/setDatasetVersionVisibility", __query, body, ModeldbSetDatasetVersionVisibiltyResponse.fromJson)
  }

  def setDatasetVersionVisibility(body: ModeldbSetDatasetVersionVisibilty)(implicit ec: ExecutionContext): Try[ModeldbSetDatasetVersionVisibiltyResponse] = Await.result(setDatasetVersionVisibilityAsync(body), Duration.Inf)

  def updateDatasetVersionAttributesAsync(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionAttributesResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionAttributes, ModeldbUpdateDatasetVersionAttributesResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionAttributes", __query, body, ModeldbUpdateDatasetVersionAttributesResponse.fromJson)
  }

  def updateDatasetVersionAttributes(body: ModeldbUpdateDatasetVersionAttributes)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionAttributesResponse] = Await.result(updateDatasetVersionAttributesAsync(body), Duration.Inf)

  def updateDatasetVersionDescriptionAsync(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Future[Try[ModeldbUpdateDatasetVersionDescriptionResponse]] = {
    val __query = Map[String,String](
    )
    if (body == null) throw new Exception("Missing required parameter \"body\"")
    return client.request[ModeldbUpdateDatasetVersionDescription, ModeldbUpdateDatasetVersionDescriptionResponse]("POST", basePath + s"/dataset-version/updateDatasetVersionDescription", __query, body, ModeldbUpdateDatasetVersionDescriptionResponse.fromJson)
  }

  def updateDatasetVersionDescription(body: ModeldbUpdateDatasetVersionDescription)(implicit ec: ExecutionContext): Try[ModeldbUpdateDatasetVersionDescriptionResponse] = Await.result(updateDatasetVersionDescriptionAsync(body), Duration.Inf)

}
