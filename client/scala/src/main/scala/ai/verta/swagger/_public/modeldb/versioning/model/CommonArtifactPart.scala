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

case class CommonArtifactPart (
  etag: Option[String] = None,
  part_number: Option[BigInt] = None
) extends BaseSwagger {
  def toJson(): JValue = CommonArtifactPart.toJson(this)
}

object CommonArtifactPart {
  def toJson(obj: CommonArtifactPart): JObject = {
    new JObject(
      List[Option[JField]](
        obj.etag.map(x => JField("etag", JString(x))),
        obj.part_number.map(x => JField("part_number", JInt(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): CommonArtifactPart =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        CommonArtifactPart(
          // TODO: handle required
          etag = fieldsMap.get("etag").map(JsonConverter.fromJsonString),
          part_number = fieldsMap.get("part_number").map(JsonConverter.fromJsonInteger)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
