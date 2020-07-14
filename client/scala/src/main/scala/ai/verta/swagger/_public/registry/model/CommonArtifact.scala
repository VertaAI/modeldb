// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.registry.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.registry.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.registry.model.OperatorEnumOperator._
import ai.verta.swagger._public.registry.model.ProtobufNullValue._
import ai.verta.swagger._public.registry.model.TernaryEnumTernary._
import ai.verta.swagger._public.registry.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.registry.model.VisibilityEnumVisibility._
import ai.verta.swagger.client.objects._

case class CommonArtifact (
  artifact_type: Option[ArtifactTypeEnumArtifactType] = None,
  filename_extension: Option[String] = None,
  key: Option[String] = None,
  linked_artifact_id: Option[String] = None,
  path: Option[String] = None,
  path_only: Option[Boolean] = None
) extends BaseSwagger {
  def toJson(): JValue = CommonArtifact.toJson(this)
}

object CommonArtifact {
  def toJson(obj: CommonArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact_type.map(x => JField("artifact_type", ((x: ArtifactTypeEnumArtifactType) => ArtifactTypeEnumArtifactType.toJson(x))(x))),
        obj.filename_extension.map(x => JField("filename_extension", JString(x))),
        obj.key.map(x => JField("key", JString(x))),
        obj.linked_artifact_id.map(x => JField("linked_artifact_id", JString(x))),
        obj.path.map(x => JField("path", JString(x))),
        obj.path_only.map(x => JField("path_only", JBool(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): CommonArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        CommonArtifact(
          // TODO: handle required
          artifact_type = fieldsMap.get("artifact_type").map(ArtifactTypeEnumArtifactType.fromJson),
          filename_extension = fieldsMap.get("filename_extension").map(JsonConverter.fromJsonString),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          linked_artifact_id = fieldsMap.get("linked_artifact_id").map(JsonConverter.fromJsonString),
          path = fieldsMap.get("path").map(JsonConverter.fromJsonString),
          path_only = fieldsMap.get("path_only").map(JsonConverter.fromJsonBoolean)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
