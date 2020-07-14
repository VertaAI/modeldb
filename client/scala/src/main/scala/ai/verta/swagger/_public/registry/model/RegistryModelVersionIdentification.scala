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

case class RegistryModelVersionIdentification (
  model_id: Option[RegistryRegisteredModelIdentification] = None,
  model_version_id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryModelVersionIdentification.toJson(this)
}

object RegistryModelVersionIdentification {
  def toJson(obj: RegistryModelVersionIdentification): JObject = {
    new JObject(
      List[Option[JField]](
        obj.model_id.map(x => JField("model_id", ((x: RegistryRegisteredModelIdentification) => RegistryRegisteredModelIdentification.toJson(x))(x))),
        obj.model_version_id.map(x => JField("model_version_id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryModelVersionIdentification =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryModelVersionIdentification(
          // TODO: handle required
          model_id = fieldsMap.get("model_id").map(RegistryRegisteredModelIdentification.fromJson),
          model_version_id = fieldsMap.get("model_version_id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
