package ai.verta.blobs.dataset

import net.liftweb.json.{JValue, JObject}
import ai.verta.swagger.client.objects._
import ai.verta.swagger.client.HttpClient

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.Duration

class AtlasHiveDatasetBlob(
  private val atlasQuery: String,
  private val atlasSourceURI: String,
  override val numRecords: Option[BigInt] = None,
  override val executionTimestamp: Option[BigInt] = None
) extends QueryDatasetBlob {
  // cannot make this a case class due to constructor conflict.
  override val dataSourceURI = Some(atlasSourceURI)
}

object AtlasHiveDatasetBlob {
  def apply(
    guid: String,
    atlasURL: String,
    atlasUserName: String,
    atlasPassword: String,
    atlasEntityEndpoint: String = "/api/atlas/v2/entity/bulk"
  )(implicit ec: ExecutionContext): Try[AtlasHiveDatasetBlob] = {
    val atlasSourceURI = f"${atlasURL}/index.html#!/detailPage/${guid}"
    val httpClient = new HttpClient(atlasURL, Map())
    Await.result(
      httpClient.request(
        "GET",
        atlasEntityEndpoint,
        Map(
          "guid" -> guid
        ),
        null,
        jsonVal => fromJson(jsonVal, atlasSourceURI), // parser
        Some((atlasUserName, atlasPassword)) // authentication
      ),
      Duration.Inf
    )
  }

  private def fromJson(value: JValue, atlasSourceURI: String): AtlasHiveDatasetBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap

        val attributesMap = fieldsMap.get("attributes") match {
          case Some(JObject(attributes)) => attributes.map(f => (f.name, f.value)).toMap
        }

        val relationshipAttributesMap = fieldsMap.get("relationshipAttributes") match {
          case Some(JObject(relationshipAttributes)) => relationshipAttributes.map(f => (f.name, f.value)).toMap
        }
        val tableName: String = attributesMap.get("name").map(JsonConverter.fromJsonString).get

        val dbRelationshipAttributesMap = relationshipAttributesMap.get("db") match {
          case Some(JObject(db)) => db.map(f => (f.name, f.value)).toMap
        }
        val databaseName: String = dbRelationshipAttributesMap.get("displayText").map(JsonConverter.fromJsonString).get

        val atlasQuery = f"select * from ${databaseName}.${tableName}"

        val parametersMap = attributesMap.get("parameters") match {
          case Some(JObject(parameters)) => parameters.map(f => (f.name, f.value)).toMap
        }
        val numRecords: BigInt = parametersMap.get("numRows").map(JsonConverter.fromJsonInteger).get

        // this is based on the Python client, but is it correct?
        val executionTimestamp = System.currentTimeMillis()

        new AtlasHiveDatasetBlob(atlasQuery, atlasSourceURI, Some(numRecords), Some(executionTimestamp))
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
