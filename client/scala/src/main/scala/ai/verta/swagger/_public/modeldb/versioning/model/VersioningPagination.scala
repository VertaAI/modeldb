// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningPagination (
  page_number: Option[BigInt] = None,
  page_limit: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningPagination.toJson(this)
}

object VersioningPagination {
  def toJson(obj: VersioningPagination): JObject = {
    new JObject(
      List[Option[JField]](
        obj.page_number.map(x => JField("page_number", JInt(x))),
        obj.page_limit.map(x => JField("page_limit", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningPagination =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningPagination(
          // TODO: handle required
          page_number = fieldsMap.get("page_number").map(JsonConverter.fromJsonInteger),
          page_limit = fieldsMap.get("page_limit").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
