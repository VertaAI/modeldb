// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.PathLocationTypeEnumPathLocationType._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger.client.objects._

case class ModeldbDeleteDatasetVersionTags (
  dataset_id: Option[String] = None,
  delete_all: Option[Boolean] = None,
  id: Option[String] = None,
  tags: Option[List[String]] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteDatasetVersionTags.toJson(this)
}

object ModeldbDeleteDatasetVersionTags {
  def toJson(obj: ModeldbDeleteDatasetVersionTags): JObject = {
    new JObject(
      List[Option[JField]](
        obj.dataset_id.map(x => JField("dataset_id", JString(x))),
        obj.delete_all.map(x => JField("delete_all", JBool(x))),
        obj.id.map(x => JField("id", JString(x))),
        obj.tags.map(x => JField("tags", ((x: List[String]) => JArray(x.map(JString)))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteDatasetVersionTags =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteDatasetVersionTags(
          // TODO: handle required
          dataset_id = fieldsMap.get("dataset_id").map(JsonConverter.fromJsonString),
          delete_all = fieldsMap.get("delete_all").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString),
          tags = fieldsMap.get("tags").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
