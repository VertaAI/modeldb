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

case class RegistryFindRegisteredModelRequest (
  ascending: Option[Boolean] = None,
  pagination: Option[CommonPagination] = None,
  predicates: Option[List[CommonKeyValueQuery]] = None,
  sort_key: Option[String] = None,
  workspace_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryFindRegisteredModelRequest.toJson(this)
}

object RegistryFindRegisteredModelRequest {
  def toJson(obj: RegistryFindRegisteredModelRequest): JObject = {
    new JObject(
      List[Option[JField]](
        obj.ascending.map(x => JField("ascending", JBool(x))),
        obj.pagination.map(x => JField("pagination", ((x: CommonPagination) => CommonPagination.toJson(x))(x))),
        obj.predicates.map(x => JField("predicates", ((x: List[CommonKeyValueQuery]) => JArray(x.map(((x: CommonKeyValueQuery) => CommonKeyValueQuery.toJson(x)))))(x))),
        obj.sort_key.map(x => JField("sort_key", JString(x))),
        obj.workspace_name.map(x => JField("workspace_name", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryFindRegisteredModelRequest =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryFindRegisteredModelRequest(
          // TODO: handle required
          ascending = fieldsMap.get("ascending").map(JsonConverter.fromJsonBoolean),
          pagination = fieldsMap.get("pagination").map(CommonPagination.fromJson),
          predicates = fieldsMap.get("predicates").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValueQuery.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          sort_key = fieldsMap.get("sort_key").map(JsonConverter.fromJsonString),
          workspace_name = fieldsMap.get("workspace_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
