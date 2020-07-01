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

case class CommonKeyValue (
  key: Option[String] = None,
  value: Option[GenericObject] = None,
  value_type: Option[ValueTypeEnumValueType] = None
) extends BaseSwagger {
  def toJson(): JValue = CommonKeyValue.toJson(this)
}

object CommonKeyValue {
  def toJson(obj: CommonKeyValue): JObject = {
    new JObject(
      List[Option[JField]](
        obj.key.map(x => JField("key", JString(x))),
        obj.value.map(x => JField("value", ((x: GenericObject) => x.toJson())(x))),
        obj.value_type.map(x => JField("value_type", ((x: ValueTypeEnumValueType) => ValueTypeEnumValueType.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): CommonKeyValue =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        CommonKeyValue(
          // TODO: handle required
          key = fieldsMap.get("key").map(JsonConverter.fromJsonString),
          value = fieldsMap.get("value").map(GenericObject.fromJson),
          value_type = fieldsMap.get("value_type").map(ValueTypeEnumValueType.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
