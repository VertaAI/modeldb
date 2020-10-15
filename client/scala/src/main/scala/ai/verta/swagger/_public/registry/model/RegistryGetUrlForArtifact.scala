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

case class RegistryGetUrlForArtifact (
  artifact_type: Option[ArtifactTypeEnumArtifactType] = None,
  key: Option[String] = None,
  method: Option[String] = None,
  model_version_id: Option[BigInt] = None,
  part_number: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryGetUrlForArtifact.toJson(this)
}

object RegistryGetUrlForArtifact {
  def toJson(obj: RegistryGetUrlForArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.artifact_type.map(x => JField("artifact_type", ((x: ArtifactTypeEnumArtifactType) => ArtifactTypeEnumArtifactType.toJson(x))(x))),
        obj.key.map(x => JField("key", JString(x))),
        obj.method.map(x => JField("method", JString(x))),
        obj.model_version_id.map(x => JField("model_version_id", JInt(x))),
        obj.part_number.map(x => JField("part_number", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryGetUrlForArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryGetUrlForArtifact(
          // TODO: handle required
          artifact_type = fieldsMap.get("artifact_type").map(ArtifactTypeEnumArtifactType.fromJson),
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          method = fieldsMap.get("method").map(JsonConverter.fromJsonString),
          model_version_id = fieldsMap.get("model_version_id").map(JsonConverter.fromJsonInteger),
          part_number = fieldsMap.get("part_number").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
