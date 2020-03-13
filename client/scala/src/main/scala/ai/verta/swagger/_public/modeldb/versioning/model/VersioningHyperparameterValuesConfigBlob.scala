// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterValuesConfigBlob (
  int_value: Option[String] = None,
  float_value: Option[Double] = None,
  string_value: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterValuesConfigBlob.toJson(this)
}

object VersioningHyperparameterValuesConfigBlob {
  def toJson(obj: VersioningHyperparameterValuesConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.int_value.map(x => JField("int_value", JString(x))),
        obj.float_value.map(x => JField("float_value", JDouble(x))),
        obj.string_value.map(x => JField("string_value", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterValuesConfigBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterValuesConfigBlob(
          // TODO: handle required
          int_value = fieldsMap.get("int_value").map(JsonConverter.fromJsonString),
          float_value = fieldsMap.get("float_value").map(JsonConverter.fromJsonDouble),
          string_value = fieldsMap.get("string_value").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
