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

case class RegistryFindRegisteredModelRequestResponse (
  registered_models: Option[List[RegistryRegisteredModel]] = None,
  total_records: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryFindRegisteredModelRequestResponse.toJson(this)
}

object RegistryFindRegisteredModelRequestResponse {
  def toJson(obj: RegistryFindRegisteredModelRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.registered_models.map(x => JField("registered_models", ((x: List[RegistryRegisteredModel]) => JArray(x.map(((x: RegistryRegisteredModel) => RegistryRegisteredModel.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryFindRegisteredModelRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryFindRegisteredModelRequestResponse(
          // TODO: handle required
          registered_models = fieldsMap.get("registered_models").map((x: JValue) => x match {case JArray(elements) => elements.map(RegistryRegisteredModel.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
