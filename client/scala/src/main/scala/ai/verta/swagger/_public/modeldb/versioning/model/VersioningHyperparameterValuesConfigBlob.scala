// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.OperatorEnumOperator._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger._public.modeldb.versioning.model.RepositoryVisibilityEnumRepositoryVisibility._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.VersioningBlobType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterValuesConfigBlob (
  float_value: Option[Double] = None,
  int_value: Option[BigInt] = None,
  string_value: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterValuesConfigBlob.toJson(this)
}

object VersioningHyperparameterValuesConfigBlob {
  def toJson(obj: VersioningHyperparameterValuesConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.float_value.map(x => JField("float_value", JDouble(x))),
        obj.int_value.map(x => JField("int_value", JInt(x))),
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
          float_value = fieldsMap.get("float_value").map(JsonConverter.fromJsonDouble),
          int_value = fieldsMap.get("int_value").map(JsonConverter.fromJsonInteger),
          string_value = fieldsMap.get("string_value").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
