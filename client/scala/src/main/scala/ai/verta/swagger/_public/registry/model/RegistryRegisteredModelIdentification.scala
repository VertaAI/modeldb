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

case class RegistryRegisteredModelIdentification (
  named_id: Option[RegistryRegisteredModelNamedIdentification] = None,
  registered_model_id: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryRegisteredModelIdentification.toJson(this)
}

object RegistryRegisteredModelIdentification {
  def toJson(obj: RegistryRegisteredModelIdentification): JObject = {
    new JObject(
      List[Option[JField]](
        obj.named_id.map(x => JField("named_id", ((x: RegistryRegisteredModelNamedIdentification) => RegistryRegisteredModelNamedIdentification.toJson(x))(x))),
        obj.registered_model_id.map(x => JField("registered_model_id", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryRegisteredModelIdentification =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryRegisteredModelIdentification(
          // TODO: handle required
          named_id = fieldsMap.get("named_id").map(RegistryRegisteredModelNamedIdentification.fromJson),
          registered_model_id = fieldsMap.get("registered_model_id").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
