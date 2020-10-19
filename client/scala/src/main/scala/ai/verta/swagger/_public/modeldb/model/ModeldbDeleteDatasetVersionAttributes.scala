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

case class ModeldbDeleteDatasetVersionAttributes (
  attribute_keys: Option[List[String]] = None,
  dataset_id: Option[String] = None,
  delete_all: Option[Boolean] = None,
  id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteDatasetVersionAttributes.toJson(this)
}

object ModeldbDeleteDatasetVersionAttributes {
  def toJson(obj: ModeldbDeleteDatasetVersionAttributes): JObject = {
    new JObject(
      List[Option[JField]](
        obj.attribute_keys.map(x => JField("attribute_keys", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.dataset_id.map(x => JField("dataset_id", JString(x))),
        obj.delete_all.map(x => JField("delete_all", JBool(x))),
        obj.id.map(x => JField("id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteDatasetVersionAttributes =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteDatasetVersionAttributes(
          // TODO: handle required
          attribute_keys = fieldsMap.get("attribute_keys").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          dataset_id = fieldsMap.get("dataset_id").map(JsonConverter.fromJsonString),
          delete_all = fieldsMap.get("delete_all").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
