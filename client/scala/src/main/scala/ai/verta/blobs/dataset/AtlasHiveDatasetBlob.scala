package ai.verta.blobs.dataset

import net.liftweb.json._
import ai.verta.swagger.client.objects._
import ai.verta.swagger.client.HttpClient

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.Duration

/** Represents a dataset from a query to an Atlas Hive table.
 */
class AtlasHiveDatasetBlob(
  private val atlasQuery: String,
  private val atlasSourceURI: String,
  override val numRecords: Option[BigInt] = None,
  override val executionTimestamp: Option[BigInt] = None
) extends QueryDatasetBlob {
  // cannot make this a case class due to constructor conflict.

  override val query = Some(atlasQuery)
  override val dataSourceURI = Some(atlasSourceURI)
}

object AtlasHiveDatasetBlob {
  def apply(
    guid: String,
    atlasURL: String = sys.env.get("ATLAS_URL").getOrElse(""),
    atlasUserName: String = sys.env.get("ATLAS_USERNAME").getOrElse(""),
    atlasPassword: String = sys.env.get("ATLAS_PASSWORD").getOrElse(""),
    atlasEntityEndpoint: String = "/api/atlas/v2/entity/bulk"
  )(implicit ec: ExecutionContext): Try[AtlasHiveDatasetBlob] = {
    val atlasSourceURI = f"${atlasURL}/index.html#!/detailPage/${guid}"
    val httpClient = new HttpClient(atlasURL, Map())
    Await.result(
      httpClient.request(
        "GET",
        atlasEntityEndpoint,
        Map(
          "guid" -> List(guid)
        ),
        null,
        jsonVal => fromJson(jsonVal, atlasSourceURI), // parser
        Some((atlasUserName, atlasPassword)) // authentication
      ),
      Duration.Inf
    ).flatten // Try[Try[AtlasHiveDatasetBlob]] to Try[AtlasHiveDatasetBlob]
  }

  private def fromJson(value: JValue, atlasSourceURI: String): Try[AtlasHiveDatasetBlob] =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap

        for (
          entityMap <- extractEntity(fieldsMap);
          attributesMap <- getSubMap(entityMap, "attributes");
          relationshipAttributesMap <- getSubMap(entityMap, "relationshipAttributes");
          dbRelationshipAttributesMap <- getSubMap(relationshipAttributesMap, "db");
          parametersMap <- getSubMap(attributesMap, "parameters")
        ) yield {
          val tableName: String = attributesMap.get("name").map(JsonConverter.fromJsonString).get
          val databaseName: String = dbRelationshipAttributesMap.get("displayText").map(JsonConverter.fromJsonString).get
          val atlasQuery = f"select * from ${databaseName}.${tableName}"

          val numRecords: BigInt = parametersMap.get("numRows").map(JsonConverter.fromJsonInteger).get

          // this is based on the Python client, but is it correct?
          val executionTimestamp = System.currentTimeMillis()

          new AtlasHiveDatasetBlob(atlasQuery, atlasSourceURI, Some(numRecords), Some(executionTimestamp))
        }
      }
      case _ => Failure(throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}"))
    }

  // get the first entity.
  // equivalent to table_obj["entities"][0]
  private def extractEntity(fieldsMap: Map[String, JValue]): Try[Map[String, JValue]] = {
    fieldsMap.get("entities") match {
      case Some(JArray(elements)) => {
        val head = elements.head

        head match {
          case JObject(entityFields) => Success(entityFields.map(f => (f.name, f.value)).toMap)
          case _ => Failure(new IllegalArgumentException(s"unknown type ${head.getClass.toString}"))
        }
      }
      case Some(other) => Failure(new IllegalArgumentException(s"unknown type ${other.getClass.toString}"))
      case None => Failure(new IllegalArgumentException("\"entities\" field not found."))
    }
  }

  // extract a certain field from a json map, which in turn is another map
  private def getSubMap(map: Map[String, JValue], key: String): Try[Map[String, JValue]] =
    map.get(key) match {
      case Some(JObject(fields)) => Success(fields.map(f => (f.name, f.value)).toMap)
      case Some(other) => Failure(new IllegalArgumentException(s"unknown type ${other.getClass.toString}"))
      case None => Failure(new IllegalArgumentException(f"key ${key} is not in map"))
    }
}
