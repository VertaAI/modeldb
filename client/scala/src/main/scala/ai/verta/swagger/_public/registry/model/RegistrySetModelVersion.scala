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

case class RegistrySetModelVersion (
  id: Option[RegistryModelVersionIdentification] = None,
  model_version: Option[RegistryModelVersion] = None,
  update_mask: Option[ProtobufFieldMask] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistrySetModelVersion.toJson(this)
}

object RegistrySetModelVersion {
  def toJson(obj: RegistrySetModelVersion): JObject = {
    new JObject(
      List[Option[JField]](
        obj.id.map(x => JField("id", ((x: RegistryModelVersionIdentification) => RegistryModelVersionIdentification.toJson(x))(x))),
        obj.model_version.map(x => JField("model_version", ((x: RegistryModelVersion) => RegistryModelVersion.toJson(x))(x))),
        obj.update_mask.map(x => JField("update_mask", ((x: ProtobufFieldMask) => ProtobufFieldMask.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistrySetModelVersion =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistrySetModelVersion(
          // TODO: handle required
          id = fieldsMap.get("id").map(RegistryModelVersionIdentification.fromJson),
          model_version = fieldsMap.get("model_version").map(RegistryModelVersion.fromJson),
          update_mask = fieldsMap.get("update_mask").map(ProtobufFieldMask.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
