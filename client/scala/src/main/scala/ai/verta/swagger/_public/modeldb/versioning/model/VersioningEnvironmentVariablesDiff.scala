// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningEnvironmentVariablesDiff (
  status: Option[DiffStatusEnumDiffStatus] = None,
  name: Option[String] = None,
  value_a: Option[String] = None,
  value_b: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningEnvironmentVariablesDiff.toJson(this)
}

object VersioningEnvironmentVariablesDiff {
  def toJson(obj: VersioningEnvironmentVariablesDiff): JObject = {
    new JObject(
      List[Option[JField]](
        obj.status.map(x => JField("status", ((x: DiffStatusEnumDiffStatus) => DiffStatusEnumDiffStatus.toJson(x))(x))),
        obj.name.map(x => JField("name", JString(x))),
        obj.value_a.map(x => JField("value_a", JString(x))),
        obj.value_b.map(x => JField("value_b", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningEnvironmentVariablesDiff =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningEnvironmentVariablesDiff(
          // TODO: handle required
          status = fieldsMap.get("status").map(DiffStatusEnumDiffStatus.fromJson),
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          value_a = fieldsMap.get("value_a").map(JsonConverter.fromJsonString),
          value_b = fieldsMap.get("value_b").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
