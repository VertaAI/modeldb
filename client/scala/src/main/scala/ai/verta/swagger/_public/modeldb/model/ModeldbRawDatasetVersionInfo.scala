// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.CollaboratorTypeEnumCollaboratorType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.EntitiesEnumEntitiesTypes._
import ai.verta.swagger._public.modeldb.model.IdServiceProviderEnumIdServiceProvider._
import ai.verta.swagger._public.modeldb.model.ModelDBActionEnumModelDBServiceActions._
import ai.verta.swagger._public.modeldb.model.ModeldbProjectVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ServiceEnumService._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.UacFlagEnum._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class ModeldbRawDatasetVersionInfo (
  checksum: Option[String] = None,
  features: Option[List[String]] = None,
  num_records: Option[BigInt] = None,
  object_path: Option[String] = None,
  size: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbRawDatasetVersionInfo.toJson(this)
}

object ModeldbRawDatasetVersionInfo {
  def toJson(obj: ModeldbRawDatasetVersionInfo): JObject = {
    new JObject(
      List[Option[JField]](
        obj.checksum.map(x => JField("checksum", JString(x))),
        obj.features.map(x => JField("features", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.num_records.map(x => JField("num_records", JInt(x))),
        obj.object_path.map(x => JField("object_path", JString(x))),
        obj.size.map(x => JField("size", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbRawDatasetVersionInfo =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbRawDatasetVersionInfo(
          // TODO: handle required
          checksum = fieldsMap.get("checksum").map(JsonConverter.fromJsonString),
          features = fieldsMap.get("features").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          num_records = fieldsMap.get("num_records").map(JsonConverter.fromJsonInteger),
          object_path = fieldsMap.get("object_path").map(JsonConverter.fromJsonString),
          size = fieldsMap.get("size").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
