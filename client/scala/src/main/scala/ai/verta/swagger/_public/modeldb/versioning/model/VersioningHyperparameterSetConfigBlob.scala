// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.swagger._public.modeldb.versioning.model

import scala.util.Try

import net.liftweb.json._

import ai.verta.swagger._public.modeldb.versioning.model.DiffStatusEnumDiffStatus._
import ai.verta.swagger._public.modeldb.versioning.model.WorkspaceTypeEnumWorkspaceType._
import ai.verta.swagger.client.objects._

case class VersioningHyperparameterSetConfigBlob (
  name: Option[String] = None,
  continuous: Option[VersioningContinuousHyperparameterSetConfigBlob] = None,
  discrete: Option[VersioningDiscreteHyperparameterSetConfigBlob] = None
) extends BaseSwagger {
  def toJson(): JValue = VersioningHyperparameterSetConfigBlob.toJson(this)
}

object VersioningHyperparameterSetConfigBlob {
  def toJson(obj: VersioningHyperparameterSetConfigBlob): JObject = {
    new JObject(
      List[Option[JField]](
        obj.name.map(x => JField("name", JString(x))),
        obj.continuous.map(x => JField("continuous", ((x: VersioningContinuousHyperparameterSetConfigBlob) => VersioningContinuousHyperparameterSetConfigBlob.toJson(x))(x))),
        obj.discrete.map(x => JField("discrete", ((x: VersioningDiscreteHyperparameterSetConfigBlob) => VersioningDiscreteHyperparameterSetConfigBlob.toJson(x))(x)))
      ).flatMap(x => x match {
        case Some(y) => List(y)
        case None => Nil
      })
    )
  }

  def fromJson(value: JValue): VersioningHyperparameterSetConfigBlob =
    value match {
      case JObject(fields) => {
        val fieldsMap = fields.map(f => (f.name, f.value)).toMap
        VersioningHyperparameterSetConfigBlob(
          // TODO: handle required
          name = fieldsMap.get("name").map(JsonConverter.fromJsonString),
          continuous = fieldsMap.get("continuous").map(VersioningContinuousHyperparameterSetConfigBlob.fromJson),
          discrete = fieldsMap.get("discrete").map(VersioningDiscreteHyperparameterSetConfigBlob.fromJson)
        )
      }
      case _ => throw new IllegalArgumentException(s"unknown type ${value.getClass.toString}")
    }
}
