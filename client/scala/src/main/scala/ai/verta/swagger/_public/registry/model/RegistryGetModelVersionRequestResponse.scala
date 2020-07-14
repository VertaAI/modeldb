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

case class RegistryGetModelVersionRequestResponse (
  model_version: Option[RegistryModelVersion] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryGetModelVersionRequestResponse.toJson(this)
}

object RegistryGetModelVersionRequestResponse {
  def toJson(obj: RegistryGetModelVersionRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.model_version.map(x => JField("model_version", ((x: RegistryModelVersion) => RegistryModelVersion.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryGetModelVersionRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryGetModelVersionRequestResponse(
          // TODO: handle required
          model_version = fieldsMap.get("model_version").map(RegistryModelVersion.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
