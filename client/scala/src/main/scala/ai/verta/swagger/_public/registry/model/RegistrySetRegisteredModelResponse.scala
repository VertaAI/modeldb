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

case class RegistrySetRegisteredModelResponse (
  registered_model: Option[RegistryRegisteredModel] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistrySetRegisteredModelResponse.toJson(this)
}

object RegistrySetRegisteredModelResponse {
  def toJson(obj: RegistrySetRegisteredModelResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.registered_model.map(x => JField("registered_model", ((x: RegistryRegisteredModel) => RegistryRegisteredModel.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistrySetRegisteredModelResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistrySetRegisteredModelResponse(
          // TODO: handle required
          registered_model = fieldsMap.get("registered_model").map(RegistryRegisteredModel.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
