// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningFolderElement (
  element_name: Option[String] = None,
  created_by_commit: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFolderElement.toJson(this)
}

object VersioningFolderElement {
  def toJson(obj: VersioningFolderElement): JObject = {
    new JObject(
      List[Option[JField]](
        obj.element_name.map(x => JField("element_name", JString(x))),
        obj.created_by_commit.map(x => JField("created_by_commit", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFolderElement =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFolderElement(
          // TODO: handle required
          element_name = fieldsMap.get("element_name").map(JsonConverter.fromJsonString),
          created_by_commit = fieldsMap.get("created_by_commit").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
