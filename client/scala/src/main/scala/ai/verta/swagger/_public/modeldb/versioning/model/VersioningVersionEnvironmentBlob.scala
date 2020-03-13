// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningVersionEnvironmentBlob (
  major: Option[BigInt] = None,
  minor: Option[BigInt] = None,
  patch: Option[BigInt] = None,
  suffix: Option[String] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningVersionEnvironmentBlob.toJson(this)
}

object VersioningVersionEnvironmentBlob {
  def toJson(obj: VersioningVersionEnvironmentBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.major.map(x => JField("major", JInt(x))),
        obj.minor.map(x => JField("minor", JInt(x))),
        obj.patch.map(x => JField("patch", JInt(x))),
        obj.suffix.map(x => JField("suffix", JString(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningVersionEnvironmentBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningVersionEnvironmentBlob(
          // TODO: handle required
          major = fieldsMap.get("major").map(JsonConverter.fromJsonInteger),
          minor = fieldsMap.get("minor").map(JsonConverter.fromJsonInteger),
          patch = fieldsMap.get("patch").map(JsonConverter.fromJsonInteger),
          suffix = fieldsMap.get("suffix").map(JsonConverter.fromJsonString)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
