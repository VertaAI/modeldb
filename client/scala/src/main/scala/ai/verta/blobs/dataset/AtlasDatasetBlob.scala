package ai.verta.blobs.dataset

import net.liftweb.json._
import ai.verta.swagger.client.objects._
import ai.verta.swagger.client.{HttpClient, BasicAuthentication}
import ai.verta.client.entities.utils._

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration.Duration

/** Represents a dataset from a query to Atlas. Currently only supports hive tables.
 */
class AtlasDatasetBlob(
  private val atlasQuery: String,
  private val atlasSourceURI: String,
  override val numRecords: Option[BigInt] = None,
  override val executionTimestamp: Option[BigInt] = None,
  val tags: List[String],
  val attributes: Map[String, ValueType]
) extends QueryDatasetBlob {
  // cannot make this a case class due to constructor conflict.

  override val query = Some(atlasQuery)
  override val dataSourceURI = Some(atlasSourceURI)
}

object AtlasDatasetBlob {
  def apply(
    guid: String,
    atlasURL: String = sys.env.get("ATLAS_URL").getOrElse(""),
    atlasUserName: String = sys.env.get("ATLAS_USERNAME").getOrElse(""),
    atlasPassword: String = sys.env.get("ATLAS_PASSWORD").getOrElse(""),
    atlasEntityEndpoint: String = "/api/atlas/v2/entity/bulk"
  )(implicit ec: ExecutionContext): Try[AtlasDatasetBlob] = {
    val atlasSourceURI = f"${atlasURL}/index.html#!/detailPage/${guid}"
    val httpClient = new HttpClient(atlasURL, Map())

    try {
      Await.result(
        httpClient.request(
          "GET",
          atlasEntityEndpoint,
          Map(
            "guid" -> List(guid)
          ),
          null,
          jsonVal => fromJson(jsonVal, atlasSourceURI), // parser
          Some(BasicAuthentication(atlasUserName, atlasPassword)) // authentication
        ),
        Duration.Inf
      ).flatten // Try[Try[AtlasHiveDatasetBlob]] to Try[AtlasHiveDatasetBlob]
    } finally {
      httpClient.close()
    }
  }

  private def fromJson(value: JValue, atlasSourceURI: String): Try[AtlasDatasetBlob] =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap

        for (
          entityMap <- extractEntity(fieldsMap);
          _ <- checkEntityType(entityMap); // return Failure right away if type is not hive table
          attributesMap <- getSubMap(entityMap, "attributes");
          relationshipAttributesMap <- getSubMap(entityMap, "relationshipAttributes");
          dbRelationshipAttributesMap <- getSubMap(relationshipAttributesMap, "db");
          parametersMap <- getSubMap(attributesMap, "parameters")
        ) yield {
          val tags = getTags(entityMap)
          val attributes = getAttributes(entityMap, attributesMap, relationshipAttributesMap, dbRelationshipAttributesMap)

          val tableName: String = attributes.get("table_name").get.asString.get
          val databaseName: String = attributes.get("database_name").get.asString.get
          val atlasQuery = f"select * from ${databaseName}.${tableName}"

          val numRecords: BigInt = parametersMap.get("numRows").map(JsonConverter.fromJsonInteger).get

          val executionTimestamp = System.currentTimeMillis()

          new AtlasDatasetBlob(atlasQuery, atlasSourceURI, Some(numRecords), Some(executionTimestamp), tags, attributes)
        }
      }
      case _ => Failure(throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}"))
    }

  // get the first entity.
  // equivalent to table_obj["entities"][0]
  private def extractEntity(fieldsMap: Map[String, JValue]): Try[Map[String, JValue]] = {
    fieldsMap.get("entities") match {
      case Some(JArray(elements)) => elements.headOption match {
        case Some(JObject(entityFields)) => Success(entityFields.map(f => (f.name, f.value)).toMap)
        case Some(other) => Failure(new IllegalArgumentException(s"unknown type ${other.getClass.toString}"))
        case None => Failure(new IllegalArgumentException(s"no entity found."))
      }
      case Some(other) => Failure(new IllegalArgumentException(s"unknown type ${other.getClass.toString}"))
      case None => Failure(new IllegalArgumentException("\"entities\" field not found."))
    }
  }

  private def checkEntityType(entityMap: Map[String, JValue]): Try[Unit] =
    if (getEntityType(entityMap) == "hive_table")
      Success(())
    else
      Failure(new IllegalArgumentException("Atlas dataset currently supported only for Hive tables."))

  private def getEntityType(entityMap: Map[String, JValue]) =
    entityMap.get("typeName").map(JsonConverter.fromJsonString).get

  // extract a certain field from a json map, which in turn is another map
  private def getSubMap(map: Map[String, JValue], key: String): Try[Map[String, JValue]] =
    map.get(key) match {
      case Some(JObject(fields)) => Success(fields.map(f => (f.name, f.value)).toMap)
      case Some(other) => Failure(new IllegalArgumentException(s"unknown type ${other.getClass.toString}"))
      case None => Failure(new IllegalArgumentException(f"key ${key} is not in map"))
    }

    private def getAttributes(
      entityMap: Map[String, JValue],
      attributesMap: Map[String, JValue],
      relationshipAttributesMap: Map[String, JValue],
      dbRelationshipAttributesMap: Map[String, JValue]
    ): Map[String, ValueType] = {
      val tableName: String = attributesMap.get("name").map(JsonConverter.fromJsonString).get
      val databaseName: String = dbRelationshipAttributesMap.get("displayText").map(JsonConverter.fromJsonString).get
      val createdTime: BigInt = entityMap.get("createTime").map(JsonConverter.fromJsonInteger).get
      val updatedTime: BigInt = entityMap.get("updateTime").map(JsonConverter.fromJsonInteger).get
      val columnNames: List[ValueType] = getListFieldDisplayText(relationshipAttributesMap, "columns")
      val loadQueries: List[ValueType] = getListFieldDisplayText(relationshipAttributesMap, "outputFromProcesses")

      Map[String, ValueType](
        "type" -> "hive_table", // only supports hive table.
        "table_name" -> tableName,
        "database_name" -> databaseName,
        "created_time" -> createdTime,
        "updated_time" -> updatedTime,
        "col_names" -> columnNames,
        "load_queries" -> loadQueries
      )
    }

    // get display test of each object in a list field
    private def getListFieldDisplayText(jsonMap: Map[String, JValue], fieldName: String): List[ValueType] =
      (jsonMap.get(fieldName) match {
        case Some(JArray(objs)) => objs
        case _ => List()
      })
        .flatMap(obj => obj match {
          case JObject(fields) =>
            fields
              .filter(field => field.name == "displayText")
              .map(_.value)
              .map(JsonConverter.fromJsonString)
          case _ => List()
        })
        .map(colName => ValueType.fromString(colName))

    private def getTags(entityMap: Map[String, JValue]): List[String] = {
      entityMap.get("classifications") match {
        case Some(JArray(classifications)) => classifications.flatMap(
          classification => classification match {
            case JObject(classificationFields) =>
              List(
                classificationFields.map(f => (f.name, f.value)).toMap.get("typeName").map(JsonConverter.fromJsonString).get
              )
            case _ => List()
          }
        )
        case _ => List()
      }
    }
}
