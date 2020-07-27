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

case class RegistryRegisteredModel (
  attributes: Option[List[CommonKeyValue]] = None,
  description: Option[String] = None,
  id: Option[BigInt] = None,
  labels: Option[List[String]] = None,
  name: Option[String] = None,
  owner: Option[String] = None,
  readme_text: Option[String] = None,
  time_created: Option[BigInt] = None,
  time_updated: Option[BigInt] = None,
  visibility: Option[VisibilityEnumVisibility] = None,
  workspace_id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = RegistryRegisteredModel.toJson(this)
}

object RegistryRegisteredModel {
  def toJson(obj: RegistryRegisteredModel): JObject = {
    new JObject(
      List[Option[JField]](
        obj.attributes.map(x => JField("attributes", ((x: List[CommonKeyValue]) => JArray(x.map(((x: CommonKeyValue) => CommonKeyValue.toJson(x)))))(x))),
        obj.description.map(x => JField("description", JString(x))),
        obj.id.map(x => JField("id", JInt(x))),
        obj.labels.map(x => JField("labels", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.owner.map(x => JField("owner", JString(x))),
        obj.readme_text.map(x => JField("readme_text", JString(x))),
        obj.time_created.map(x => JField("time_created", JInt(x))),
        obj.time_updated.map(x => JField("time_updated", JInt(x))),
        obj.visibility.map(x => JField("visibility", ((x: VisibilityEnumVisibility) => VisibilityEnumVisibility.toJson(x))(x))),
        obj.workspace_id.map(x => JField("workspace_id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): RegistryRegisteredModel =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        RegistryRegisteredModel(
          // TODO: handle required
          attributes = fieldsMap.get("attributes").map((x: JValue) => x match {case JArray(elements) => elements.map(CommonKeyValue.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          description = fieldsMap.get("description").map(JsonConverter.fromJsonString),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonInteger),
          labels = fieldsMap.get("labels").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          owner = fieldsMap.get("owner").map(JsonConverter.fromJsonString),
          readme_text = fieldsMap.get("readme_text").map(JsonConverter.fromJsonString),
          time_created = fieldsMap.get("time_created").map(JsonConverter.fromJsonInteger),
          time_updated = fieldsMap.get("time_updated").map(JsonConverter.fromJsonInteger),
          visibility = fieldsMap.get("visibility").map(VisibilityEnumVisibility.fromJson),
          workspace_id = fieldsMap.get("workspace_id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
