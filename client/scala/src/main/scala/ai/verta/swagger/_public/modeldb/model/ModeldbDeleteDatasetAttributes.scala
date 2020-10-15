// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.model.DatasetTypeEnumDatasetType._
import ai.verta.swagger._public.modeldb.model.DatasetVisibilityEnumDatasetVisibility._
import ai.verta.swagger._public.modeldb.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class ModeldbDeleteDatasetAttributes (
  attribute_keys: Option[List[String]] = None,
  delete_all: Option[Boolean] = None,
  id: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = ModeldbDeleteDatasetAttributes.toJson(this)
}

object ModeldbDeleteDatasetAttributes {
  def toJson(obj: ModeldbDeleteDatasetAttributes): JObject = {
    new JObject(
      List[Option[JField]](
        obj.attribute_keys.map(x => JField("attribute_keys", ((x: List[String]) => JArray(x.map(JString)))(x))),
        obj.delete_all.map(x => JField("delete_all", JBool(x))),
        obj.id.map(x => JField("id", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): ModeldbDeleteDatasetAttributes =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        ModeldbDeleteDatasetAttributes(
          // TODO: handle required
          attribute_keys = fieldsMap.get("attribute_keys").map((x: JValue) => x match {case JArray(elements) => elements.map(JsonConverter.fromJsonString); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          delete_all = fieldsMap.get("delete_all").map(JsonConverter.fromJsonBoolean),
          id = fieldsMap.get("id").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
