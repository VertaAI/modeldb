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

case class VersioningFolder (
  blobs: Option[List[VersioningFolderElement]] = None,
  sub_folders: Option[List[VersioningFolderElement]] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningFolder.toJson(this)
}

object VersioningFolder {
  def toJson(obj: VersioningFolder): JObject = {
    new JObject(
      List[Option[JField]](
        obj.blobs.map(x => JField("blobs", ((x: List[VersioningFolderElement]) => JArray(x.map(((x: VersioningFolderElement) => VersioningFolderElement.toJson(x)))))(x))),
        obj.sub_folders.map(x => JField("sub_folders", ((x: List[VersioningFolderElement]) => JArray(x.map(((x: VersioningFolderElement) => VersioningFolderElement.toJson(x)))))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningFolder =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningFolder(
          // TODO: handle required
          blobs = fieldsMap.get("blobs").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningFolderElement.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")}),
          sub_folders = fieldsMap.get("sub_folders").map((x: JValue) => x match {case JArray(elements) => elements.map(VersioningFolderElement.fromJson); case _ => throw new IllegalArgumentException(s"unknown type ${x.getClass.toString}")})
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
