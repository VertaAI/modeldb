// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.ArtifactTypeEnumArtifactType._
import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.TernaryEnumTernary._
import ai.verta.swagger._public.modeldb.versioning.model.ValueTypeEnumValueType._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger._public.modeldb.versioning.model.ProtobufNullValue._
import ai.verta.swagger.client.objects._

case class VersioningFolderElement (
  created_by_commit: Option[String] = None,
  element_name: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFolderElement.toJson(this)
}

object VersioningFolderElement {
  def toJson(obj: VersioningFolderElement): JObject = {
    new JObject(
      List[Option[JField]](
        obj.created_by_commit.map(x => JField("created_by_commit", JString(x))),
        obj.element_name.map(x => JField("element_name", JString(x)))
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
          created_by_commit = fieldsMap.get("created_by_commit").map(JsonConverter.fromJsonString),
          element_name = fieldsMap.get("element_name").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
