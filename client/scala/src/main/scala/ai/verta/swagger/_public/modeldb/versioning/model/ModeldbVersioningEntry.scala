// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class ModeldbVersioningEntry (
  commit: Option[String] = None,
  key_location_map: Option[Map[String,VertamodeldbLocation]] = None,
  repository_id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbVersioningEntry.toJson(this)
}

object ModeldbVersioningEntry {
  def toJson(obj: ModeldbVersioningEntry): JObject = {
    new JObject(
      List[Option[JField]](
        obj.commit.map(x => JField("commit", JString(x))),
        obj.key_location_map.map(x => JField("key_location_map", ((x: Map[String,VertamodeldbLocation]) => JObject(x.toList.map(kv => JField(kv._1,((x: VertamodeldbLocation) => VertamodeldbLocation.toJson(x))(kv._2)))))(x))),
        obj.repository_id.map(x => JField("repository_id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbVersioningEntry =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbVersioningEntry(
          // TODO: handle required
          commit = fieldsMap.get("commit").map(JsonConverter.fromJsonString),
          key_location_map = fieldsMap.get("key_location_map").map((x: JValue) => x match {case JObject(fields) => fields.map(kv => (kv.name, VertamodeldbLocation.fromJson(kv.value))).toMap; case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          repository_id = fieldsMap.get("repository_id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
