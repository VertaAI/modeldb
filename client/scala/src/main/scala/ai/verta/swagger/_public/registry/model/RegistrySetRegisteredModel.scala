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

case class RegistrySetRegisteredModel (
  id: Option[RegistryRegisteredModelIdentification] = None,
  registered_model: Option[RegistryRegisteredModel] = None,
  update_mask: Option[ProtobufFieldMask] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistrySetRegisteredModel.toJson(this)
}

object RegistrySetRegisteredModel {
  def toJson(obj: RegistrySetRegisteredModel): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", ((x: RegistryRegisteredModelIdentification) => RegistryRegisteredModelIdentification.toJson(x))(x))),
        obj.registered_model.map(x => JField("registered_model", ((x: RegistryRegisteredModel) => RegistryRegisteredModel.toJson(x))(x))),
        obj.update_mask.map(x => JField("update_mask", ((x: ProtobufFieldMask) => ProtobufFieldMask.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistrySetRegisteredModel =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistrySetRegisteredModel(
          // TODO: handle required
          id = fieldsMap.get("id").map(RegistryRegisteredModelIdentification.fromJson),
          registered_model = fieldsMap.get("registered_model").map(RegistryRegisteredModel.fromJson),
          update_mask = fieldsMap.get("update_mask").map(ProtobufFieldMask.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
