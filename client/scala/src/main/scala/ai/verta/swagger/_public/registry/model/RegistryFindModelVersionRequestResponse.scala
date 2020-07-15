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

case class RegistryFindModelVersionRequestResponse (
  model_versions: Option[List[RegistryModelVersion]] = None,
  total_records: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryFindModelVersionRequestResponse.toJson(this)
}

object RegistryFindModelVersionRequestResponse {
  def toJson(obj: RegistryFindModelVersionRequestResponse): JObject = {
    new JObject(
      List[Option[JField]](
        obj.model_versions.map(x => JField("model_versions", ((x: List[RegistryModelVersion]) => JArray(x.map(((x: RegistryModelVersion) => RegistryModelVersion.toJson(x)))))(x))),
        obj.total_records.map(x => JField("total_records", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryFindModelVersionRequestResponse =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryFindModelVersionRequestResponse(
          // TODO: handle required
          model_versions = fieldsMap.get("model_versions").map((x: JValue) => x match {case JArray(elements) => elements.map(RegistryModelVersion.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          total_records = fieldsMap.get("total_records").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
