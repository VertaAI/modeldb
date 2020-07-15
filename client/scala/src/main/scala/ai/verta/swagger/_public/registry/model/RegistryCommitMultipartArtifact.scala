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

case class RegistryCommitMultipartArtifact (
  key: Option[String] = None,
  model_version_id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryCommitMultipartArtifact.toJson(this)
}

object RegistryCommitMultipartArtifact {
  def toJson(obj: RegistryCommitMultipartArtifact): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.model_version_id.map(x => JField("model_version_id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryCommitMultipartArtifact =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryCommitMultipartArtifact(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          model_version_id = fieldsMap.get("model_version_id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
